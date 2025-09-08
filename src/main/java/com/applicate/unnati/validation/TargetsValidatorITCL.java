package com.applicate.unnati.validation;


import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;

public class TargetsValidatorITCL extends AbstractValidationRule<CommonDataModel> {
    @Override
    public OperationResult.StepResult apply(CommonDataModel cdm) {
        return OperationResult.StepResult.OK;
    }
}

