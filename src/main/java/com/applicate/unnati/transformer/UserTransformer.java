package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class UserTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>>  {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Multiple date time formatters to handle various input formats
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_DATE_TIME
    };

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // Common fields from CommonDataModel
        result.put("id", getString(inputMap, "id"));
        result.put("activeStatus", getActiveStatus(inputMap, "activeStatus"));
        result.put("activeStatusReason", getString(inputMap, "activeStatusReason"));
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("creationTime", getLocalDateTime(inputMap, "creationTime"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("lastModifiedTime", getLocalDateTime(inputMap, "lastModifiedTime"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("version", getInteger(inputMap, "version"));

        // User specific fields
        result.put("address", getString(inputMap, "address"));
        result.put("contactType", getString(inputMap, "contactType"));
        result.put("countryCode", getString(inputMap, "countryCode"));
        result.put("email", getString(inputMap, "email"));
        result.put("hierarchy", getString(inputMap, "hierarchy"));
        result.put("lastPasswordResetDate", getLocalDateTime(inputMap, "lastPasswordResetDate"));
        result.put("loginid", getString(inputMap, "loginid"));
        result.put("mobile", getString(inputMap, "mobile"));
        result.put("name", getString(inputMap, "name"));
        result.put("password", getString(inputMap, "password"));
        result.put("useraccountid", getString(inputMap, "useraccountid"));
        result.put("usercontext", getString(inputMap, "usercontext"));
        result.put("webcontext", getString(inputMap, "webcontext"));
        result.put("locationHierarchy", getString(inputMap, "locationHierarchy"));
        result.put("source", getString(inputMap, "source"));
        result.put("registeredNumber", getString(inputMap, "registeredNumber"));
        result.put("facebookpsid", getString(inputMap, "facebookpsid"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("dialCode", getString(inputMap, "dialCode"));
        result.put("ssoId", getString(inputMap, "ssoId"));
        result.put("deviceId", getString(inputMap, "deviceId"));
        result.put("verified", getBoolean(inputMap, "verified"));
        result.put("doa", getLocalDateTime(inputMap, "doa"));
        result.put("dob", getLocalDateTime(inputMap, "dob"));
        result.put("assignedHierarchy", getString(inputMap, "assignedHierarchy"));
        result.put("rowid", getInteger(inputMap, "rowid"));
        result.put("changed", getByte(inputMap, "changed"));
        result.put("blocked", getBoolean(inputMap, "blocked"));
        result.put("normalizedHierarchy", getString(inputMap, "normalizedHierarchy"));
        result.put("alternateId", getString(inputMap, "alternateId"));
        result.put("externalReferenceId", getString(inputMap, "externalReferenceId"));
        result.put("reportPassword", getString(inputMap, "reportPassword"));
        result.put("prodauthcode", getString(inputMap, "prodauthcode"));

        return result;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            String str = value.toString().toLowerCase();
            return "true".equals(str) || "1".equals(str) || "yes".equals(str);
        } catch (Exception e) {
            return null;
        }
    }

    private Byte getByte(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).byteValue();
            }
            return Byte.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime getLocalDateTime(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }

            String dateTimeStr = value.toString();
            if (dateTimeStr.trim().isEmpty()) {
                return null;
            }

            // Try multiple formatters for better compatibility
            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    return LocalDateTime.parse(dateTimeStr, formatter);
                } catch (DateTimeParseException e) {
                    // Continue to next formatter
                }
            }

            // If all formatters fail, try to handle epoch timestamp
            try {
                long epochMilli = Long.parseLong(dateTimeStr);
                return LocalDateTime.ofEpochSecond(epochMilli / 1000, 0, java.time.ZoneOffset.UTC);
            } catch (NumberFormatException e) {
                // Final fallback - return null
                return null;
            }

        } catch (Exception e) {
            return null;
        }
    }

    private ActiveStatus getActiveStatus(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof ActiveStatus) {
                return (ActiveStatus) value;
            }
            String statusStr = value.toString().toUpperCase().trim();
            return ActiveStatus.valueOf(statusStr);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode getJsonNode(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof JsonNode) {
                return (JsonNode) value;
            }
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
}