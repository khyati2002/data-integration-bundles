package com.applicate.validation;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.Targets;
import com.salescode.dim.jooq.impl.User;

public class VistaarTargetsValidator extends AbstractValidationRule<Targets> {

    @Override
    public OperationResult.StepResult apply(Targets cdm) {

        final UserService userService = (UserService) ServiceLocator.lookup(User.class);

        StringBuilder errorStr = new StringBuilder();

        if(StringUtils.isEmpty(cdm.getUserValueStr())){
            errorStr.append("UID Null or empty. ");
        }

        if(StringUtils.isEmpty(cdm.getTargetName())){
            errorStr.append("Parameter Null or empty. ");
        }

        if (cdm.getExtendedAttributes() == null ||
                !cdm.getExtendedAttributes().hasNonNull("month") ||
                !cdm.getExtendedAttributes().hasNonNull("year") ||
                StringUtils.isEmpty(cdm.getExtendedAttributes().path("month").asText()) ||
                StringUtils.isEmpty(cdm.getExtendedAttributes().path("year").asText())) {
            errorStr.append("Month or Year Passed is Null or empty. ");
        }


        if (cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().hasNonNull("loginId")) {
            User user = userService.findByLoginId(cdm.getExtendedAttributes().get("loginId").asText());
            if(user == null){
                errorStr.append("User Passed not present in DB. ");
            }
        } else {
            errorStr.append("loginId missing from extendedAttributes. ");
        }


        if(errorStr.length() == 0){
            return OperationResult.StepResult.OK;
        }else{
            return new OperationResult.StepResult(OperationResult.Status.ERROR,"Validation Failed! " + errorStr.toString());
        }
    }
}