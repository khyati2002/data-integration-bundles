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

import java.util.ArrayList;
import java.util.List;

public class VistaarUserValidation extends AbstractValidationRule<User> {

	public static final String DISTRICT = "district";
	public static final String BRANCH = "branch";
	public static final String USER_HAS_INVALID_IMMEDIATE_PARENT = "User has invalid immediate parent";
	private static final String REGEX_MOBILE = "^[0-9]{10}$";
	private static final String CAPITAL_CASE_BRANCH_REGEX = "^[A-Z]+[A-Z0-9!@#$&\\-.+]*$";

	@Override
	public OperationResult.StepResult apply(User user) {
		UserService userService = (UserService) ServiceLocator.lookup(User.class);
		RegexValidation regexValidation = new RegexValidation();
		StringBuilder ruleResult = new StringBuilder();

		validateDesignations(user, ruleResult, userService);
		validateCategory(user, ruleResult);
		validateLocationAndParent(user, ruleResult, userService, regexValidation);

		return !ruleResult.length().isEmpty()
				? new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString())
				: OperationResult.StepResult.OK;
	}

	// --- Designation Validation ---
	private void validateDesignations(User user, StringBuilder ruleResult, UserService userService) {
		checkDesignationPresence(user, ruleResult);
		checkInvalidDesignation(user, ruleResult);
		checkDesignationConsistency(user, ruleResult, userService);
		checkSupplierEligibility(user, ruleResult);
	}

	private void checkDesignationPresence(User user, StringBuilder ruleResult) {
		if (user.getDesignation() == null || user.getDesignation().isEmpty()) {
			ruleResult.append("No designations detected");
		} else if (!isOnlyDesignation(user)) {
			ruleResult.append("Multiple designations detected for user");
		}
	}

	private void checkInvalidDesignation(User user, StringBuilder ruleResult) {
		if (isUserDesignation(user, "retailer")) {
			ruleResult.append("User should not have a designation as retailer");
		}
	}

	private void checkDesignationConsistency(User user, StringBuilder ruleResult, UserService userService) {
		User userInDB = userService.findByLoginId(user.getLoginId());
		if (userInDB != null && !user.getDesignation().equals(userInDB.getDesignation())) {
			ruleResult.append("User designation mismatch with info present in DB user");
		}
	}

	private void checkSupplierEligibility(User user, StringBuilder ruleResult) {
		if (!isUserDesignation(user, "wd") && user.getSupplierMetaData() != null && !user.getSupplierMetaData().isEmpty()) {
			ruleResult.append("Only wd can be a supplier");
		}
	}

	// --- Category Validation ---
	private void validateCategory(User user, StringBuilder ruleResult) {
		JsonNode extendedAttributes = user.getExtendedAttributes();
		if (extendedAttributes == null || !extendedAttributes.hasNonNull("Category")) return;

		String category = extendedAttributes.get("Category").asText();
		if (StringUtils.isBlank(category)) return;

		String catLower = category.toLowerCase();
		if (catLower.equals("wd")) {
			validateCommonFields(user, ruleResult);
		} else if (catLower.equals(BRANCH)) {
			validateBranchCategory(user, ruleResult);
		} else if (catLower.equals(DISTRICT)) {
			validateDistrictCategory(user, ruleResult);
		} else {
			ruleResult.append("Category field should have either wd branch or district");
		}
	}

	private void validateDistrictCategory(User user, StringBuilder ruleResult) {
		RegexValidation regexValidation = new RegexValidation();
		if (user.getLocation().getBranch() != null) {
			ruleResult.append("category type district cannot have branch");
		}
		validateDistrictName(user, ruleResult, regexValidation);
		validateCommonFields(user, ruleResult);
	}

	private void validateDistrictName(User user, StringBuilder ruleResult, RegexValidation regexValidation) {
		String district = user.getLocation().getDistrict();
		if (district == null) {
			ruleResult.append("District can not be null");
			return;
		}
		if (!StringUtils.isNotBlank(district) || !regexValidation.match(CAPITAL_CASE_BRANCH_REGEX, district)) {
			ruleResult.append("Value given for district should contain only alphabets with capital case. Current given value is not compatible");
		}
	}

	private void validateBranchCategory(User user, StringBuilder ruleResult) {
		if (isNullOrEmpty(user.getLocation().getBranch())) {
			ruleResult.append("branch cannot be empty");
		}
		if (isNullOrEmpty(user.getLocation().getDistrict())) {
			ruleResult.append("district cannot be empty");
		}
		validateCommonFields(user, ruleResult);
	}

	private void validateCommonFields(User user, StringBuilder ruleResult) {
		JsonNode extendedAttributes = user.getExtendedAttributes();
		if (extendedAttributes == null) return;
		validateWdCode(extendedAttributes, ruleResult);
		validateMobileNumbers(user, extendedAttributes, ruleResult);
	}

	private void validateWdCode(JsonNode extendedAttributes, StringBuilder ruleResult) {
		if (extendedAttributes.hasNonNull("WDCode")
				&& StringUtils.isNotBlank(extendedAttributes.get("WDCode").asText())) {
			ruleResult.append("WDCode value should be null or empty");
		}
	}

	private void validateMobileNumbers(User user, JsonNode extendedAttributes, StringBuilder ruleResult) {
		if (user.getMobile() == null || user.getMobile().isBlank()) {
			ruleResult.append("Mobile number cannot be null or empty");
		}

		if (extendedAttributes.hasNonNull("MobileNumber2")) {
			String mobile2 = extendedAttributes.get("MobileNumber2").asText();
			if (StringUtils.isNotBlank(mobile2) && !checkMobileNumberPattern(mobile2)) {
				ruleResult.append("Mobile number 2 field allowed only 10 digit valid number or blank.");
			}
		}
	}

	// --- Location & Parent Validation ---
	private void validateLocationAndParent(User user, StringBuilder ruleResult, UserService userService, RegexValidation regexValidation) {
		if (isUserDistrict(user)) return;
		validateLocation(user, ruleResult, regexValidation);
		validateImmediateParent(user, ruleResult, userService, isUserBranch(user));
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
		} else if (!StringUtils.isNotBlank(fieldValue)
				|| !regexValidation.match(CAPITAL_CASE_BRANCH_REGEX, fieldValue)) {
			ruleResult.append("Value given for ").append(fieldName.toLowerCase())
					.append(" should contain only alphabets with capital case. Current given value is not compatible");
		}
	}

	private void validateImmediateParent(User user, StringBuilder ruleResult, UserService userService, boolean isBranch) {
		if (user.getImmediateParent() == null || user.getImmediateParent().isEmpty()) {
			ruleResult.append("immediate parent can not be null.");
			return;
		}
		if (hasInvalidParentCount(user)) {
			ruleResult.append("immediate parent can not be more than one.");
		}
		for (HierarchyMetadata immParent : user.getImmediateParent()) {
			if (processParentHierarchy(immParent, user, ruleResult, userService, isBranch)) break;
		}
	}

	private boolean hasInvalidParentCount(User user) {
		return user.getImmediateParent().size() > 1
				&& (!user.getDesignation().contains("psr")
				&& !user.getDesignation().contains("stockist"));
	}

	private boolean processParentHierarchy(HierarchyMetadata immParent, User user, StringBuilder ruleResult,
										   UserService userService, boolean isBranch) {
		String parentId = immParent.getImmediateParent();
		if (parentId == null) {
			ruleResult.append("immediate parent can not be null.");
			return true;
		}
		User dbParent = userService.findByLoginId(parentId);
		if (dbParent == null) {
			ruleResult.append("Given immediate parent is not present in database. Please verify the input data.");
			return true;
		}
		if (dbParent.getDesignation() == null) {
			userService.addDesignationFromDb(dbParent);
		}
		validateParentLocation(user, dbParent, ruleResult, isBranch);
		appendParentErrors(user, dbParent, ruleResult);
		return false;
	}

	private void appendParentErrors(User user, User dbParent, StringBuilder ruleResult) {
		List<String> errors = isValidParent(user, dbParent);
		if (!errors.isEmpty()) {
			ruleResult.append(String.join(",", errors));
		}
	}

	private void validateParentLocation(User user, User dbParent, StringBuilder ruleResult, boolean isBranch) {
		if (dbParent.getLocation() == null) return;

		if (!districtMatches(user, dbParent)) {
			ruleResult.append("District value of loginId is not matching the parent district");
		}
		if (!isBranch && !branchMatches(user, dbParent)) {
			ruleResult.append("Branch value of loginId is not matching the parent branch");
		}
	}

	private boolean districtMatches(User user, User dbParent) {
		String district = dbParent.getLocation().getDistrict();
		return district == null || district.equalsIgnoreCase(user.getLocation().getDistrict());
	}

	private boolean branchMatches(User user, User dbParent) {
		String branch = dbParent.getLocation().getBranch();
		return branch == null || branch.equalsIgnoreCase(user.getLocation().getBranch());
	}

	// --- Utility Methods ---
	private boolean checkMobileNumberPattern(String mobile) {
		return new RegexValidation().match(REGEX_MOBILE, mobile);
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
		return user.getDesignation() != null &&
				user.getDesignation().stream().anyMatch(designationToMatch::equalsIgnoreCase);
	}

	private boolean isNullOrEmpty(String value) {
		return value == null || value.isEmpty();
	}

	private List<String> isValidParent(User user, User dbParent) {
		List<String> errors = new ArrayList<>();
		for (String designation : user.getDesignation()) {
			if (isInvalidParentCombination(designation, dbParent)) {
				errors.add(StringUtils.format(USER_HAS_INVALID_IMMEDIATE_PARENT));
			}
		}
		return errors;
	}

	private boolean isInvalidParentCombination(String designation, User dbParent) {
		return (BRANCH.equals(designation) && !isUserDistrict(dbParent))
				|| ("wd".equals(designation) && !isUserBranch(dbParent))
				|| ("psr".equals(designation) && !isUserDesignation(dbParent, "wd"))
				|| ("stockist".equals(designation)
				&& !isUserDesignation(dbParent, "psr")
				&& !isUserDesignation(dbParent, "wd"));
	}
}
