package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSON;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class SalesTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>>  {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private SalesHistoryTransformer salesHistoryTransformer;

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
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("source", getString(inputMap, "source"));
        result.put("version", getInteger(inputMap, "version"));
        result.put("systemTime", getString(inputMap, "systemTime"));
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
        result.put("orderedDate", getString(inputMap, "orderedDate"));
        result.put("payByDate", getString(inputMap, "payByDate"));
        result.put("programNumber", getString(inputMap, "programNumber"));
        result.put("remarks", getString(inputMap, "remarks"));
        result.put("size", getString(inputMap, "size"));
        result.put("status", getString(inputMap, "status"));
        result.put("supplierid", getString(inputMap, "supplierId"));
        result.put("hierarchy", getString(inputMap, "hierarchy"));
        result.put("type", getString(inputMap, "type"));

        result.put("loginid", getString(inputMap, "loginId"));

        result.put("outletcode", getString(inputMap, "outletCode"));

        // Handle orderType - convert enum to String if needed
        result.put("orderType", getOrderTypeAsString(inputMap, "orderType"));

        result.put("invoiceNumber", getString(inputMap, "invoiceNumber"));
        result.put("referenceNumber", getString(inputMap, "referenceNumber"));
        result.put("statusReason", getString(inputMap, "statusReason"));
        result.put("saleCondition", getString(inputMap, "saleCondition"));

        result.put("totalQuantity", getDouble(inputMap, "totalQuantity"));
        result.put("totalInitialQuantity", getDouble(inputMap, "totalInitialQuantity"));
        result.put("normalizedQuantity", getDouble(inputMap, "normalizedQuantity"));
        result.put("initialNormalizedQuantity", getDouble(inputMap, "initialNormalizedQuantity"));

        result.put("deliveryDate", getString(inputMap, "deliveryDate"));

        result.put("discountInfo", getJooqJsonAsString(inputMap, "discountInfo"));


        result.put("beat", getString(inputMap, "beat"));
        result.put("beatName", getString(inputMap, "beatName"));

        result.put("inBeat", getBoolean(inputMap, "inBeat"));
        result.put("inRange", getBoolean(inputMap, "inRange"));

        result.put("rowid", getInteger(inputMap, "rowid"));
        result.put("amount", getDouble(inputMap, "amount"));
        result.put("routeId", getString(inputMap, "routeId"));
        result.put("nw", getDouble(inputMap, "nw"));
        result.put("invSerNo", getString(inputMap, "invSerNo"));
        result.put("salesHistory",getSalesHistoryList(inputMap));

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


    private List<Map<String, Object>> getSalesHistoryList(Map<String, Object> rawMap) {
        Object value = rawMap.get("salesHistory");
        if (value == null) return Collections.emptyList();

        if (value instanceof List<?>) {
            return ((List<?>) value).stream()
                    .filter(Objects::nonNull)
                    .map(item -> {
                        return salesHistoryTransformer.transform((Map<String, Object>) item);
                    })
                    .filter(obj -> true)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
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
