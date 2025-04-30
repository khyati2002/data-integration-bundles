package com.applicate.unnati.validation;


import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;

import com.applicate.services.channelkart.validations.repository.RegexValidation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.Targets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TargetValidationITCL extends AbstractValidationRule<Targets> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    final OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);

    @Override
    public OperationResult.StepResult apply(Targets cdm) {
        String monthNames = "1,2,3,4,5,6,7,8,9,10,11,12";
        String decimalRegex = "^([0-9-]*\\.)+?[0-9]+$";
        String loyaltyType = "FC COMMON,FC FOODS";

        List<String> monthNamesList = Arrays.asList(monthNames.split(","));
        List<String> loyaltyTypeList = Arrays.asList(loyaltyType.split(","));
        List<String> errors = new ArrayList<>();

        OperationResult.StepResult error = validteTarget(cdm, errors, loyaltyTypeList);
        if (error != null){
            return error;
        }

        JsonNode extendAttr = cdm.getExtendedAttributes();

        validateTarget(cdm,extendAttr, errors, decimalRegex);

        validateMonth(extendAttr, monthNamesList, errors, monthNames);

        validateYear(extendAttr, errors);

        if (!errors.isEmpty()) {
            String errorstr = StringUtils.format(
                    "Validation error occured for target. Kindly go through provided errors and make sure those conditions should fulfill while retrying. {}",
                    String.join(",", errors));
            logger.error(errorstr);
            return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
        }
        return OperationResult.StepResult.OK;
    }

    private OperationResult.StepResult validteTarget(Targets cdm, List<String> errors, List<String> loyaltyTypeList) {
        var outlet = cdm.getOutletValue();
        OutletDetails dbRecord = null;
        if (cdm.getOutletValue() == null || cdm.getOutletValue().isNull()) {
            errors.add("OutletCode should not be null or empty");
        } else {
            dbRecord = outletDetailsService.findByOutletCode(outlet.get(0).asText());
            if (dbRecord == null) {
                errors.add("OutleCode is not present in OutletDetails");
                String errorstr = StringUtils.format(
                        "Validation error occured for target. Kindly go through provided errors and make sure those conditions should fulfill while retrying. {}",
                        String.join( ",", errors));
                logger.error(errorstr);
                return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
            } else {
                ActiveStatus s1 = dbRecord.getActiveStatus();
                if (!s1.equals(ActiveStatus.ACTIVE)) {
                    errors.add("OutletCode is not active");
                }
                if (dbRecord.getOutletCategory().equals("non loyalty")) {
                    errors.add("OutletCode is of non loyalty type. Target for only loyalty type outlet is allowed");
                }
                if(!loyaltyTypeList.contains(dbRecord.getOutletClass())){
                    errors.add("Incentive Target for only FC FOODS or FC COMMON loyalty type outlet is allowed");
                }
            }
        }
        return null;
    }

    private void validateTarget(Targets cdm, JsonNode extendAttr, List<String> errors, String decimalRegex) {
        if (String.valueOf(cdm.getTarget()) == null || String.valueOf(cdm.getTarget()).isEmpty()) {
            errors.add("Target value should not be null or empty");
        }
        if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("target")) {
            String valueAttr = extendAttr.get("target").asText();
            if (valueAttr == null || valueAttr.isEmpty()) {
                errors.add("target should not be null or empty");
            }
            if (!validatePattern(decimalRegex, valueAttr)) {
                errors.add("only decimal value are allowed in target");
            }
        } else {
            errors.add("target should not be null or empty");
        }
    }

    private static void validateMonth(JsonNode extendAttr, List<String> monthNamesList, List<String> errors, String monthNames) {
        String month;
        if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("month")) {
            month = extendAttr.get("month").asText();
            if (!StringUtils.isNullOrBlank(month)) {
                if(!monthNamesList.contains(month)){
                    errors.add("Invalid Month name please specify proper name , valid month names are [" + monthNames
                            + "]");
                }
            } else {
                errors.add("Month should not be null or empty");
            }
        } else {
            errors.add("Month should not be null or empty");
        }
    }

    private static void validateYear(JsonNode extendAttr, List<String> errors) {
        String year;
        if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("year")) {
            year = extendAttr.get("year").asText();
            if (year != null && !year.trim().isEmpty()) {
                int nonSpaceCount = year.replace(" ", "").length();
                if (nonSpaceCount != 4) {
                    errors.add("Year must consist of exactly 4 non-space characters.");
                }
            } else {
                errors.add("Year should not be null or empty");
            }
        } else {
            errors.add("Year should not be null or empty");
        }
    }

    public boolean validatePattern(String pattern, String value) {
        RegexValidation regexValidation = new RegexValidation();
        return regexValidation.match(pattern, value);
    }

}