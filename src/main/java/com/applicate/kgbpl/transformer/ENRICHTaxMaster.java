package com.applicate.kgbpl.transformer;

import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ENRICHTaxMaster extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final List<String> requiredFields = Arrays.asList(
            "skucode", "component", "hsn", "rate", "effectivedate"
    );

    private static final DateTimeFormatter ISO_WITH_Z_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public Map<String, Object> transform(Map<String, Object> source) {
        validateRequiredFields(source);

        Map<String, Object> result = new HashMap<>();
        result.put("skuCode", getString(source, "skucode"));
        result.put("batchCode", getString(source, "skucode"));
        result.put("taxType", getString(source, "component"));
        result.put("taxGroup", getString(source, "hsn") + "-" + "eafp");
        result.put("taxRate", getNumericValue(source.get("rate"), "rate"));
        result.put("priority", 1);
        result.put("startDate", convertToClientTimezoneString(source.get("effectivedate")));

        return result;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null || ObjectUtils.isEmpty(val.toString().trim())) {
            throw new DataTransformationService.TransformationException("Missing or empty required field: " + key);
        }
        return val.toString().trim();
    }

    private Float getNumericValue(Object value, String fieldName) {
        try {
            return Float.parseFloat(getString(Collections.singletonMap(fieldName, value), fieldName));
        } catch (NumberFormatException e) {
            throw new DataTransformationService.TransformationException("Invalid numeric value for " + fieldName + ": " + value);
        }
    }

    /**
     * Updated method — exactly like DateToClientTimeZoneStringConverter logic
     * OR directly calling it.
     */
    private String convertToClientTimezoneString(Object dateValue) {
        try {
            String dateStr = getString(Collections.singletonMap("effectivedate", dateValue), "effectivedate");
            LocalDateTime localDateTime = LocalDateTime.parse(dateStr, ISO_WITH_Z_FORMATTER);

            Date inputDate = Date.from(localDateTime
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
            );

            // DIRECT CALL to converter
            return new DateToClientTimeZoneStringConverter().convert(inputDate);

        } catch (Exception e) {
            throw new DataTransformationService.TransformationException(
                    "Invalid format for effectiveDate: " + dateValue
            );
        }
    }

    private void validateRequiredFields(Map<String, Object> source) {
        for (String key : requiredFields) {
            Object value = source.get(key);
            if (value == null || ObjectUtils.isEmpty(value.toString().trim())) {
                throw new DataTransformationService.TransformationException("Missing or blank required field: " + key);
            }
        }
    }
}