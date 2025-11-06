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

<<<<<<< Updated upstream
	public static final String DISTRICT = "district";
	public static final String BRANCH = "branch";
	public static final String USER_HAS_INVALID_IMMEDIATE_PARENT = "User has invalid immediate parent";
	final UserService userService = (UserService) ServiceLocator.lookup(User.class);
	static final String capitalCaseRegex = "(^[A-Z\\s]*$)";
	String regex = "(^[0-9]{10}$)";
	static final String capitalCaseBranchRegex = "^[A-Z]+[A-Z0-9!@#$&\\-.+]*$";

	@Override
	public OperationResult.StepResult apply(User user) {
		return OperationResult.StepResult.OK;
	}
//		RegexValidation regexValidation = new RegexValidation();
//		StringBuilder ruleResult = new StringBuilder();
//
//		if (user.getDesignation() == null || user.getDesignation().isEmpty()) {
//			ruleResult.append("No designations detected");
//		}
//
//		if (!isOnlyDesignation(user)) {
//			ruleResult.append("Multiple designations detected for user");
//		}
//
//		if (isUserDesignation(user, "retailer")) {
//			ruleResult.append("User should not have a designation as retailer");
//		}
//
//		List<User> userinDB = userService.findUserByQuery("select u from User u where u.loginId='" + user.getLoginId() + "'", false);
//		if (userinDB != null && userinDB.size() == 1 && !user.getDesignation().equals(userinDB.get(0).getDesignation())) {
//			ruleResult.append("User designation mismatch with info present in DB user");
//		}
//		if (!user.getDesignation("wd") && user.getSupplierMetaData() != null && !user.getSupplierMetaData().isEmpty()) {
//			ruleResult.append("Only wd can be a supplier");
//		}
//
//		JsonNode extendedAttributes = user.getExtendedAttributes();
//		if (extendedAttributes != null && extendedAttributes.hasNonNull("Category")) {
//			String category = extendedAttributes.get("Category").asText();
//			if (category != null && !category.isBlank() && !(category.equalsIgnoreCase("wd") || category.equalsIgnoreCase(BRANCH) || category.equalsIgnoreCase(DISTRICT))) {
//				ruleResult.append("Category field should have either wd branch or district");
//			}
//			if (category.equalsIgnoreCase(DISTRICT)) {
//				if (user.getLocationHierarchy().getBranch() != null) {
//					ruleResult.append("category type district cannot have branch");
//				}
//				if (user.getLocationHierarchy().getDistrict() != null) {
//					if (!StringUtils.isNotBlank(user.getLocationHierarchy().getDistrict()) || !regexValidation.match(capitalCaseBranchRegex, user.getLocationHierarchy().getDistrict())) {
//						ruleResult.append(
//								"Value given for district should contain only alphabets with capital case.Current given value is not compatible");
//					}
//				} else {
//					ruleResult.append("District can not be null");
//				}
//				String wd = extendedAttributes.get("WDCode").asText();
//				if (wd != null && !wd.isBlank()) {
//					ruleResult.append("WDCode value should be null or empty");
//				}
//				if (user.getMobile() == null || user.getMobile().isBlank())
//					ruleResult.append("Mobile number cannot be null or empty");
//				String mobile = extendedAttributes.get("MobileNumber2").asText();
//				if (mobile != null && !mobile.isEmpty() && !checkMobileNumberPattern(mobile))
//					ruleResult.append("Mobile number 2 field allowed only 10 digit valid number or blank.");
//
//			} else if (category.equalsIgnoreCase(BRANCH)) {
//				if (user.getLocationHierarchy().getBranch() == null || user.getLocationHierarchy().getBranch().isEmpty()) {
//					ruleResult.append("branch cannot be empty");
//				}
//				if (user.getLocationHierarchy().getDistrict() == null || user.getLocationHierarchy().getDistrict().isEmpty()) {
//					ruleResult.append("district cannot be empty");
//				}
//				String wd = extendedAttributes.get("WDCode").asText();
//				if (wd != null && !wd.isBlank()) {
//					ruleResult.append("WDCode value should be null or empty");
//				}
//				if (user.getMobile() == null || user.getMobile().isBlank())
//					ruleResult.append("Mobile number cannot be null or empty");
//				String mobile = extendedAttributes.get("MobileNumber2").asText();
//				if (mobile != null && !mobile.isEmpty() && !checkMobileNumberPattern(mobile))
//					ruleResult.append("Mobile number 2 field allowed only 10 digit valid number or blank.");
//
//			}
//		}
//
//
//		boolean isuserDistrict = isUserDistrict(user);
//		boolean isuserBranch = isUserBranch(user);
//		if (!isuserDistrict) {
//			if (user.getLocationHierarchy() == null) {
//				ruleResult.append("location value cannot be null or empty");
//			}
//			if (user.getLocationHierarchy().getDistrict() != null) {
//				if (!StringUtils.isNotBlank(user.getLocationHierarchy().getDistrict()) || !regexValidation.match(capitalCaseBranchRegex, user.getLocationHierarchy().getDistrict())) {
//					ruleResult.append(
//							"Value given for district should contain only alphabets with capital case.Current given value is not compatible");
//				}
//			} else {
//				ruleResult.append("District can not be null");
//			}
//
//			if (user.getLocationHierarchy().getBranch() != null) {
//				if (!StringUtils.isNotBlank(user.getLocationHierarchy().getBranch()) || !regexValidation.match(capitalCaseBranchRegex, user.getLocationHierarchy().getBranch())) {
//					ruleResult.append(
//							"Value given for branch should contain only alphabets with capital case.Current given value is not compatible");
//				}
//			} else {
//				ruleResult.append("Branch can not be null");
//			}
//
//			if (user.getImmediateParent() == null || user.getImmediateParent().isEmpty()) {
//				ruleResult.append("immediate parent can not be null.");
//			} else {
//				if (user.getImmediateParent().size() > 1 && (!user.getDesignation().contains("psr") ? !user.getDesignation().contains("stockist") : false)) {
//					ruleResult.append("immediate parent can not be more than one.");
//				}
//				for (HierarchyMetadata immParent : user.getImmediateParent()) {
//					String parentId = immParent.getImmediateParent();
//					if (parentId == null) {
//						ruleResult.append("immediate parent can not be null.");
//						break;
//					}
//					User dbparent = userService.findByLoginId(parentId);
//					if (dbparent == null) {
//						ruleResult.append("Given immediate parent is not present in database.Please verify the input data.");
//						break;
//					}
//
//					if (!isuserDistrict && dbparent.getLocationHierarchy() != null) {
//						String district = dbparent.getLocationHierarchy().getDistrict();
//						String branch = dbparent.getLocationHierarchy().getBranch();
//
//						if (district != null && !district.equalsIgnoreCase(user.getLocationHierarchy().getDistrict())) {
//							ruleResult.append("District value of loginId is not matching the parent district");
//						}
//						if (!isuserBranch && branch != null && !branch.equalsIgnoreCase(user.getLocationHierarchy().getBranch())) {
//							ruleResult.append("Branch value of loginId is not matching the parent branch");
//						}
//					}
//
//					List<String> errors = isValidParent(user, dbparent);
//					if (!errors.isEmpty()) {
//						ruleResult.append(String.join(",", errors));
//					}
//				}
//			}
//
//		}
//
//		if (ruleResult.length() > 0) {
//			return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
//		} else {
//			return OperationResult.StepResult.OK;
//		}
//	}
//
//	private boolean checkMobileNumberPattern(String mobile) {
//		RegexValidation regexValidation = new RegexValidation();
//		return regexValidation.match(regex, mobile);
//	}
//
//	private boolean isOnlyDesignation(User user) {
//		return user.getDesignation().size() == 1;
//	}
//
//	private boolean isUserDistrict(User user) {
//		return user.getDesignation().contains(DISTRICT);
//	}
//
//	private boolean isUserBranch(User user) {
//		return user.getDesignation().contains(BRANCH);
//	}
//
//	private boolean isUserDesignation(User user, String designation) {
//		return user.getDesignation().contains(designation);
//	}
//
//	private List<String> isValidParent(User user, User dbParent) {
//		List<String> errors = new ArrayList<>();
//		user.getDesignation().stream().forEach((designation) -> {
//			switch (designation) {
//				case BRANCH:
//					if (!isUserDistrict(dbParent)) {
//						errors.add(StringUtils.format(USER_HAS_INVALID_IMMEDIATE_PARENT));
//					}
//					break;
//				case "wd":
//					if (!isUserBranch(dbParent)) {
//						errors.add(StringUtils.format(USER_HAS_INVALID_IMMEDIATE_PARENT));
//					}
//					break;
//				case "psr":
//					if (!isUserDesignation(dbParent, "wd")) {
//						errors.add(StringUtils.format(USER_HAS_INVALID_IMMEDIATE_PARENT));
//					}
//					break;
//				case "stockist":
//					if (!isUserDesignation(dbParent, "psr") && !isUserDesignation(dbParent, "wd")) {
//						errors.add(StringUtils.format(("User has invalid immediate parent ")));
//					}
//					break;
//			}
//		});
//		return errors;


}
=======
    public static final String DISTRICT = "district";
    public static final String BRANCH = "branch";
    public static final String USER_HAS_INVALID_IMMEDIATE_PARENT = "User has invalid immediate parent";
    final UserService userService = (UserService) ServiceLocator.lookup(User.class);
    static final String capitalCaseRegex = "(^[A-Z\\s]*$)";
    String regex = "(^[0-9]{10}$)";
    static final String capitalCaseBranchRegex = "^[A-Z]+[A-Z0-9!@#$&\\-.+]*$";

    @Override
    public OperationResult.StepResult apply(User user) {
        RegexValidation regexValidation = new RegexValidation();
        StringBuilder ruleResult = new StringBuilder();

        if (user.getDesignation() == null || user.getDesignation().isEmpty()) {
            ruleResult.append("No designations detected");
        }

        if (!isOnlyDesignation(user)) {
            ruleResult.append("Multiple designations detected for user");
        }

        if (isUserDesignation(user, "retailer")) {
            ruleResult.append("User should not have a designation as retailer");
        }

        User userinDB = userService.findByLoginId(user.getLoginId());
        if (userinDB != null && !user.getDesignation().equals(userinDB.getDesignation())) {
            ruleResult.append("User designation mismatch with info present in DB user");
        }
        if (!isUserDesignation(user, "wd") && user.getSupplierMetaData() != null && !user.getSupplierMetaData().isEmpty()) {
            ruleResult.append("Only wd can be a supplier");
        }

        JsonNode extendedAttributes = user.getExtendedAttributes();
        if (extendedAttributes != null && extendedAttributes.hasNonNull("Category")) {
            String category = extendedAttributes.get("Category").asText();
            if (category != null && !category.isBlank() && !(category.equalsIgnoreCase("wd") || category.equalsIgnoreCase(BRANCH) || category.equalsIgnoreCase(DISTRICT))) {
                ruleResult.append("Category field should have either wd branch or district");
            }
            if (category.equalsIgnoreCase(DISTRICT)) {
                if (user.getLocation().getBranch() != null) {
                    ruleResult.append("category type district cannot have branch");
                }
                if (user.getLocation().getDistrict() != null) {
                    if (!StringUtils.isNotBlank(user.getLocation().getDistrict()) || !regexValidation.match(capitalCaseBranchRegex, user.getLocation().getDistrict())) {
                        ruleResult.append(
                                "Value given for district should contain only alphabets with capital case.Current given value is not compatible");
                    }
                } else {
                    ruleResult.append("District can not be null");
                }
                String wd = extendedAttributes.get("WDCode").asText();
                if (wd != null && !wd.isBlank()) {
                    ruleResult.append("WDCode value should be null or empty");
                }
                if (user.getMobile() == null || user.getMobile().isBlank())
                    ruleResult.append("Mobile number cannot be null or empty");
                String mobile = extendedAttributes.get("MobileNumber2").asText();
                if (mobile != null && !mobile.isEmpty() && !checkMobileNumberPattern(mobile))
                    ruleResult.append("Mobile number 2 field allowed only 10 digit valid number or blank.");

            } else if (category.equalsIgnoreCase(BRANCH)) {
                if (user.getLocation().getBranch() == null || user.getLocation().getBranch().isEmpty()) {
                    ruleResult.append("branch cannot be empty");
                }
                if (user.getLocation().getDistrict() == null || user.getLocation().getDistrict().isEmpty()) {
                    ruleResult.append("district cannot be empty");
                }
                String wd = extendedAttributes.get("WDCode").asText();
                if (wd != null && !wd.isBlank()) {
                    ruleResult.append("WDCode value should be null or empty");
                }
                if (user.getMobile() == null || user.getMobile().isBlank())
                    ruleResult.append("Mobile number cannot be null or empty");
                String mobile = extendedAttributes.get("MobileNumber2").asText();
                if (mobile != null && !mobile.isEmpty() && !checkMobileNumberPattern(mobile))
                    ruleResult.append("Mobile number 2 field allowed only 10 digit valid number or blank.");

            }
        }


        boolean isuserDistrict = isUserDistrict(user);
        boolean isuserBranch = isUserBranch(user);
        if (!isuserDistrict) {
            if (user.getLocationHierarchy() == null) {
                ruleResult.append("location value cannot be null or empty");
            }
            if (user.getLocation().getDistrict() != null) {
                if (!StringUtils.isNotBlank(user.getLocation().getDistrict()) || !regexValidation.match(capitalCaseBranchRegex, user.getLocation().getDistrict())) {
                    ruleResult.append(
                            "Value given for district should contain only alphabets with capital case.Current given value is not compatible");
                }
            } else {
                ruleResult.append("District can not be null");
            }

            if (user.getLocation().getBranch() != null) {
                if (!StringUtils.isNotBlank(user.getLocation().getBranch()) || !regexValidation.match(capitalCaseBranchRegex, user.getLocation().getBranch())) {
                    ruleResult.append(
                            "Value given for branch should contain only alphabets with capital case.Current given value is not compatible");
                }
            } else {
                ruleResult.append("Branch can not be null");
            }

            if (user.getImmediateParent() == null || user.getImmediateParent().isEmpty()) {
                ruleResult.append("immediate parent can not be null.");
            } else {
                if (user.getImmediateParent().size() > 1 && (!user.getDesignation().contains("psr") ? !user.getDesignation().contains("stockist") : false)) {
                    ruleResult.append("immediate parent can not be more than one.");
                }
                for (HierarchyMetadata immParent : user.getImmediateParent()) {
                    String parentId = immParent.getImmediateParent();
                    if (parentId == null) {
                        ruleResult.append("immediate parent can not be null.");
                        break;
                    }
                    User dbparent = userService.findByLoginId(parentId);
                    if (dbparent == null) {
                        ruleResult.append("Given immediate parent is not present in database.Please verify the input data.");
                        break;
                    }

                    if (!isuserDistrict && dbparent.getLocation() != null) {
                        String district = dbparent.getLocation().getDistrict();
                        String branch = dbparent.getLocation().getBranch();

                        if (district != null && !district.equalsIgnoreCase(user.getLocation().getDistrict())) {
                            ruleResult.append("District value of loginId is not matching the parent district");
                        }
                        if (!isuserBranch && branch != null && !branch.equalsIgnoreCase(user.getLocation().getBranch())) {
                            ruleResult.append("Branch value of loginId is not matching the parent branch");
                        }
                    }

                    List<String> errors = isValidParent(user, dbparent);
                    if (!errors.isEmpty()) {
                        ruleResult.append(String.join(",", errors));
                    }
                }
            }

        }

        if (ruleResult.length() > 0) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
        } else {
            return OperationResult.StepResult.OK;
        }
    }

    private boolean checkMobileNumberPattern(String mobile) {
        RegexValidation regexValidation = new RegexValidation();
        return regexValidation.match(regex, mobile);
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
                .anyMatch(d -> designationToMatch.equalsIgnoreCase(d));
    }



    private List<String> isValidParent(User user, User dbParent) {
        List<String> errors = new ArrayList<>();
        user.getDesignation().stream().forEach((designation) -> {
            switch (designation) {
                case BRANCH:
                    if (!isUserDistrict(dbParent)) {
                        errors.add(StringUtils.format(USER_HAS_INVALID_IMMEDIATE_PARENT));
                    }
                    break;
                case "wd":
                    if (!isUserBranch(dbParent)) {
                        errors.add(StringUtils.format(USER_HAS_INVALID_IMMEDIATE_PARENT));
                    }
                    break;
                case "psr":
                    if (!isUserDesignation(dbParent, "wd")) {
                        errors.add(StringUtils.format(USER_HAS_INVALID_IMMEDIATE_PARENT));
                    }
                    break;
                case "stockist":
                    if (!isUserDesignation(dbParent, "psr") && !isUserDesignation(dbParent, "wd")) {
                        errors.add(StringUtils.format(("User has invalid immediate parent ")));
                    }
                    break;
            }
        });
        return errors;
    }

}
>>>>>>> Stashed changes
