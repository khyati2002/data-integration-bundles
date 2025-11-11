package com.applicate.validation;

import com.applicate.services.channelkart.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletDetails;

public class OutletDetailsValidator extends AbstractValidationRule<OutletDetails> {
    private ValidatorFactory factory = Validation.buildDefaultValidatorFactory();

    private Validator validator = factory.getValidator();

    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {

            List<String> errors= new ArrayList<>();
            Set<javax.validation.ConstraintViolation<OutletDetails>> constraintViolations=validator.validate(cdm);
            for (ConstraintViolation<OutletDetails> violation : constraintViolations) {
                errors.add(StringUtils.format("'{}' : {}",violation.getPropertyPath().toString(),violation.getMessage()));
            }
            if(!errors.isEmpty()){
                String errorstr= StringUtils.format("Error saving outlet : {}, Reason : [{}]", cdm.getOutletcode(),org.apache.commons.lang3.StringUtils.join(errors, ','));
                return new OperationResult.StepResult(OperationResult.Status.ERROR,errorstr);
            }
            return  OperationResult.StepResult.OK;
    }
}

