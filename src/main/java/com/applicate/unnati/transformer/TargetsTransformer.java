package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.TargetResults;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;


import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TargetsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

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
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("source", getString(inputMap, "source"));
        result.put("version", getInteger(inputMap, "version"));


        result.put("endDate", getString(inputMap, "endDate"));
        result.put("outletType", getString(inputMap, "outletType"));
        result.put("outletValue", getJsonNode(inputMap, "outletValue"));
        result.put("productType", getString(inputMap, "productType"));
        result.put("productValue", getJsonNode(inputMap, "productValue"));
        result.put("startDate", getString(inputMap, "startDate"));
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
        result.put("targetResults", getList(inputMap, "targetResults", TargetResults.class));

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

    private <T> List<T> getList(Map<String, Object> map, String key, Class<T> clazz) {
        Object value = map.get(key);
        if (value == null) return Collections.emptyList();
        try {
            if (value instanceof List<?>) {
                return ((List<?>) value).stream()
                        .filter(clazz::isInstance)
                        .map(clazz::cast)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
        }
        return Collections.emptyList();
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

}


/*
        sample srd

            public static String rawStreamingData = "{\n" +
            "  \"requestId\": \"test-req-TARGET-001\",\n" +
            "  \"groupId\": \"2025-09-08\",\n" +
            "  \"lob\": \"cktestitcloyalty\",\n" +
            "  \"loginId\": \"integration_user\",\n" +
            "  \"batchNumber\": 1,\n" +
            "  \"transformerInfo\": [\n" +
            "    {\n" +
            "      \"skipPreprocessing\": false,\n" +
            "      \"skipPersist\": false,\n" +
            "      \"entityName\": \"Targets\",\n" +
            "      \"transformerId\": \"genericTargetsTransformer\",\n" +
            "      \"operationType\": \"insert\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"features\": [\n" +
            "    {\n" +
            "      \"id\": \"TARGET12345\",\n" +
            "      \"activeStatus\": \"ACTIVE\",\n" +
            "      \"activeStatusReason\": \"Valid target entry\",\n" +
            "      \"createdBy\": \"system_user\",\n" +
            "      \"extendedAttributes\": \"{ \\\"priority\\\": \\\"HIGH\\\", \\\"quarter\\\": \\\"Q3\\\" }\",\n" +
            "      \"hash\": \"abc123def456\",\n" +
            "      \"lob\": \"FMCG\",\n" +
            "      \"modifiedBy\": \"admin_user\",\n" +
            "      \"source\": \"IntegrationAPI\",\n" +
            "      \"version\": \"1\",\n" +
            "      \"endDate\": \"2025-03-07 00:00:00T00:00:00Z\",\n" +
            "      \"outletType\": \"RETAIL\",\n" +
            "      \"outletValue\": \"{ \\\"categories\\\": [\\\"GROCERY\\\", \\\"PHARMACY\\\"], \\\"regions\\\": [\\\"NORTH\\\", \\\"SOUTH\\\"] }\",\n" +
            "      \"productType\": \"BRAND\",\n" +
            "      \"productValue\": \"{ \\\"brands\\\": [\\\"BRAND_A\\\", \\\"BRAND_B\\\"], \\\"skus\\\": [\\\"SKU001\\\", \\\"SKU002\\\"] }\",\n" +
            "      \"startDate\": \"2025-03-07 00:00:00T00:00:00Z\",\n" +
            "      \"target\": \"50000.75\",\n" +
            "      \"targetId\": \"TGT2025Q3001\",\n" +
            "      \"targetName\": \"Q3 Sales Target - Premium Products\",\n" +
            "      \"targetTable\": \"sales_targets\",\n" +
            "      \"targetType\": \"SALES_VOLUME\",\n" +
            "      \"unit\": \"INR\",\n" +
            "      \"userType\": \"SALES_REP\",\n" +
            "      \"userValue\": \"{ \\\"roles\\\": [\\\"SALES_EXECUTIVE\\\", \\\"TERRITORY_MANAGER\\\"], \\\"levels\\\": [\\\"L2\\\", \\\"L3\\\"] }\",\n" +
            "      \"changed\": \"1\",\n" +
            "      \"targetcondition\": \"80.5\",\n" +
            "      \"targetconditionunit\": \"PERCENTAGE\",\n" +
            "      \"userValueStr\": \"SALES_REP|TERRITORY_001|ZONE_NORTH\",\n" +
            "      \"outletValueStr\": \"RETAIL|GROCERY|TIER_A\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

 */
