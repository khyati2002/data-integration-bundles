package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSON;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SalesHistoryTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        result.put("saleId", getString(inputMap, "invoiceNumber"));
        result.put("status", getStatusUpperCase(inputMap, "status"));
        result.put("transactionDetails", getJooqJsonAsString(inputMap, "transactionDetails"));
        result.put("rowid", getInteger(inputMap, "rowid"));

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

    private String getStatusUpperCase(Map<String, Object> map, String key) {
        String value = getString(map, key);
        return (value != null) ? value.toUpperCase() : null;
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

    // Mock streaming data for SalesHistory testing
//    public static String rawStreamingData = "{\n" +
//                                                    "  \"requestId\": \"test-req-SALES-HISTORY-001\",\n" +
//                                                    "  \"groupId\": \"2025-11-06\",\n" +
//                                                    "  \"lob\": \"kgbpl\",\n" +
//                                                    "  \"loginId\": \"integration_user\",\n" +
//                                                    "  \"batchNumber\": 1,\n" +
//                                                    "  \"transformerInfo\": [\n" +
//                                                    "    {\n" +
//                                                    "      \"skipPreprocessing\": false,\n" +
//                                                    "      \"skipPersist\": false,\n" +
//                                                    "      \"entityName\": \"SalesHistory\",\n" +
//                                                    "      \"transformerId\": \"genericSalesHistoryTransformer\",\n" +
//                                                    "      \"operationType\": \"insert\"\n" +
//                                                    "    }\n" +
//                                                    "  ],\n" +
//                                                    "  \"features\": [\n" +
//                                                    "    {\n" +
//                                                    "      \"id\": \"SALES_HIST_001\",\n" +
//                                                    "      \"activeStatus\": \"ACTIVE\",\n" +
//                                                    "      \"activeStatusReason\": \"Valid sales history entry\",\n" +
//                                                    "      \"changed\": true,\n" +
//                                                    "      \"createdBy\": \"system_user\",\n" +
//                                                    "      \"creationTime\": \"2025-11-06T10:15:30\",\n" +
//                                                    "      \"extendedAttributes\": { \"source\": \"ERP\", \"module\": \"SALES\" },\n" +
//                                                    "      \"hash\": \"sales_hist_hash_abc123\",\n" +
//                                                    "      \"lastModifiedTime\": \"2025-11-06T11:30:02\",\n" +
//                                                    "      \"lob\": \"kgbpl\",\n" +
//                                                    "      \"modifiedBy\": \"admin_user\",\n" +
//                                                    "      \"source\": \"ERP_SYSTEM\",\n" +
//                                                    "      \"version\": \"1\",\n" +
//                                                    "      \"invoiceNumber\": \"TI-JLBD26-08591\",\n" +
//                                                    "      \"status\": \"generated\",\n" +
//                                                    "      \"transactionDetails\": { \"action\": \"CREATED\", \"remarks\": \"Invoice generated successfully\", \"timestamp\": \"2025-11-06T10:15:30\" }\n" +
//                                                    "    },\n" +
//                                                    "    {\n" +
//                                                    "      \"id\": \"SALES_HIST_002\",\n" +
//                                                    "      \"activeStatus\": \"ACTIVE\",\n" +
//                                                    "      \"activeStatusReason\": \"Valid sales history entry\",\n" +
//                                                    "      \"changed\": true,\n" +
//                                                    "      \"createdBy\": \"system_user\",\n" +
//                                                    "      \"creationTime\": \"2025-11-06T11:00:00\",\n" +
//                                                    "      \"extendedAttributes\": { \"source\": \"ERP\", \"module\": \"SALES\" },\n" +
//                                                    "      \"hash\": \"sales_hist_hash_def456\",\n" +
//                                                    "      \"lastModifiedTime\": \"2025-11-06T11:30:02\",\n" +
//                                                    "      \"lob\": \"kgbpl\",\n" +
//                                                    "      \"modifiedBy\": \"admin_user\",\n" +
//                                                    "      \"source\": \"ERP_SYSTEM\",\n" +
//                                                    "      \"version\": \"1\",\n" +
//                                                    "      \"invoiceNumber\": \"TI-JLBD26-08591\",\n" +
//                                                    "      \"status\": \"confirmed\",\n" +
//                                                    "      \"transactionDetails\": { \"action\": \"CONFIRMED\", \"confirmedBy\": \"SALES_MANAGER_001\", \"remarks\": \"Order confirmed by sales manager\", \"timestamp\": \"2025-11-06T11:00:00\" }\n" +
//                                                    "    },\n" +
//                                                    "    {\n" +
//                                                    "      \"id\": \"SALES_HIST_003\",\n" +
//                                                    "      \"activeStatus\": \"ACTIVE\",\n" +
//                                                    "      \"activeStatusReason\": \"Valid sales history entry\",\n" +
//                                                    "      \"changed\": true,\n" +
//                                                    "      \"createdBy\": \"system_user\",\n" +
//                                                    "      \"creationTime\": \"2025-11-06T14:30:00\",\n" +
//                                                    "      \"extendedAttributes\": { \"source\": \"ERP\", \"module\": \"SALES\" },\n" +
//                                                    "      \"hash\": \"sales_hist_hash_ghi789\",\n" +
//                                                    "      \"lastModifiedTime\": \"2025-11-06T14:30:00\",\n" +
//                                                    "      \"lob\": \"kgbpl\",\n" +
//                                                    "      \"modifiedBy\": \"system_user\",\n" +
//                                                    "      \"source\": \"WAREHOUSE_SYSTEM\",\n" +
//                                                    "      \"version\": \"1\",\n" +
//                                                    "      \"invoiceNumber\": \"TI-JLBD26-08591\",\n" +
//                                                    "      \"status\": \"processing\",\n" +
//                                                    "      \"transactionDetails\": { \"action\": \"PROCESSING\", \"warehouseCode\": \"WH_JLBD049\", \"pickingStarted\": true, \"remarks\": \"Order processing initiated\", \"timestamp\": \"2025-11-06T14:30:00\" }\n" +
//                                                    "    },\n" +
//                                                    "    {\n" +
//                                                    "      \"id\": \"SALES_HIST_004\",\n" +
//                                                    "      \"activeStatus\": \"ACTIVE\",\n" +
//                                                    "      \"activeStatusReason\": \"Valid sales history entry\",\n" +
//                                                    "      \"changed\": true,\n" +
//                                                    "      \"createdBy\": \"system_user\",\n" +
//                                                    "      \"creationTime\": \"2025-11-06T16:45:00\",\n" +
//                                                    "      \"extendedAttributes\": { \"source\": \"ERP\", \"module\": \"SALES\" },\n" +
//                                                    "      \"hash\": \"sales_hist_hash_jkl012\",\n" +
//                                                    "      \"lastModifiedTime\": \"2025-11-06T16:45:00\",\n" +
//                                                    "      \"lob\": \"kgbpl\",\n" +
//                                                    "      \"modifiedBy\": \"system_user\",\n" +
//                                                    "      \"source\": \"LOGISTICS_SYSTEM\",\n" +
//                                                    "      \"version\": \"1\",\n" +
//                                                    "      \"invoiceNumber\": \"TI-JLBD26-08591\",\n" +
//                                                    "      \"status\": \"shipped\",\n" +
//                                                    "      \"transactionDetails\": { \"action\": \"SHIPPED\", \"carrierCode\": \"CARRIER_001\", \"trackingNumber\": \"TRK12345678\", \"vehicleNumber\": \"PB01AB1234\", \"driverName\": \"Rajesh Kumar\", \"remarks\": \"Shipment dispatched\", \"timestamp\": \"2025-11-06T16:45:00\" }\n" +
//                                                    "    },\n" +
//                                                    "    {\n" +
//                                                    "      \"id\": \"SALES_HIST_005\",\n" +
//                                                    "      \"activeStatus\": \"ACTIVE\",\n" +
//                                                    "      \"activeStatusReason\": \"Valid sales history entry\",\n" +
//                                                    "      \"changed\": true,\n" +
//                                                    "      \"createdBy\": \"system_user\",\n" +
//                                                    "      \"creationTime\": \"2025-11-07T10:00:00\",\n" +
//                                                    "      \"extendedAttributes\": { \"source\": \"ERP\", \"module\": \"SALES\" },\n" +
//                                                    "      \"hash\": \"sales_hist_hash_mno345\",\n" +
//                                                    "      \"lastModifiedTime\": \"2025-11-07T10:00:00\",\n" +
//                                                    "      \"lob\": \"kgbpl\",\n" +
//                                                    "      \"modifiedBy\": \"system_user\",\n" +
//                                                    "      \"source\": \"DELIVERY_SYSTEM\",\n" +
//                                                    "      \"version\": \"1\",\n" +
//                                                    "      \"invoiceNumber\": \"TI-JLBD26-08591\",\n" +
//                                                    "      \"status\": \"delivered\",\n" +
//                                                    "      \"transactionDetails\": { \"action\": \"DELIVERED\", \"deliveredBy\": \"Rajesh Kumar\", \"receivedBy\": \"Store Manager\", \"signatureImage\": \"sig_img_url\", \"deliveryLocation\": { \"lat\": \"28.7041\", \"lon\": \"77.1025\" }, \"remarks\": \"Successfully delivered\", \"timestamp\": \"2025-11-07T10:00:00\" }\n" +
//                                                    "    }\n" +
//                                                    "  ],\n" +
//                                                    "  \"appId\": \"integration\",\n" +
//                                                    "  \"retryCount\": 0,\n" +
//                                                    "  \"preserveOnFailure\": true,\n" +
//                                                    "  \"ignoreS3Log\": false\n" +
//                                                    "}";
}