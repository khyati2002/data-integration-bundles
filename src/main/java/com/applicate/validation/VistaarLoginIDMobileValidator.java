package com.applicate.validation;

//import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.User;
import java.util.List;

import com.salescode.dim.etl.validation.AbstractValidationRule;
import org.apache.commons.lang3.StringUtils;

public class VistaarLoginIDMobileValidator extends AbstractValidationRule<User> {

    String regex = "(^[0-9]{10}$)";
    String stringRegex = "^[a-zA-Z]*$";
    String emailRegexValidator="^[a-zA-Z0-9_!#$%&'+/=?`{|}~^-]+(?:\\.[a-zA-Z0-9_!#$%&'+/=?`{|}~^-]+)*@"
            + "[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*$";
    String capitalCaseRegex = "(^[A-Z\\s]*$)";
    private static final RegexValidation regexValidation = new RegexValidation();



    @SuppressWarnings("all")
    @Override
    public OperationResult.StepResult apply(User cdm) {
        StringBuilder ruleResult = new StringBuilder();
        if (cdm.getDesignation()!= null && cdm.getDesignation().contains("psr") || cdm.getDesignation().contains("stockist")) {
            if (cdm.getMobile() != null) {
                if(cdm.getDesignation().contains("psr") || (cdm.getDesignation().contains("stockist") && !StringUtils.isBlank(cdm.getMobile()))){
                    if (!checkMobileNumberPattern(cdm.getMobile())) {
                        ruleResult.append("Given loginid of the user did not match the required validations.Mobile number should exactly contain 10 numeric values.");
                    }
                }
            }
            String ausValue = null;
            String district = null;
            String branch = null;
            if(cdm.getDesignation().contains("psr") && cdm.getMobile()==null){
                ruleResult.append("PSR mobile number cannnot be null");
            }
            if(cdm.getExtendedAttributes() != null) {
                ausValue = cdm.getExtendedAttributes().has("AUS") ? cdm.getExtendedAttributes().get("AUS").asText() : null;
                district = cdm.getExtendedAttributes().hasNonNull("District")?cdm.getExtendedAttributes().get("District").asText():null;
                branch = cdm.getExtendedAttributes().hasNonNull("Branch")?cdm.getExtendedAttributes().get("Branch").asText():null;
            }
            if (StringUtils.isBlank(ausValue) || !(ausValue.trim().equals("Y") || ausValue.trim().equals("N"))) {
                ruleResult.append("AUS field is Y or N for psr and stockist");
            }
            if(StringUtils.isBlank(district) || district == null) {
            	ruleResult.append("District value cannot be null or empty");
            }
            if(StringUtils.isBlank(branch) || branch == null) {
            	ruleResult.append("Branch value cannot be null or empty ");
            }
        }
        
        if (cdm.getDesignation()!= null && cdm.getDesignation().contains("stockist")) {
             List<String> KYCoption= List.of("Y","N","y","n");
        	if(cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().hasNonNull("KYC") && !cdm.getExtendedAttributes().get("KYC").isEmpty() ) {
            	if(!KYCoption.contains(cdm.getExtendedAttributes().get("KYC").asText())) {
            		ruleResult.append("KYC can only be Y or N");
            	}
            }
            
            if(cdm.getExtendedAttributes() == null && !cdm.getExtendedAttributes().hasNonNull("COLOUR")) {
            	ruleResult.append("Colour cannot be null or empty");
            }else if(!checkStringRegex(cdm.getExtendedAttributes().get("COLOUR").asText())) {
            	ruleResult.append("Colour can only have alphabets");
            }
        }

        if(cdm.getDesignation().contains("branch") || cdm.getDesignation().contains("wd") || cdm.getDesignation().contains("district")) {
            /*
            if(cdm.getLoginId() != null && !regexValidation.match(wdbranchLoginRegex, cdm.getLoginId())) {
                ruleResult.append("Given loginid is not matching validation pattern, loginid should only have uppercase letters followed by digits(optional)");
            }*/
            if(cdm.getMobile() != null && !cdm.getMobile().isBlank()) {
                if (!checkMobileNumberPattern(cdm.getMobile())) {
                    ruleResult.append("Given mobile of the user did not match the required validations.Mobile number should exactly contain 10 numeric values.");
                }
            }
            else if(!cdm.getDesignation().contains("wd")) {
               ruleResult.append("mobile number is missing");
            }

            if(cdm.getEmail() != null && !cdm.getEmail().isBlank() && !regexValidation.match(emailRegexValidator, cdm.getEmail())) {
                ruleResult.append("email provided is invalid");
            }

            if(cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().hasNonNull("MobileNumber2")) {
                String mobileNumber2 = cdm.getExtendedAttributes().get("MobileNumber2").asText().toString();
                if(StringUtils.isNotBlank(mobileNumber2) && !checkMobileNumberPattern(mobileNumber2)) {
                    ruleResult.append("Given MobileNumber2 of the user did not match the required validations.Mobile number should exactly contain 10 numeric values. " +mobileNumber2);
                }
            }

            if(cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().hasNonNull("prvLoc")) {
                String prvLoc = cdm.getExtendedAttributes().get("prvLoc").asText().toString();
                if(StringUtils.isNotBlank(prvLoc) && !(prvLoc.equalsIgnoreCase("Y") || prvLoc.equalsIgnoreCase("N"))) {
                    ruleResult.append("prvLoc could be either Y or N, current value >>{} " + prvLoc);
                }
            }

            if(cdm.hasDesignation("wd")) {
                String wdCode=null;
                if(cdm.getExtendedAttributes() != null) {
                    wdCode = cdm.getExtendedAttributes().hasNonNull("WDCode")?cdm.getExtendedAttributes().get("WDCode").asText():null;
                }
                if(wdCode == null || wdCode.isBlank()) {
                    ruleResult.append("WDCode is compulsory for WD data");
                }
            }
            
            if(cdm.hasDesignation("district")) {
            	String district = null;
            	if(cdm.getExtendedAttributes() != null) {
            		district = cdm.getExtendedAttributes().hasNonNull("District")?cdm.getExtendedAttributes().get("District").asText():null;
            	}if(district == null || district.isBlank()) {
            		ruleResult.append("District is complusory data");
            	}
            	if(district != null || !district.isBlank()) {
                    if(!regexValidation.match(capitalCaseRegex, district)){
                    	ruleResult.append(
                                "Value given for District should contain only alphabets with capital case.Current given value is not compatible");
                    }
                }
            }

            if(cdm.hasDesignation("branch")) {
                String branch=null;
                if(cdm.getExtendedAttributes() != null) {
                    branch = cdm.getExtendedAttributes().hasNonNull("Branch")?cdm.getExtendedAttributes().get("Branch").asText():null;
                }
                if(branch == null || branch.isBlank()) {
                    ruleResult.append("Branch is compulsory for branch data");
                }
                if(branch != null || !branch.isBlank()) {
                if(!regexValidation.match(capitalCaseRegex, branch)){
                	ruleResult.append(
                            "Value given for Branch should contain only alphabets with capital case.Current given value is not compatible");
                }
            }
            }
        }


        if(ruleResult.length() > 0) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
        }

        return OperationResult.StepResult.OK;
    }

    public boolean checkMobileNumberPattern(String mobile) {
        return regexValidation.match(regex, mobile);
    }
    
    public boolean checkStringRegex(String colour) {
		return regexValidation.match(stringRegex, colour);
	}
}