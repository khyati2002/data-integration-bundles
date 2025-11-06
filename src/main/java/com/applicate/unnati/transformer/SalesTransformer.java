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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class SalesTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>>  {

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
        result.put("changed", getBoolean(inputMap, "changed"));
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("creationTime", getLocalDateTime(inputMap, "creationTime"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("lastModifiedTime", getLocalDateTime(inputMap, "lastModifiedTime"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("source", getString(inputMap, "source"));
        result.put("version", getInteger(inputMap, "version"));
        result.put("systemTime", getLocalDateTime(inputMap, "systemTime"));
        result.put("gpsLatitude", getString(inputMap, "gpsLatitude"));
        result.put("gpsLongitude", getString(inputMap, "gpsLongitude"));
        result.put("billAmount", getDouble(inputMap, "billAmount"));
        result.put("userHierarchy", getString(inputMap, "userHierarchy"));
        result.put("initialAmount", getDouble(inputMap, "initialAmount"));
        result.put("locationHierarchy", getString(inputMap, "locationHierarchy"));
        result.put("mrp", getDouble(inputMap, "mrp"));
        result.put("name", getString(inputMap, "name"));
        result.put("netAmount", getDouble(inputMap, "netAmount"));
        result.put("normalizedVolume", getDouble(inputMap, "normalizedVolume"));
        result.put("orderNumber", getString(inputMap, "orderNumber"));
        result.put("orderedDate", getLocalDateTime(inputMap, "orderedDate"));
        result.put("payByDate", getLocalDateTime(inputMap, "payByDate"));
        result.put("programNumber", getString(inputMap, "programNumber"));
        result.put("remarks", getString(inputMap, "remarks"));
        result.put("size", getString(inputMap, "size"));
        result.put("status", getString(inputMap, "status"));
        result.put("supplierid", getString(inputMap, "supplierId"));
        result.put("hierarchy", getString(inputMap, "hierarchy"));
        result.put("type", getString(inputMap, "type"));

        // Sales specific fields
        // Map loginId from model to loginid in POJO
        result.put("loginid", getString(inputMap, "loginId"));

        // Map outletCode from model to outletcode in POJO
        result.put("outletcode", getString(inputMap, "outletCode"));

        // Handle orderType - convert enum to String if needed
        result.put("orderType", getOrderTypeAsString(inputMap, "orderType"));

        result.put("invoiceNumber", getString(inputMap, "invoiceNumber"));
        result.put("referenceNumber", getString(inputMap, "referenceNumber"));
        result.put("statusReason", getString(inputMap, "statusReason"));
        result.put("saleCondition", getString(inputMap, "saleCondition"));

        // Handle float to Double conversion for quantity fields
        result.put("totalQuantity", getDouble(inputMap, "totalQuantity"));
        result.put("totalInitialQuantity", getDouble(inputMap, "totalInitialQuantity"));
        result.put("normalizedQuantity", getDouble(inputMap, "normalizedQuantity"));
        result.put("initialNormalizedQuantity", getDouble(inputMap, "initialNormalizedQuantity"));

        // Handle Date to LocalDateTime conversion for deliveryDate
        result.put("deliveryDate", getLocalDateTimeFromDate(inputMap, "deliveryDate"));

        // Handle discountInfo - JsonNode to JSON
        result.put("discountInfo", getJooqJsonAsString(inputMap, "discountInfo"));

        // Beat fields
        result.put("beat", getString(inputMap, "beat"));
        result.put("beatName", getString(inputMap, "beatName"));

        // Boolean fields
        result.put("inBeat", getBoolean(inputMap, "inBeat"));
        result.put("inRange", getBoolean(inputMap, "inRange"));

        // Additional fields in POJO
        result.put("rowid", getInteger(inputMap, "rowid"));
        result.put("amount", getDouble(inputMap, "amount"));
        result.put("routeId", getString(inputMap, "routeId"));
        result.put("nw", getDouble(inputMap, "nw"));
        result.put("invSerNo", getString(inputMap, "invSerNo"));

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

    private String getOrderTypeAsString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof String) {
                return value.toString();
            }
            if (value instanceof Enum) {
                return ((Enum<?>) value).name();
            }
            return value.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime getLocalDateTimeFromDate(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }

            if (value instanceof Date) {
                Date date = (Date) value;
                return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
            }

            String dateStr = value.toString();
            return getLocalDateTimeValue(dateStr);

        } catch (Exception e) {
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
            return getLocalDateTimeValue(dateTimeStr);

        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime getLocalDateTimeValue(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException e) {

            }
        }
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

    private String getJooqJsonAsString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return "{}";
        try {
            if (value instanceof JSON) {
                return ((JSON) value).data();
            }
            if (value instanceof JsonNode) {
                return objectMapper.writeValueAsString(value);
            }
            if (value instanceof Map || value instanceof Iterable) {
                return objectMapper.writeValueAsString(value);
            }
            String str = value.toString().trim();
            objectMapper.readTree(str);
            return str;
        } catch (Exception e) {
            return "{}";
        }
    }

    // Test data for Sales - matching the existing streaming format from KGBPL
//    public static String rawStreamingData = "{\n" +
//                                                    "  \"requestId\": \"test-req-SALES-001\",\n" +
//                                                    "  \"groupId\": \"2025-11-04 11:30:02:239\",\n" +
//                                                    "  \"fileId\": null,\n" +
//                                                    "  \"lob\": \"kgbpl\",\n" +
//                                                    "  \"loginId\": \"integration_user_kgbpl\",\n" +
//                                                    "  \"submittedBy\": null,\n" +
//                                                    "  \"batchNumber\": 0,\n" +
//                                                    "  \"transformerInfo\": [\n" +
//                                                    "    {\n" +
//                                                    "      \"transformerId\": \"genericSalesTransformer\",\n" +
//                                                    "      \"skipPreprocessing\": false,\n" +
//                                                    "      \"skipPersist\": false,\n" +
//                                                    "      \"entityName\": \"Sales\",\n" +
//                                                    "      \"operationType\": \"insert\"\n" +
//                                                    "    }\n" +
//                                                    "  ],\n" +
//                                                    "  \"features\": [\n" +
//                                                    "    {\n" +
//                                                    "      \"id\": \"SALES001\",\n" +
//                                                    "      \"activeStatus\": \"ACTIVE\",\n" +
//                                                    "      \"activeStatusReason\": \"Valid sales entry\",\n" +
//                                                    "      \"changed\": true,\n" +
//                                                    "      \"createdBy\": \"system_user\",\n" +
//                                                    "      \"creationTime\": \"2025-11-04T10:15:30\",\n" +
//                                                    "      \"extendedAttributes\": { \"division\": \"PRIMARY\", \"region\": \"NORTH\" },\n" +
//                                                    "      \"hash\": \"sales_hash_abc123\",\n" +
//                                                    "      \"lastModifiedTime\": \"2025-11-04T11:30:02\",\n" +
//                                                    "      \"lob\": \"kgbpl\",\n" +
//                                                    "      \"modifiedBy\": \"admin_user\",\n" +
//                                                    "      \"source\": \"ERP_SYSTEM\",\n" +
//                                                    "      \"version\": \"1\",\n" +
//                                                    "      \"systemTime\": \"2025-11-04T11:30:02\",\n" +
//                                                    "      \"gpsLatitude\": \"28.7041\",\n" +
//                                                    "      \"gpsLongitude\": \"77.1025\",\n" +
//                                                    "      \"billAmount\": \"461739.58\",\n" +
//                                                    "      \"userHierarchy\": \"SALES_ZONE01|REGION01\",\n" +
//                                                    "      \"initialAmount\": \"461739.58\",\n" +
//                                                    "      \"locationHierarchy\": \"ZONE01|STATE01|CITY01\",\n" +
//                                                    "      \"mrp\": \"461739.58\",\n" +
//                                                    "      \"name\": \"Primary Sales Invoice\",\n" +
//                                                    "      \"netAmount\": \"461739.58\",\n" +
//                                                    "      \"normalizedVolume\": \"1078.0\",\n" +
//                                                    "      \"orderNumber\": \"SOPBGT2526-13544\",\n" +
//                                                    "      \"orderedDate\": \"2025-11-04T09:00:00\",\n" +
//                                                    "      \"payByDate\": \"2025-11-14T23:59:59\",\n" +
//                                                    "      \"programNumber\": \"PROG2025\",\n" +
//                                                    "      \"remarks\": \"Primary sales transaction\",\n" +
//                                                    "      \"size\": \"BULK\",\n" +
//                                                    "      \"status\": \"CONFIRMED\",\n" +
//                                                    "      \"supplierId\": \"SUPP001\",\n" +
//                                                    "      \"hierarchy\": \"NATIONAL|ZONE01|STATE01\",\n" +
//                                                    "      \"type\": \"PRIMARY\",\n" +
//                                                    "      \"loginId\": \"SALES_REP_001\",\n" +
//                                                    "      \"outletCode\": \"305601\",\n" +
//                                                    "      \"orderType\": \"REGULAR\",\n" +
//                                                    "      \"invoiceNumber\": \"TI-JLBD26-08591\",\n" +
//                                                    "      \"referenceNumber\": \"REF_TI-JLBD26-08591\",\n" +
//                                                    "      \"statusReason\": \"Successfully processed\",\n" +
//                                                    "      \"saleCondition\": \"CREDIT\",\n" +
//                                                    "      \"totalQuantity\": \"1078.0\",\n" +
//                                                    "      \"totalInitialQuantity\": \"1078.0\",\n" +
//                                                    "      \"normalizedQuantity\": \"1078.0\",\n" +
//                                                    "      \"initialNormalizedQuantity\": \"1078.0\",\n" +
//                                                    "      \"deliveryDate\": \"2025-11-05T10:00:00\",\n" +
//                                                    "      \"discountInfo\": { \"totalDiscount\": \"11420.2\", \"specialDiscount\": \"5715.04\" },\n" +
//                                                    "      \"beat\": \"BEAT_JLBD049\",\n" +
//                                                    "      \"beatName\": \"Jalandhar Beat 49\",\n" +
//                                                    "      \"inBeat\": true,\n" +
//                                                    "      \"inRange\": true,\n" +
//                                                    "      \"amount\": \"461739.58\",\n" +
//                                                    "      \"routeId\": \"ROUTE_JLBD_001\",\n" +
//                                                    "      \"nw\": \"1078.0\",\n" +
//                                                    "      \"invSerNo\": \"INV_SER_08591\"\n" +
//                                                    "    }\n" +
//                                                    "  ],\n" +
//                                                    "  \"appId\": \"integration\",\n" +
//                                                    "  \"retryCount\": 0,\n" +
//                                                    "  \"preserveOnFailure\": true,\n" +
//                                                    "  \"ignoreS3Log\": false\n" +
//                                                    "}";
}