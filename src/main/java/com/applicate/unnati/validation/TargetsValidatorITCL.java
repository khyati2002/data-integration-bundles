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

public class TargetsValidatorITCL extends AbstractValidationRule<Targets> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public OperationResult.StepResult apply(Targets cdm) {
        final OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);

        String decimalRegex = "^([0-9-]*\\.)+?[0-9]+$";

        List<String> errors = new ArrayList<>();

        OperationResult.StepResult error = validateOutlet(cdm, errors, outletDetailsService);
        if (error != null) return error;

        if (String.valueOf(cdm.getTarget()) == null || String.valueOf(cdm.getTarget()).isEmpty()) {
            errors.add("Target value should not be null or empty");
        }

        JsonNode extendAttr = cdm.getExtendedAttributes();

        if (cdm.getTargetName() == null || cdm.getTargetName().isEmpty()) {
            errors.add("Target type should not be null or blank");
        }

        validateAch(extendAttr, errors, decimalRegex);
        validateTarget(extendAttr, errors, decimalRegex);
        validateMonth(extendAttr, errors);
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

    private void validateAch(JsonNode extendAttr, List<String> errors, String decimalRegex) {
        if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("achieved")) {
            String valueAttr = extendAttr.get("achieved").asText();
            if (valueAttr == null || valueAttr.isEmpty()) {
                errors.add("achieved should not be null or empty");
            }
            if (!validatePattern(decimalRegex, valueAttr)) {
                errors.add("only decimal value are allowed in achieved");
            }
        } else {
            errors.add("achieved should not be null or empty");
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
    private static void validateMonth(JsonNode extendAttr, List<String> errors) {
        String monthNames = "Jan,Feb,Mar,Apr,May,Jun,Jul,Aug,Sep,Oct,Nov,Dec";
        List<String> monthNamesList = Arrays.asList(monthNames.split(","));

        String month;
        if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("month")) {
            month = extendAttr.get("month").asText();
            month = month.substring(0, 1).toUpperCase() + month.substring(1).toLowerCase();
            if (monthNamesList.indexOf(month) == -1) {
                errors.add("Invalid Month name please specify proper name , valid month names are [" + monthNames
                        + "]");
            }
        } else {
            errors.add("Month should not be null or empty");
        }

    }

    private OperationResult.StepResult validateOutlet(Targets cdm, List<String> errors, OutletDetailsService outletDetailsService) {
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
                        String.join(",", errors));
                logger.error(errorstr);
                return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
            } else {
                ActiveStatus s1 = dbRecord.getActiveStatus();
                if (!s1.equals(ActiveStatus.ACTIVE)) {
                    errors.add("OutletCode is not active");
                }
            }
        }
        return null;
    }

    private void validateTarget(JsonNode extendAttr, List<String> errors, String decimalRegex) {
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

    public boolean validatePattern(String pattern, String value) {
        RegexValidation regexValidation = new RegexValidation();
        return regexValidation.match(pattern, value);

    }

}