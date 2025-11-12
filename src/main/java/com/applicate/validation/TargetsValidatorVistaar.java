package com.applicate.validation;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.Targets;
import com.salescode.dim.jooq.impl.User;

public class TargetsValidatorVistaar extends AbstractValidationRule<Targets> {

    @Override
    public OperationResult.StepResult apply(Targets cdm) {

        final UserService userService = (UserService) ServiceLocator.lookup(User.class);

        StringBuilder ruleResult = new StringBuilder();

        if (cdm.getUserValueStr() == null) {
            ruleResult.append("userValueStr is null. ");
        } else {
            User user = userService.findByLoginId(cdm.getUserValueStr());
            if(user == null){
                ruleResult.append(" LoginId " + cdm.getUserValueStr() + " does not exist. ");
            }
        }

        if(!ruleResult.toString().isEmpty()){
            return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
        }
        return OperationResult.StepResult.OK;
    }
}