package com.applicate.unnati.validation;


import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.User;

import java.util.regex.Pattern;

public class UserValidatorITCL extends AbstractValidationRule<User> {



    @Override
    public OperationResult.StepResult apply(User cdm) {
        UserService userservice = (UserService) ServiceLocator.lookup(User.class);
        StringBuilder ruleResult = new StringBuilder();
        String regexNum = "^[0-9]*";
        String regexY_N = "^(Y|N)$";
        if (cdm.getDesignation().contains("supplier")) {
            if (!cdm.getSupplierMetaData().isEmpty()) {
                if (cdm.getSupplierMetaData().get(0).getExtendedAttributes().has("orderFunction")
                        && cdm.getSupplierMetaData().get(0).getExtendedAttributes().has("orderFulfillmentTime")) {
                    if (!(Pattern.matches(regexY_N,
                            cdm.getSupplierMetaData().get(0).getExtendedAttributes().get("orderFunction").asText()))) {
                        ruleResult.append("Field OrderFunc must have 'Y' or 'N' only");
                        return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());

                    }
                    if (!(Pattern.matches(regexNum, cdm.getSupplierMetaData().get(0).getExtendedAttributes()
                            .get("orderFulfillmentTime").asText()))) {
                        ruleResult.append(
                                "Field OrderFulfillmentTime is not matching our requirements. It contains some special characters except this [ 0-9 ]");
                        return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());

                    }
                    if (!(Pattern.matches(regexNum, String.valueOf(cdm.getSupplierMetaData().get(0).getMin())))) {
                        ruleResult.append(
                                "Field MinOrderValue is not matching our requirements. It contains some special characters except this [ 0-9 ]");
                        return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
                    }
                } else {
                    return new OperationResult.StepResult(OperationResult.Status.ERROR, "Header mismatch check whether all the headers are present");
                }
            } else {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "Header mismatch check whether all the headers are present");
            }
        }
        return OperationResult.StepResult.OK;
    }

}
