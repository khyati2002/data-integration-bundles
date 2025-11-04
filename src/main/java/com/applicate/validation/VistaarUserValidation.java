package com.applicate.validation;

import com.applicate.services.channelkart.models.HierarchyMetaData;
//import com.applicate.services.channelkart.models.User;
import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.OperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;

public class VistaarUserValidation extends AbstractValidationRule<User> {

    final UserService userService = (UserService) ServiceLocator.lookup(UserService.class);
    final String capitalCaseRegex = "(^[A-Z\\s]*$)";
    String regex = "(^[0-9]{10}$)";
    final String capitalCaseBranchRegex = "^[A-Z]+[A-Z0-9!@#$&\\-.+]*$";
    private ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private Validator validator = factory.getValidator();
    private static final Logger logger = LoggerFactory.getLogger(VistaarUserValidation.class);


    @Override
    public OperationResult.StepResult apply(User user) {
        RegexValidation regexValidation = new RegexValidation();
        StringBuilder ruleResult = new StringBuilder();

        if(user.getDesignation() == null || user.getDesignation().isEmpty()) {
        	ruleResult.append("No designations detected");
        }

        if(!isOnlyDesignation(user)) {
        	ruleResult.append("Multiple designations detected for user");
        }

        if(isUserDesignation(user, "retailer")) {
        	ruleResult.append("User should not have a designation as retailer");
        }
        
        List<User> userinDB = userService.findUserByQuery("select u from User u where u.loginId='"+user.getLoginId()+"'", false);
        if(userinDB != null && userinDB.size() ==1 && !user.getDesignation().equals(userinDB.get(0).getDesignation())) {
        	ruleResult.append("User designation mismatch with info present in DB user");
        }
        if(!user.hasDesignation("wd") && user.getSupplierMetaData()!= null && user.getSupplierMetaData().size() > 0) {
        	ruleResult.append("Only wd can be a supplier");
        }

        JsonNode extendedAttributes = user.getExtendedAttributes();
        if(extendedAttributes != null) {
            if(extendedAttributes.hasNonNull("Category")) {
                String category = extendedAttributes.get("Category").asText();
                if (category != null && !category.isBlank() && !(category.equalsIgnoreCase("wd") || category.equalsIgnoreCase("branch") || category.equalsIgnoreCase("district"))) {
                	ruleResult.append("Category field should have either wd branch or district");
                }
                if(category.equalsIgnoreCase("district")) {
                	if(user.getLocationHierarchy().getBranch() != null) {
                		ruleResult.append("category type district cannot have branch");
                	}
                	if (user.getLocationHierarchy().getDistrict() != null) {
                        if (!StringUtils.isNotBlank(user.getLocationHierarchy().getDistrict()) || !regexValidation.match(capitalCaseBranchRegex, user.getLocationHierarchy().getDistrict())) {
                            ruleResult.append(
                                    "Value given for district should contain only alphabets with capital case.Current given value is not compatible");
                        }
                    } else {
                        ruleResult.append("District can not be null");
                    }
                	String wd = extendedAttributes.get("WDCode").asText();
                	if(wd != null && !wd.isBlank()) {
                		ruleResult.append("WDCode value should be null or empty");
                	}
                	if(user.getMobile() == null || user.getMobile().isBlank())
                		ruleResult.append("Mobile number cannot be null or empty");
                	String mobile = extendedAttributes.get("MobileNumber2").asText();
                	if(mobile != null) {
                		if(!mobile.isEmpty() && !checkMobileNumberPattern(mobile))
                			ruleResult.append("Mobile number 2 field allowed only 10 digit valid number or blank.");
                	}
                }
                else if (category.equalsIgnoreCase("branch")) {
                	if(user.getLocationHierarchy().getBranch() == null || user.getLocationHierarchy().getBranch().isEmpty()) {
                		ruleResult.append("branch cannot be empty");
                	}
                	if(user.getLocationHierarchy().getDistrict() == null || user.getLocationHierarchy().getDistrict().isEmpty()) {
                		ruleResult.append("district cannot be empty");
                	}
                	String wd = extendedAttributes.get("WDCode").asText();
                	if(wd != null && !wd.isBlank()) {
                		ruleResult.append("WDCode value should be null or empty");
                	}
                	if(user.getMobile() == null || user.getMobile().isBlank())
                		ruleResult.append("Mobile number cannot be null or empty");
                	String mobile = extendedAttributes.get("MobileNumber2").asText();
                	if(mobile != null) {
                		if(!mobile.isEmpty() && !checkMobileNumberPattern(mobile))
                			ruleResult.append("Mobile number 2 field allowed only 10 digit valid number or blank.");
                	}
                }
            }
        }
        
        boolean isuserDistrict = isUserDistrict(user);
        boolean isuserBranch = isUserBranch(user);
        if(!isuserDistrict) {
            if (user.getLocationHierarchy() == null) {
            	ruleResult.append("location value cannot be null or empty");
            }
            if (user.getLocationHierarchy().getDistrict() != null) {
                if (!StringUtils.isNotBlank(user.getLocationHierarchy().getDistrict()) || !regexValidation.match(capitalCaseBranchRegex, user.getLocationHierarchy().getDistrict())) {
                    ruleResult.append(
                            "Value given for district should contain only alphabets with capital case.Current given value is not compatible");
                }
            } else {
                ruleResult.append("District can not be null");
            }

            if (user.getLocationHierarchy().getBranch() != null) {
                if (!StringUtils.isNotBlank(user.getLocationHierarchy().getBranch()) || !regexValidation.match(capitalCaseBranchRegex, user.getLocationHierarchy().getBranch())) {
                    ruleResult.append(
                            "Value given for branch should contain only alphabets with capital case.Current given value is not compatible");
                }
            } else {
                ruleResult.append("Branch can not be null");
            }

            if (user.getImmediateParent() == null || user.getImmediateParent().size() == 0) {
                ruleResult.append("immediate parent can not be null.");
            } else {
                if(user.getImmediateParent().size() > 1 && (!user.getDesignation().contains("psr") ? !user.getDesignation().contains("stockist"):false)) {
                    ruleResult.append("immediate parent can not be more than one.");
                }
                for (HierarchyMetaData immParent : user.getImmediateParent()) {
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

                    if (!isuserDistrict) {
                        if(dbparent.getLocationHierarchy() != null) {
                        	String district = dbparent.getLocationHierarchy().getDistrict();
                        	String branch = dbparent.getLocationHierarchy().getBranch();

                            if (district != null  && !district.equalsIgnoreCase(user.getLocationHierarchy().getDistrict())) {
                                ruleResult.append("District value of loginId is not matching the parent district");
                            }
                            if(!isuserBranch && branch != null  && !branch.equalsIgnoreCase(user.getLocationHierarchy().getBranch())) {
                                ruleResult.append("Branch value of loginId is not matching the parent branch");
                            }
                            }
                        }
                    List<String> errors = isValidParent(user, dbparent);
                    if (!errors.isEmpty()) {
                        ruleResult.append(String.join(",", errors));
                    }
                }
            }

            }

        if(ruleResult.length()>0) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
        }else {
            return OperationResult.StepResult.OK;
        }
    }

	private boolean checkMobileNumberPattern(String mobile) {
		RegexValidation regexValidation = new RegexValidation();
		return regexValidation.match(regex, mobile);
	}

	private boolean isOnlyDesignation(User user) {
        return user.getDesignation().size() ==1;
    }
    
    private boolean isUserDistrict(User user) {
        return user.getDesignation().contains("district");
    }

    private boolean isUserBranch(User user) {
        return user.getDesignation().contains("branch");
    }

    private boolean isUserDesignation(User user, String designation) {
        return user.getDesignation().contains(designation);
    }

    private List<String> isValidParent(User user, User dbParent) {
        List<String> errors = new ArrayList<>();
        user.getDesignation().stream().forEach((designation) -> {
            switch(designation) {
                case "branch":
                    if (!isUserDistrict(dbParent)) {
                        errors.add(StringUtils.format(("User has invalid immediate parent")));
                    }
                    break;
                case "wd":
                    if(!isUserBranch(dbParent)) {
                        errors.add(StringUtils.format(("User has invalid immediate parent")));
                    }
                    break;
                case "psr":
                    if(!isUserDesignation(dbParent, "wd")) {
                        errors.add(StringUtils.format(("User has invalid immediate parent")));
                    }
                    break;
                case "stockist":
                    if(!isUserDesignation(dbParent, "psr") && !isUserDesignation(dbParent, "wd")) {
                        errors.add(StringUtils.format(("User has invalid immediate parent ")));
                    }
                    break;
            }
        });
        return errors;
    }

}
