package com.applicate.hccb.validation;

import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletDetails;

import java.util.regex.Pattern;

public class OutletDetailsValidatorITCL extends AbstractValidationRule<OutletDetails> {

    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {

        PropertyRegistry propertyRegistry = PropertyRegistry.getInstance();
        String regexY_N = "^(Y|N)$";
        String regexLoyaltyFlag = "^(loyalty|non loyalty)$";
        StringBuilder ruleResult = new StringBuilder();

        if (propertyRegistry.getAsBoolean(PropertyDefinition.USE_SUPPLIER_FROM_OUTLET_METADATA)) {
            return OperationResult.StepResult.OK;
        }

        if (cdm.getExtendedAttributes() != null) {
            if (cdm.getExtendedAttributes().has("custOrder")) {
                if (!Pattern.matches(regexY_N, cdm.getExtendedAttributes().get("custOrder").asText())) {
                    ruleResult.append("Order can have 'Y' or 'N' only");
                }
            } else {
                ruleResult.append("Order cannot not be null");
            }

            if (cdm.getExtendedAttributes().has("custLoyalty")) {
                if (!Pattern.matches(regexY_N, cdm.getExtendedAttributes().get("custLoyalty").asText())) {
                    ruleResult.append("Loyalty can have 'Y' or 'N' only");
                }
            } else {
                ruleResult.append("Loyalty cannot be null");
            }
        }


        if (null != cdm.getOutletCategory()) {
            if (Pattern.matches(regexLoyaltyFlag, cdm.getOutletCategory())) {
                if (cdm.getOutletCategory().equalsIgnoreCase("non loyalty")) {
                    ruleResult.append("Loyalty value cannot be Y for non loyalty outlets");
                }
            } else {
                ruleResult.append("Unexpected value found in loyaltyFlag. LoyaltyFlag can have either  be loyalty or non loyalty");
            }
        } else {
            ruleResult.append("LoyaltyFlag cannot be null or empty");
        }

        if (ruleResult.length() > 0) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
        } else {
            return OperationResult.StepResult.OK;
        }
    }
}