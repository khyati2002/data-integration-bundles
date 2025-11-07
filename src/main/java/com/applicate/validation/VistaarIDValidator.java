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
	//final UserService userService = (UserService) ServiceLocator.lookup(User.class);
	//final UserParentService userParentService = (UserParentService) ServiceLocator.lookup(UserParent.class);
	private transient UserService userService;
	private transient UserParentService userParentService;
	String regex = "^[a-zA-Z]*$";
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public OperationResult.StepResult apply(User user) {

		if (this.userService == null) {
			this.userService = (UserService) ServiceLocator.lookup(User.class);
		}
		if (this.userParentService == null) {
			this.userParentService = (UserParentService) ServiceLocator.lookup(UserParent.class);
		}

		List<String> errors = new ArrayList<>();
		if (user.getDesignation() != null && user.getDesignation().contains("stockist")) {

			if (StringUtils.isNullOrBlank(user.getLoginId())) {
				errors.add("UID is missing for stockist");
			}

			List<String> wddestList = new ArrayList<>();
			if (user.getExtendedAttributes() == null || !user.getExtendedAttributes().hasNonNull(SUPPLIER) || objectMapper.convertValue(user.getExtendedAttributes().get(SUPPLIER), List.class).isEmpty()) {
				errors.add("supplier is missing for stockist");
			} else {
				wddestList = objectMapper.convertValue(user.getExtendedAttributes().get(SUPPLIER), List.class);
				for (String wddest : wddestList) {
					User wduser = userService.findByLoginId(wddest);
					if (wduser == null) {
						errors.add("supplier is not present in database for wddest :" + wddest);
					}
				}
			}

			String psrcrmid = "NA";
			if (user.getExtendedAttributes() != null && user.getExtendedAttributes().hasNonNull(PSRCRMID) && StringUtils.isNotBlank(user.getExtendedAttributes().get(PSRCRMID).asText())) {
				psrcrmid = user.getExtendedAttributes().get(PSRCRMID).asText();
			}
			if (!psrcrmid.equalsIgnoreCase("NA")) {
				User psrDB = userService.findByLoginId(psrcrmid);
				if (psrDB == null) {
					errors.add("psr is not present in database for stockist");
				} else {
					List<UserParent> userParents = userParentService.findByUserLoginId(psrDB.getLoginId());
					List<String> userParentList = userParents.stream().map(UserParent::getParent).collect(Collectors.toList());

					List<String> parentMismatch = wddestList.stream().filter(wdDest -> !userParentList.contains(wdDest)).collect(Collectors.toList());
					if (!parentMismatch.isEmpty()) {
						errors.add("wd " + wddestList + " is not present as psr " + psrcrmid + " parent");
					}
				}
			}
		}

		if (user.getDesignation() != null && user.getDesignation().contains("psr")) {
			String psrcrmid = "";
			if (user.getExtendedAttributes() == null || StringUtils.isNullOrBlank(user.getLoginId())) {
				errors.add("PSRCRMID as loginid is missing");
			} else {
				psrcrmid = user.getLoginId();
				if (user.getUseraccountid() == null || !user.getUseraccountid().equalsIgnoreCase(psrcrmid)) {
					errors.add("useraccountid must be equal to psrcrmid");
				}
			}
			if (user.getExtendedAttributes() == null || StringUtils.isNullOrBlank(user.getExtendedAttributes().get("Branch").asText())) {
				errors.add("Branch is missing for the PSRCRMID");
			}
			if (user.getExtendedAttributes() == null || StringUtils.isNullOrBlank(user.getExtendedAttributes().get(DS_TYPE).asText())) {
				errors.add("DSType is missing for the PSRCRMID");
			}

			if (user.getExtendedAttributes() != null && !StringUtils.isNullOrBlank(user.getExtendedAttributes().get(DS_TYPE).asText()) && !user.getExtendedAttributes().get(DS_TYPE).asText().equals("PSR")) {
				errors.add("DSType should be PSR");
			}
		}

		if (user.getDesignation() != null && user.getDesignation().contains("wd")) {
			String userAccountId = user.getUseraccountid();
			if (userAccountId == null || !userAccountId.equalsIgnoreCase(user.getLoginId())) {
				errors.add("userAccountID must match loginID or userName in wd");
			}
			if (user.getExtendedAttributes() == null || StringUtils.isNullOrBlank(user.getExtendedAttributes().get("Branch").asText())) {
				errors.add("Branch is missing for the given WD");
			}
		}

		if (!errors.isEmpty()) {
			return new OperationResult.StepResult(OperationResult.Status.ERROR, String.join(",", errors));
		}
		return OperationResult.StepResult.OK;
	}

	public boolean checkStringRegex(String colour) {
		RegexValidation regexValidation = new RegexValidation();
		return regexValidation.match(regex, colour);
	}
}