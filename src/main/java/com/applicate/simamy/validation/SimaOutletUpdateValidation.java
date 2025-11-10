package com.applicate.simamy.validation;

import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;

public class SimaOutletUpdateValidation extends AbstractRule<OutletDetails> {

    @Override
    public RuleResult apply(OutletDetails outletDetails) {
        Double latitude = outletDetails.getLatitude();
        Double longitude = outletDetails.getLongitude();
        if (latitude != null || longitude != null) {
            if (hasMoreThanNDecimalPlaces(latitude, 8)) {
                return new RuleResult(Status.ERROR, "Latitude contains more than 8 decimal places.");
            }
            if (hasMoreThanNDecimalPlaces(longitude, 8)) {
                return new RuleResult(Status.ERROR, "Longitude contains more than 8 decimal places.");
            }
        }

            return new RuleResult(Status.OK, "Sima outlet update validation successful.");
    }


    private static boolean hasMoreThanNDecimalPlaces(Double value, int n) {
        if (value == null) {
            return false;
        }

        String valueString = String.valueOf(value);

        int decimalIndex = valueString.indexOf('.');

        return decimalIndex != -1 && valueString.length() - decimalIndex - 1 > n;
    }

}
