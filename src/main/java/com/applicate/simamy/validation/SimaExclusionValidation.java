package com.applicate.simamy.validation;

import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class SimaExclusionValidation extends AbstractRule<GenericEntity> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public RuleResult apply(GenericEntity genericEntity) {
        if(genericEntity.getName().equals("PriceInclusionExclusion")) {
            List<String> errors = new ArrayList<>();
            if (ObjectUtils.isEmpty(genericEntity.getKey1()) || genericEntity.getKey1().equals("null"))
                errors.add("'ConsSubtrChnl' or 'CustomerID' field is missing ");
            if (ObjectUtils.isEmpty(genericEntity.getKey2()) || genericEntity.getKey2().equals("null"))
                errors.add("'ValidTo' field is missing ");
            if (ObjectUtils.isEmpty(genericEntity.getKey3()) || genericEntity.getKey3().equals("null"))
                errors.add("'ValidFrom' field is missing ");
            validateTableAndChangeIndicator(genericEntity, errors);
            if (ObjectUtils.isEmpty(genericEntity.getKey6()) || genericEntity.getKey6().equals("null"))
                errors.add("'MaterialNumber' field is missing ");
            genericEntity.setKey2(null);
            genericEntity.setKey3(null);
            genericEntity.setKey5(null);
            genericEntity.setKey6(null);
            if (!errors.isEmpty()) {
                String errorstr = com.applicate.services.channelkart.utils.StringUtils.format("Some values for Sima Inclusion Exclusion: {} violating validations. Reason : {}", org.apache.commons.lang.StringUtils.join(errors, ","));
                logger.error(errorstr);
                return new RuleResult(Status.ERROR, errorstr);
            }
        }
        return RuleResult.OK;
    }

    private void validateTableAndChangeIndicator(GenericEntity genericEntity, List<String> errors) {
        if(ObjectUtils.isEmpty(genericEntity.getKey5()) || genericEntity.getKey5().equals("null") || !(genericEntity.getKey5().equals("I") || genericEntity.getKey5().equals("D")))
            errors.add("'ChangeIndicator' field is missing or provided other than I or D ");
        if( ObjectUtils.isEmpty(genericEntity.getKey4()) || genericEntity.getKey4().equals("null") || ! (genericEntity.getKey4().equals("924") || genericEntity.getKey4().equals("929")))
            errors.add("'Table' field is missing or provided other than '924' or '929' ");
    }
}
