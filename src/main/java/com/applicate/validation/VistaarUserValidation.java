package com.applicate.validation;

import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class VistaarUserValidation extends AbstractValidationRule<User> {

	public static final String DISTRICT = "district";
	public static final String BRANCH = "branch";
	public static final String USER_HAS_INVALID_IMMEDIATE_PARENT = "User has invalid immediate parent";
	String regex = "(^[0-9]{10}$)";
	static final String CAPITAL_CASE_BRANCH_REGEX = "^[A-Z]+[A-Z0-9!@#$&\\-.+]*$";

	@Override
	public OperationResult.StepResult apply(User user) {
		UserService userService = (UserService) ServiceLocator.lookup(User.class);
		RegexValidation regexValidation = new RegexValidation();
		StringBuilder ruleResult = new StringBuilder();

		validateDesignations(user, ruleResult, userService);
		validateCategory(user, ruleResult);
		validateLocationAndParent(user, ruleResult, userService, regexValidation);

		if (ruleResult.length() != 0) {
			return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
		}
		return OperationResult.StepResult.OK;
	}

	private void validateDesignations(User user, StringBuilder ruleResult, UserService userService) {
		if (user.getDesignation() == null || user.getDesignation().isEmpty()) {
			ruleResult.append("No designations detected");
		} else if (!isOnlyDesignation(user)) {
			ruleResult.append("Multiple designations detected for user");
		}

		if (isUserDesignation(user, "retailer")) {
			ruleResult.append("User should not have a designation as retailer");
		}

		User userInDB = userService.findByLoginId(user.getLoginId());
		if (userInDB != null && !user.getDesignation().equals(userInDB.getDesignation())) {
			ruleResult.append("User designation mismatch with info present in DB user");
		}

		if (!isUserDesignation(user, "wd") && user.getSupplierMetaData() != null && !user.getSupplierMetaData().isEmpty()) {
			ruleResult.append("Only wd can be a supplier");
		}
	}

	private void validateCategory(User user, StringBuilder ruleResult) {
		JsonNode extendedAttributes = user.getExtendedAttributes();
		if (extendedAttributes == null || !extendedAttributes.hasNonNull("Category")) {
			return;
		}

		String category = extendedAttributes.get("Category").asText();
		if (category == null || category.isBlank()) {
			return;
		}

		if (!(category.equalsIgnoreCase("wd") || category.equalsIgnoreCase(BRANCH) || category.equalsIgnoreCase(DISTRICT))) {
			ruleResult.append("Category field should have either wd branch or district");
			return;
		}

		if (category.equalsIgnoreCase(DISTRICT)) {
			validateDistrictCategory(user, extendedAttributes, ruleResult);
		} else if (category.equalsIgnoreCase(BRANCH)) {
			validateBranchCategory(user, extendedAttributes, ruleResult);
		}
	}

	private void validateDistrictCategory(User user, JsonNode extendedAttributes, StringBuilder ruleResult) {
		RegexValidation regexValidation = new RegexValidation();
		if (user.getLocation().getBranch() != null) {
			ruleResult.append("category type district cannot have branch");
		}
		if (user.getLocation().getDistrict() == null) {
			ruleResult.append("District can not be null");
		} else if (!StringUtils.isNotBlank(user.getLocation().getDistrict())
				|| !regexValidation.match(CAPITAL_CASE_BRANCH_REGEX, user.getLocation().getDistrict())) {
			ruleResult.append("Value given for district should contain only alphabets with capital case.Current given value is not compatible");
		}

		validateCommonFields(user, extendedAttributes, ruleResult);
	}

	private void validateBranchCategory(User user, JsonNode extendedAttributes, StringBuilder ruleResult) {
		if (isNullOrEmpty(user.getLocation().getBranch())) {
			ruleResult.append("branch cannot be empty");
		}
		if (isNullOrEmpty(user.getLocation().getDistrict())) {
			ruleResult.append("district cannot be empty");
		}
		validateCommonFields(user, extendedAttributes, ruleResult);
	}

	private void validateCommonFields(User user, JsonNode extendedAttributes, StringBuilder ruleResult) {
		String wd = extendedAttributes.get("WDCode").asText();
		if (wd != null && !wd.isBlank()) {
			ruleResult.append("WDCode value should be null or empty");
		}

		if (user.getMobile() == null || user.getMobile().isBlank()) {
			ruleResult.append("Mobile number cannot be null or empty");
		}

		String mobile = extendedAttributes.get("MobileNumber2").asText();
		if (mobile != null && !mobile.isEmpty() && !checkMobileNumberPattern(mobile)) {
			ruleResult.append("Mobile number 2 field allowed only 10 digit valid number or blank.");
		}
	}

	private void validateLocationAndParent(User user, StringBuilder ruleResult, UserService userService, RegexValidation regexValidation) {
		boolean isDistrict = isUserDistrict(user);
		boolean isBranch = isUserBranch(user);

		if (isDistrict) {
			return;
		}

		validateLocation(user, ruleResult, regexValidation);
		validateImmediateParent(user, ruleResult, userService, isBranch);
	}

	private void validateLocation(User user, StringBuilder ruleResult, RegexValidation regexValidation) {
		if (user.getLocationHierarchy() == null) {
			ruleResult.append("location value cannot be null or empty");
		}
		validateLocationField(user.getLocation().getDistrict(), "District", ruleResult, regexValidation);
		validateLocationField(user.getLocation().getBranch(), "Branch", ruleResult, regexValidation);
	}

	private void validateLocationField(String fieldValue, String fieldName, StringBuilder ruleResult, RegexValidation regexValidation) {
		if (fieldValue == null) {
			ruleResult.append(fieldName).append(" can not be null");
		} else if (!StringUtils.isNotBlank(fieldValue) || !regexValidation.match(CAPITAL_CASE_BRANCH_REGEX, fieldValue)) {
			ruleResult.append("Value given for ").append(fieldName.toLowerCase())
					.append(" should contain only alphabets with capital case.Current given value is not compatible");
		}
	}

	private void validateImmediateParent(User user, StringBuilder ruleResult, UserService userService, boolean isBranch) {
		if (user.getImmediateParent() == null || user.getImmediateParent().isEmpty()) {
			ruleResult.append("immediate parent can not be null.");
			return;
		}

		if (user.getImmediateParent().size() > 1
				&& (!user.getDesignation().contains("psr") && !user.getDesignation().contains("stockist"))) {
			ruleResult.append("immediate parent can not be more than one.");
		}

		for (HierarchyMetadata immParent : user.getImmediateParent()) {
			String parentId = immParent.getImmediateParent();
			User dbParent = userService.findByLoginId(parentId);
			if (!validateParent(parentId, ruleResult)) {
				break;
			}
			if (dbParent.getDesignation() == null) {
				userService.addDesignationFromDb(dbParent);
			}

			validateParentLocation(user, dbParent, ruleResult, isBranch);
			List<String> errors = isValidParent(user, dbParent);
			if (!errors.isEmpty()) {
				ruleResult.append(String.join(",", errors));
			}
		}
	}

	private boolean validateParent(String parentId, StringBuilder ruleResult) {
		UserService userService = (UserService) ServiceLocator.lookup(User.class);
		if (parentId == null) {
			ruleResult.append("Immediate parent cannot be null.");
			return false;
		}

		User dbParent = userService.findByLoginId(parentId);
		if (dbParent == null) {
			ruleResult.append("Given immediate parent is not present in database. Please verify the input data.");
			return false;
		}

		return true;
	}

	private void validateParentLocation(User user, User dbParent, StringBuilder ruleResult,
										boolean isBranch) {
		if (dbParent.getLocation() == null) {
			return;
		}

		String district = dbParent.getLocation().getDistrict();
		String branch = dbParent.getLocation().getBranch();

		if (district != null && !district.equalsIgnoreCase(user.getLocation().getDistrict())) {
			ruleResult.append("District value of loginId is not matching the parent district");
		}
		if (!isBranch && branch != null && !branch.equalsIgnoreCase(user.getLocation().getBranch())) {
			ruleResult.append("Branch value of loginId is not matching the parent branch");
		}
	}

	private boolean checkMobileNumberPattern(String mobile) {
		return new RegexValidation().match(regex, mobile);
	}

	private boolean isOnlyDesignation(User user) {
		return user.getDesignation().size() == 1;
	}

	private boolean isUserDistrict(User user) {
		return user.getDesignation().contains(DISTRICT);
	}

	private boolean isUserBranch(User user) {
		return user.getDesignation().contains(BRANCH);
	}

	private boolean isUserDesignation(User user, String designationToMatch) {
		if (user.getDesignation() == null || designationToMatch == null) {
			return false;
		}
		return user.getDesignation().stream()
				.anyMatch(designationToMatch::equalsIgnoreCase);
	}

	private boolean isNullOrEmpty(String value) {
		return value == null || value.isEmpty();
	}

	private List<String> isValidParent(User user, User dbParent) {
		List<String> errors = new ArrayList<>();
		for (String designation : user.getDesignation()) {
			if (isInvalidParentForDesignation(designation, dbParent)) {
				errors.add(StringUtils.format(USER_HAS_INVALID_IMMEDIATE_PARENT));
			}
		}
		return errors;
	}

	private boolean isInvalidParentForDesignation(String designation, User dbParent) {
		switch (designation) {
			case BRANCH:
				return !isUserDistrict(dbParent);
			case "wd":
				return !isUserBranch(dbParent);
			case "psr":
				return !isUserDesignation(dbParent, "wd");
			case "stockist":
				return !isUserDesignation(dbParent, "psr") && !isUserDesignation(dbParent, "wd");
			default:
				return false;
		}
	}
}
