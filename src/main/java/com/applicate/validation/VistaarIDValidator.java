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

		if (isDesignation(user, "stockist")) {
			errors.addAll(validateStockist(user));
		} else if (isDesignation(user, "psr")) {
			errors.addAll(validatePSR(user));
		} else if (isDesignation(user, "wd")) {
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

	private boolean isDesignation(User user, String type) {
		return user.getDesignation() != null && user.getDesignation().contains(type);
	}

	private List<String> validateStockist(User user) {
		List<String> errors = new ArrayList<>();

		if (StringUtils.isNullOrBlank(user.getLoginId())) {
			errors.add("UID is missing for stockist");
		}

		List<String> wddestList = extractSupplierList(user, errors);
		validateSuppliersExist(wddestList, errors);

		validatePSRParentLink(user, wddestList, errors);

		return errors;
	}

	private List<String> extractSupplierList(User user, List<String> errors) {
		if (user.getExtendedAttributes() == null ||
				!user.getExtendedAttributes().hasNonNull(SUPPLIER) ||
				objectMapper.convertValue(user.getExtendedAttributes().get(SUPPLIER), List.class).isEmpty()) {
			errors.add("supplier is missing for stockist");
			return new ArrayList<>();
		}
		return objectMapper.convertValue(user.getExtendedAttributes().get(SUPPLIER), List.class);
	}

	private void validateSuppliersExist(List<String> wddestList, List<String> errors) {
		for (String wddest : wddestList) {
			User wduser = userService.findByLoginId(wddest);
			if (wduser == null) {
				errors.add("supplier is not present in database for wddest :" + wddest);
			}
		}
	}

	private void validatePSRParentLink(User user, List<String> wddestList, List<String> errors) {
		String psrcrmid = getPSRCRMID(user);
		if (psrcrmid.equalsIgnoreCase("NA")) return;

		User psrDB = userService.findByLoginId(psrcrmid);
		if (psrDB == null) {
			errors.add("psr is not present in database for stockist");
			return;
		}

		List<UserParent> userParents = userParentService.findByUserLoginId(psrDB.getLoginId());
		List<String> userParentList = userParents.stream().map(UserParent::getParent).collect(Collectors.toList());

		List<String> parentMismatch = wddestList.stream()
				.filter(wdDest -> !userParentList.contains(wdDest))
				.collect(Collectors.toList());

		if (!parentMismatch.isEmpty()) {
			errors.add("wd " + wddestList + " is not present as psr " + psrcrmid + " parent");
		}
	}

	private String getPSRCRMID(User user) {
		if (user.getExtendedAttributes() != null &&
				user.getExtendedAttributes().hasNonNull(PSRCRMID) &&
				StringUtils.isNotBlank(user.getExtendedAttributes().get(PSRCRMID).asText())) {
			return user.getExtendedAttributes().get(PSRCRMID).asText();
		}
		return "NA";
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

		validatePSRExtendedAttributes(user, errors);
		return errors;
	}

	private void validatePSRExtendedAttributes(User user, List<String> errors) {
		if (StringUtils.isNullOrBlank(user.getExtendedAttributes().get("Branch").asText())) {
			errors.add("Branch is missing for the PSRCRMID");
		}
		if (StringUtils.isNullOrBlank(user.getExtendedAttributes().get(DS_TYPE).asText())) {
			errors.add("DSType is missing for the PSRCRMID");
		} else if (!user.getExtendedAttributes().get(DS_TYPE).asText().equals("PSR")) {
			errors.add("DSType should be PSR");
		}
	}

	private List<String> validateWD(User user) {
		List<String> errors = new ArrayList<>();

		if (user.getUseraccountid() == null || !user.getUseraccountid().equalsIgnoreCase(user.getLoginId())) {
			errors.add("userAccountID must match loginID or userName in wd");
		}
		if (user.getExtendedAttributes() == null || StringUtils.isNullOrBlank(user.getExtendedAttributes().get("Branch").asText())) {
			errors.add("Branch is missing for the given WD");
		}
		return errors;
	}

	public boolean checkStringRegex(String colour) {
		RegexValidation regexValidation = new RegexValidation();
		return regexValidation.match(regex, colour);
	}
}
