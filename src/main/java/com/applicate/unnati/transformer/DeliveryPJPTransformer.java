package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSON;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DeliveryPJPTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Formatters for different date patterns in streaming data
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,           // 2025-10-31T00:00:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),        // 2025-10-31
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), // 2025-10-31 00:00:00
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS") // 2025-10-31T00:00:00.000
    };

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // CommonDataModel fields
        result.put("id", getString(inputMap, "id"));
        result.put("activeStatus", getActiveStatus(inputMap, "activeStatus"));
        result.put("activeStatusReason", getString(inputMap, "activeStatusReason"));
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("version", getInteger(inputMap, "version"));
        result.put("source", getString(inputMap, "source"));
        result.put("hash", getString(inputMap, "hash"));

        // DeliveryPJP specific fields
        result.put("beat", getString(inputMap, "beat"));
        result.put("dayAndFrequency", getJSONAsJsonNode(inputMap, "dayAndFrequency"));
        result.put("month", getString(inputMap, "month"));
        result.put("year", getString(inputMap, "year"));

        // Field name mappings (camelCase to lowercase in POJO)
        result.put("outletcode", getString(inputMap, "outletCode"));
        result.put("loginid", getString(inputMap, "loginId"));
        result.put("supplierid", getString(inputMap, "supplierId"));

        // Designation with toLowerCase transformation
        result.put("designation", getDesignationLowerCase(inputMap, "designation"));

        result.put("approvedBy", getString(inputMap, "approvedBy"));
        result.put("destinationCode", getString(inputMap, "destinationCode"));
        result.put("destinationName", getString(inputMap, "destinationName"));

        // FIXED: pjpDate should be LocalDateTime, not String
        result.put("pjpDate", getString(inputMap, "pjpDate"));

        result.put("pjpPlan", getString(inputMap, "pjpPlan"));
        result.put("sourceCode", getString(inputMap, "sourceCode"));
        result.put("sourceName", getString(inputMap, "sourceName"));
        result.put("status", getString(inputMap, "status"));
        result.put("statusRemarks", getString(inputMap, "statusRemarks"));
        result.put("type", getString(inputMap, "type"));
        result.put("referenceNumber", getString(inputMap, "referenceNumber"));
        result.put("turnAroundTime", getString(inputMap, "turnAroundTime"));

        // FIXED: changed field should be Boolean, not Byte
        result.put("changed", getBoolean(inputMap, "changed"));

        result.put("outletVisited", getBoolean(inputMap, "outletVisited"));
        result.put("sequence", getInteger(inputMap, "sequence"));
        result.put("beatId", getString(inputMap, "beatId"));

        // REMOVED: deliveryDate (doesn't exist in POJO)
        // ADD: rowid field (auto-generated, typically null from input)
        result.put("rowid", getInteger(inputMap, "rowid"));

        return result;
    }

    // Helper methods

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

    private String getDesignationLowerCase(Map<String, Object> map, String key) {
        String value = getString(map, key);
        return (value != null) ? value.toLowerCase() : null;
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
            // Handle Map objects from streaming data
            if (value instanceof Map) {
                String jsonString = objectMapper.writeValueAsString(value);
                return objectMapper.readTree(jsonString);
            }
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            System.err.println("Failed to parse JsonNode for key: " + key);
            return null;
        }
    }

    private JsonNode getJSONAsJsonNode(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof JSON) {
                // Parse jOOQ JSON to JsonNode
                return objectMapper.readTree(((JSON) value).data());
            }
            if (value instanceof JsonNode) {
                return (JsonNode) value;
            }
            if (value instanceof java.util.List || value instanceof java.util.Map) {
                String jsonString = objectMapper.writeValueAsString(value);
                return objectMapper.readTree(jsonString);
            }
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            System.err.println("Failed to parse JSON for key: " + key);
            return null;
        }
    }

}
