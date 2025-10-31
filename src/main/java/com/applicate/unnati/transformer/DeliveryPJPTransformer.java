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

public class DeliveryPJPTransformer  extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

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

        // DeliveryPJP specific fields
        result.put("beat", getString(inputMap, "beat"));
        result.put("dayAndFrequency", getJSON(inputMap, "dayAndFrequency"));
        result.put("deliveryDate", getLocalDateTime(inputMap, "deliveryDate"));
        result.put("month", getString(inputMap, "month"));
        result.put("year", getString(inputMap, "year"));

        // Map outletCode from model to outletcode in POJO
        result.put("outletcode", getString(inputMap, "outletCode"));

        // Map loginId from model to loginid in POJO
        result.put("loginid", getString(inputMap, "loginId"));

        // Map supplierId from model to supplierid in POJO
        result.put("supplierid", getString(inputMap, "supplierId"));

        result.put("source", getString(inputMap, "source"));

        // Handle designation with toLowerCase transformation as per model
        result.put("designation", getDesignationLowerCase(inputMap, "designation"));

        result.put("approvedBy", getString(inputMap, "approvedBy"));
        result.put("destinationCode", getString(inputMap, "destinationCode"));
        result.put("destinationName", getString(inputMap, "destinationName"));

        // Map pjpDate - handle both Date and LocalDateTime
        result.put("pjpDate", getString(inputMap, "pjpDate"));

        result.put("pjpPlan", getString(inputMap, "pjpPlan"));
        result.put("sourceCode", getString(inputMap, "sourceCode"));
        result.put("sourceName", getString(inputMap, "sourceName"));
        result.put("status", getString(inputMap, "status"));
        result.put("statusRemarks", getString(inputMap, "statusRemarks"));
        result.put("type", getString(inputMap, "type"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("referenceNumber", getString(inputMap, "referenceNumber"));
        result.put("turnAroundTime", getString(inputMap, "turnAroundTime"));

        // Handle changed field - convert from Boolean to Byte
        result.put("changed", getChangedAsByte(inputMap, "changed"));

        result.put("outletVisited", getBoolean(inputMap, "outletVisited"));
        result.put("sequence", getInteger(inputMap, "sequence"));
        result.put("beatId", getString(inputMap, "beatId"));

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

    private Byte getChangedAsByte(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).byteValue();
            }
            if (value instanceof Boolean) {
                return ((Boolean) value) ? (byte) 1 : (byte) 0;
            }
            String str = value.toString().toLowerCase();
            if ("true".equals(str) || "1".equals(str) || "yes".equals(str)) {
                return (byte) 1;
            }
            if ("false".equals(str) || "0".equals(str) || "no".equals(str)) {
                return (byte) 0;
            }
            return Byte.valueOf(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String getDesignationLowerCase(Map<String, Object> map, String key) {
        String value = getString(map, key);
        return (value != null) ? value.toLowerCase() : null;
    }


    private LocalDateTime getLocalDateTime(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }

            String dateTimeStr = value.toString();
            return getLocalDateTimeValue(dateTimeStr);

        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime getLocalDateTimeValue(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
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

    private JSON getJSON(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof JSON) {
                return (JSON) value;
            }
            // Convert JsonNode or Map to JSON string
            if (value instanceof JsonNode) {
                return JSON.valueOf(objectMapper.writeValueAsString(value));
            }
            return JSON.valueOf(objectMapper.writeValueAsString(value));
        } catch (Exception e) {
            return null;
        }
    }
}

// Test data for DeliveryPJP - matching the existing streaming format
//    public static String rawStreamingData = "{\n" +
//                                                    "  \"requestId\": \"test-req-PJP-001\",\n" +
//                                                    "  \"groupId\": \"2025-09-08\",\n" +
//                                                    "  \"lob\": \"cokesademo\",\n" +
//                                                    "  \"loginId\": \"integration_user\",\n" +
//                                                    "  \"batchNumber\": 1,\n" +
//                                                    "  \"transformerInfo\": [\n" +
//                                                    "    {\n" +
//                                                    "      \"skipPreprocessing\": false,\n" +
//                                                    "      \"skipPersist\": false,\n" +
//                                                    "      \"entityName\": \"DeliveryPJP\",\n" +
//                                                    "      \"transformerId\": \"genericDeliveryPJPTransformer\",\n" +
//                                                    "      \"operationType\": \"insert\"\n" +
//                                                    "    }\n" +
//                                                    "  ],\n" +
//                                                    "  \"features\": [\n" +
//                                                    "    {\n" +
//                                                    "      \"lob\": \"cokesademo\",\n" +
//                                                    "      \"id\": \"5044931-507649-wed-oct-29-00:00:00-utc-2025\",\n" +
//                                                    "      \"activeStatus\": \"ACTIVE\",\n" +
//                                                    "      \"activeStatusReason\": \"Valid PJP entry\",\n" +
//                                                    "      \"createdBy\": \"system_user\",\n" +
//                                                    "      \"creationTime\": \"2025-08-31T10:15:30\",\n" +
//                                                    "      \"extendedAttributes\": { \"depo\": \"50\", \"frequency\": 7, \"routeType\": \"10\", \"firstVisitDate\": \"2024-12-11 00:00:00\" },\n" +
//                                                    "      \"lastModifiedTime\": \"2025-09-07T12:00:00\",\n" +
//                                                    "      \"modifiedBy\": \"admin_user\",\n" +
//                                                    "      \"version\": \"1\",\n" +
//                                                    "      \"outletCode\": \"5044931\",\n" +
//                                                    "      \"loginId\": \"507649\",\n" +
//                                                    "      \"beat\": \"T03\",\n" +
//                                                    "      \"pjpDate\": \"2025-10-31\",\n" +
//                                                    "      \"dayAndFrequency\": [ { \"day\": \"friday\", \"frequency\": 5 } ],\n" +
//                                                    "      \"month\": \"october\",\n" +
//                                                    "      \"year\": \"2025\",\n" +
//                                                    "      \"designation\": \"SALES_REP\",\n" +
//                                                    "      \"sourceCode\": \"SRC001\",\n" +
//                                                    "      \"sourceName\": \"Main Warehouse\",\n" +
//                                                    "      \"destinationCode\": \"DEST001\",\n" +
//                                                    "      \"destinationName\": \"Retail Outlet 001\",\n" +
//                                                    "      \"type\": \"DELIVERY\",\n" +
//                                                    "      \"status\": \"SCHEDULED\",\n" +
//                                                    "      \"statusRemarks\": \"Ready for delivery\",\n" +
//                                                    "      \"approvedBy\": \"manager_001\",\n" +
//                                                    "      \"pjpPlan\": \"WEEKLY_PLAN\",\n" +
//                                                    "      \"referenceNumber\": \"REF_PJP_001\",\n" +
//                                                    "      \"turnAroundTime\": \"2_HOURS\",\n" +
//                                                    "      \"changed\": true,\n" +
//                                                    "      \"outletVisited\": false,\n" +
//                                                    "      \"sequence\": 0,\n" +
//                                                    "      \"beatId\": \"BEAT_T03\",\n" +
//                                                    "      \"supplierId\": \"SUPP_001\",\n" +
//                                                    "      \"source\": \"MDM_SYSTEM\",\n" +
//                                                    "      \"hash\": \"pjp_hash_abc123\"\n" +
//                                                    "    },\n" +
//                                                    "    {\n" +
//                                                    "      \"lob\": \"cokesademo\",\n" +
//                                                    "      \"id\": \"5043637-507649-thu-oct-30-00:00:00-utc-2025\",\n" +
//                                                    "      \"activeStatus\": \"ACTIVE\",\n" +
//                                                    "      \"activeStatusReason\": \"Valid PJP entry\",\n" +
//                                                    "      \"createdBy\": \"system_user\",\n" +
//                                                    "      \"creationTime\": \"2025-08-31T10:15:30\",\n" +
//                                                    "      \"extendedAttributes\": { \"depo\": \"50\", \"frequency\": 7, \"routeType\": \"10\", \"firstVisitDate\": \"2022-12-29 00:00:00\" },\n" +
//                                                    "      \"lastModifiedTime\": \"2025-09-07T12:00:00\",\n" +
//                                                    "      \"modifiedBy\": \"admin_user\",\n" +
//                                                    "      \"version\": \"1\",\n" +
//                                                    "      \"outletCode\": \"5043637\",\n" +
//                                                    "      \"loginId\": \"507649\",\n" +
//                                                    "      \"beat\": \"T03\",\n" +
//                                                    "      \"pjpDate\": \"2025-10-31\",\n" +
//                                                    "      \"dayAndFrequency\": [ { \"day\": \"friday\", \"frequency\": 5 } ],\n" +
//                                                    "      \"month\": \"october\",\n" +
//                                                    "      \"year\": \"2025\",\n" +
//                                                    "      \"designation\": \"DELIVERY_AGENT\",\n" +
//                                                    "      \"sourceCode\": \"SRC002\",\n" +
//                                                    "      \"sourceName\": \"Regional Depot\",\n" +
//                                                    "      \"destinationCode\": \"DEST002\",\n" +
//                                                    "      \"destinationName\": \"Retail Outlet 002\",\n" +
//                                                    "      \"type\": \"DELIVERY\",\n" +
//                                                    "      \"status\": \"SCHEDULED\",\n" +
//                                                    "      \"statusRemarks\": \"Pending approval\",\n" +
//                                                    "      \"approvedBy\": \"manager_002\",\n" +
//                                                    "      \"pjpPlan\": \"WEEKLY_PLAN\",\n" +
//                                                    "      \"referenceNumber\": \"REF_PJP_002\",\n" +
//                                                    "      \"turnAroundTime\": \"3_HOURS\",\n" +
//                                                    "      \"changed\": true,\n" +
//                                                    "      \"outletVisited\": false,\n" +
//                                                    "      \"sequence\": 0,\n" +
//                                                    "      \"beatId\": \"BEAT_T03\",\n" +
//                                                    "      \"supplierId\": \"SUPP_002\",\n" +
//                                                    "      \"source\": \"MDM_SYSTEM\",\n" +
//                                                    "      \"hash\": \"pjp_hash_def456\"\n" +
//                                                    "    }\n" +
//                                                    "  ]\n" +
//                                                    "}";
