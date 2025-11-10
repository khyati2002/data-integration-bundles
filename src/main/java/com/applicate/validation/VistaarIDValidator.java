package com.applicate.validation;

import com.salescode.dim.jooq.generated.tables.pojos.UserParent;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserParentService;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.etl.OperationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VistaarIDValidator extends AbstractValidationRule<User> {

	public static final String SUPPLIER = "supplier";
	public static final String DS_TYPE = "DSType";
	public static final String PSRCRMID = "PSRCRMID";
	private transient UserService userService;
	private transient UserParentService userParentService;
	String regex = "^[a-zA-Z]*$";
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public OperationResult.StepResult apply(User user) {
		initializeServices();

		List<String> errors = new ArrayList<>();

		if (isStockist(user)) {
			errors.addAll(validateStockist(user));
		}

		if (isPSR(user)) {
			errors.addAll(validatePSR(user));
		}

		if (isWD(user)) {
			errors.addAll(validateWD(user));
		}

		if (!errors.isEmpty()) {
			return new OperationResult.StepResult(OperationResult.Status.ERROR, String.join(",", errors));
		}

		return OperationResult.StepResult.OK;
	}

	private void initializeServices() {
		if (this.userService == null) {
			this.userService = (UserService) ServiceLocator.lookup(User.class);
		}
		if (this.userParentService == null) {
			this.userParentService = (UserParentService) ServiceLocator.lookup(UserParent.class);
		}
	}

	private List<String> validateStockist(User user) {
		List<String> errors = new ArrayList<>();

		if (StringUtils.isNullOrBlank(user.getLoginId())) {
			errors.add("UID is missing for stockist");
		}

		List<String> supplierList = extractSupplierList(user, errors);
		validateSupplierPresence(supplierList, errors);

		String psrcrmid = extractPSRCRMID(user);
		if (!psrcrmid.equalsIgnoreCase("NA")) {
			validateStockistPSRRelation(psrcrmid, supplierList, errors);
		}

		return errors;
	}

	private List<String> extractSupplierList(User user, List<String> errors) {
		List<String> supplierList = new ArrayList<>();
		if (user.getExtendedAttributes() == null ||
				!user.getExtendedAttributes().hasNonNull(SUPPLIER) ||
				objectMapper.convertValue(user.getExtendedAttributes().get(SUPPLIER), List.class).isEmpty()) {
			errors.add("supplier is missing for stockist");
		} else {
			supplierList = objectMapper.convertValue(user.getExtendedAttributes().get(SUPPLIER), List.class);
		}
		return supplierList;
	}

	private void validateSupplierPresence(List<String> supplierList, List<String> errors) {
		for (String supplier : supplierList) {
			User wdUser = userService.findByLoginId(supplier);
			if (wdUser == null) {
				errors.add("supplier is not present in database for wddest :" + supplier);
			}
		}
	}

	private String extractPSRCRMID(User user) {
		if (user.getExtendedAttributes() != null &&
				user.getExtendedAttributes().hasNonNull(PSRCRMID) &&
				StringUtils.isNotBlank(user.getExtendedAttributes().get(PSRCRMID).asText())) {
			return user.getExtendedAttributes().get(PSRCRMID).asText();
		}
		return "NA";
	}

	private void validateStockistPSRRelation(String psrcrmid, List<String> supplierList, List<String> errors) {
		User psrDB = userService.findByLoginId(psrcrmid);
		if (psrDB == null) {
			errors.add("psr is not present in database for stockist");
			return;
		}

		List<UserParent> userParents = userParentService.findByUserLoginId(psrDB.getLoginId());
		List<String> userParentList = userParents.stream()
				.map(UserParent::getParent)
				.collect(Collectors.toList());

		List<String> parentMismatch = supplierList.stream()
				.filter(supplier -> !userParentList.contains(supplier))
				.collect(Stream.toList());

		if (!parentMismatch.isEmpty()) {
			errors.add("wd " + supplierList + " is not present as psr " + psrcrmid + " parent");
		}
	}

	private List<String> validatePSR(User user) {
		List<String> errors = new ArrayList<>();

		if (user.getExtendedAttributes() == null || StringUtils.isNullOrBlank(user.getLoginId())) {
			errors.add("PSRCRMID as loginid is missing");
			return errors;
		}

		String psrcrmid = user.getLoginId();
		if (user.getUseraccountid() == null || !user.getUseraccountid().equalsIgnoreCase(psrcrmid)) {
			errors.add("useraccountid must be equal to psrcrmid");
		}

		if (isMissingAttribute(user, "Branch")) {
			errors.add("Branch is missing for the PSRCRMID");
		}

		if (isMissingAttribute(user, DS_TYPE)) {
			errors.add("DSType is missing for the PSRCRMID");
		} else if (!"PSR".equalsIgnoreCase(user.getExtendedAttributes().get(DS_TYPE).asText())) {
			errors.add("DSType should be PSR");
		}

		return errors;
	}

	private List<String> validateWD(User user) {
		List<String> errors = new ArrayList<>();
		String userAccountId = user.getUseraccountid();

		if (userAccountId == null || !userAccountId.equalsIgnoreCase(user.getLoginId())) {
			errors.add("userAccountID must match loginID or userName in wd");
		}

		if (isMissingAttribute(user, "Branch")) {
			errors.add("Branch is missing for the given WD");
		}

		return errors;
	}

	private boolean isMissingAttribute(User user, String key) {
		return user.getExtendedAttributes() == null ||
				StringUtils.isNullOrBlank(user.getExtendedAttributes().get(key).asText());
	}

	private boolean isStockist(User user) {
		return user.getDesignation() != null && user.getDesignation().contains("stockist");
	}

	private boolean isPSR(User user) {
		return user.getDesignation() != null && user.getDesignation().contains("psr");
	}

	private boolean isWD(User user) {
		return user.getDesignation() != null && user.getDesignation().contains("wd");
	}
}