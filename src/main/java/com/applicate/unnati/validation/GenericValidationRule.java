package com.applicate.unnati.validation;


import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.User;

import java.util.regex.Pattern;

public class GenericValidationRule extends AbstractValidationRule<CommonDataModel> {

    @Override
    public OperationResult.StepResult apply(CommonDataModel cdm) {
        return OperationResult.StepResult.OK;
    }

}
