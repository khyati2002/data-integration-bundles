package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class OutletMetadataTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        result.put("changed", getBoolean(inputMap, "changed"));
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("source", getString(inputMap, "source"));
        result.put("version", getInteger(inputMap, "version"));

        // OutletMetadata specific fields
        result.put("customerCode", getString(inputMap, "customerCode"));
        result.put("groupKey", getString(inputMap, "groupKey"));
        result.put("loginId", getString(inputMap, "loginId"));
        result.put("outletCode", getString(inputMap, "outletCode"));
        result.put("outletId", getString(inputMap, "outletId"));
        result.put("outletUniqueCode", getString(inputMap, "outletUniqueCode"));
        result.put("salesrep", getString(inputMap, "salesrep"));
        result.put("status", getString(inputMap, "status"));
        result.put("supplierCode", getString(inputMap, "supplierCode"));
        result.put("supplierUniqueCode", getString(inputMap, "supplierUniqueCode"));
        result.put("syncedTime", getString(inputMap, "syncedTime"));

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
/* sample srd

public static String rawStreamingData = "{\n" +
        "  \"requestId\": \"test-req-OUTLET-METADATA-001\",\n" +
        "  \"groupId\": \"2025-09-08\",\n" +
        "  \"lob\": \"cktestitcloyalty\",\n" +
        "  \"loginId\": \"integration_user\",\n" +
        "  \"batchNumber\": 1,\n" +
        "  \"transformerInfo\": [\n" +
        "    {\n" +
        "      \"skipPreprocessing\": false,\n" +
        "      \"skipPersist\": false,\n" +
        "      \"entityName\": \"OutletMetadata\",\n" +
        "      \"transformerId\": \"genericOutletMetadataTransformer\",\n" +
        "      \"operationType\": \"insert\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"features\": [\n" +
        "    {\n" +
        "      \"id\": \"OM12345\",\n" +
        "      \"activeStatus\": \"ACTIVE\",\n" +
        "      \"activeStatusReason\": \"Valid outlet metadata entry\",\n" +
        "      \"changed\": \"true\",\n" +
        "      \"createdBy\": \"system_user\",\n" +
        "      \"extendedAttributes\": \"{ \\\"category\\\": \\\"PREMIUM\\\", \\\"tier\\\": \\\"GOLD\\\" }\",\n" +
        "      \"hash\": \"outlet_meta_hash_abc123\",\n" +
        "      \"lob\": \"FMCG\",\n" +
        "      \"modifiedBy\": \"admin_user\",\n" +
        "      \"source\": \"OutletManagementSystem\",\n" +
        "      \"version\": \"1\",\n" +
        "      \"customerCode\": \"CUST001\",\n" +
        "      \"groupKey\": \"GROUP_KEY_RETAIL_001\",\n" +
        "      \"loginId\": \"outlet_manager_001\",\n" +
        "      \"outletCode\": \"OUT_CODE_001\",\n" +
        "      \"outletId\": \"OUTLET_ID_12345\",\n" +
        "      \"outletUniqueCode\": \"UNQ_OUT_001_2025\",\n" +
        "      \"salesrep\": \"SALES_REP_RAMESH_001\",\n" +
        "      \"status\": \"ACTIVE\",\n" +
        "      \"supplierCode\": \"SUPP_001\",\n" +
        "      \"supplierUniqueCode\": \"UNQ_SUPP_001_2025\",\n" +
        "      \"syncedTime\": \"2025-03-07 00:00:00T00:00:00Z\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";

 */


