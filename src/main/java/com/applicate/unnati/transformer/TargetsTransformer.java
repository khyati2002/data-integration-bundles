package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TargetsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
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
        result.put("hash", getString(inputMap, "hash"));
        result.put("lastModifiedTime", getLocalDateTime(inputMap, "lastModifiedTime"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("source", getString(inputMap, "source"));
        result.put("version", getInteger(inputMap, "version"));

        // Targets specific fields
        result.put("endDate", getLocalDateTime(inputMap, "endDate"));
        result.put("outletType", getString(inputMap, "outletType"));
        result.put("outletValue", getJsonNode(inputMap, "outletValue"));
        result.put("productType", getString(inputMap, "productType"));
        result.put("productValue", getJsonNode(inputMap, "productValue"));
        result.put("startDate", getLocalDateTime(inputMap, "startDate"));
        result.put("target", getDouble(inputMap, "target"));
        result.put("targetId", getString(inputMap, "targetId"));
        result.put("targetName", getString(inputMap, "targetName"));
        result.put("targetTable", getString(inputMap, "targetTable"));
        result.put("targetType", getString(inputMap, "targetType"));
        result.put("unit", getString(inputMap, "unit"));
        result.put("userType", getString(inputMap, "userType"));
        result.put("userValue", getJsonNode(inputMap, "userValue"));
        result.put("changed", getByte(inputMap, "changed"));
        result.put("targetcondition", getDouble(inputMap, "targetcondition"));
        result.put("targetconditionunit", getString(inputMap, "targetconditionunit"));
        result.put("userValueStr", getString(inputMap, "userValueStr"));
        result.put("outletValueStr", getString(inputMap, "outletValueStr"));

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

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
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
            String raw = value.toString().trim();
            if (raw.contains("T") && raw.contains("Z")) {
                raw = raw.substring(0, raw.indexOf("T")) + " " + raw.substring(raw.indexOf("T") + 1, raw.indexOf("Z"));
            }

            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    return LocalDateTime.parse(raw, formatter);
                } catch (Exception ignored) {}
            }
            return null;
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
            return ActiveStatus.valueOf(value.toString().toUpperCase());
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

    // Test data for Targets

}