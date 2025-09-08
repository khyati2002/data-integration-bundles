package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSON;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScoreDetailsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

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
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("source", getString(inputMap, "source"));
        result.put("version", getInteger(inputMap, "version"));


        result.put("closingPoints", getDouble(inputMap, "closingPoints"));
        result.put("endDate", getString(inputMap, "endDate"));
        result.put("feature", getString(inputMap, "feature"));
        result.put("openingPoints", getDouble(inputMap, "openingPoints"));
        result.put("pointsBreakup", getJooqJsonAsString(inputMap, "pointsBreakup"));
        result.put("startDate", getString(inputMap, "startDate"));
        result.put("totalPoints", getDouble(inputMap, "totalPoints"));
        result.put("locationHierarchy", parseLocationHierarchy(inputMap, "locationHierarchy"));
        result.put("loginid", getString(inputMap, "loginid"));
        result.put("outletcode", getString(inputMap, "outletcode"));
        result.put("programNumber", getString(inputMap, "programNumber"));
        result.put("currentVolumn", getDouble(inputMap, "currentVolumn"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("changed", getByte(inputMap, "changed"));

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

    private String getJooqJsonAsString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof JSON) {
                return ((JSON) value).data();
            }
            return value.toString();
        } catch (Exception e) {
            return "{}";
        }
    }


    @SuppressWarnings("unchecked")
    private Object parseLocationHierarchy(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;


        if (value instanceof Map || value instanceof JsonNode) return value;


        if (value instanceof JSON) {
            try { return objectMapper.readValue(((JSON) value).data(), Map.class); } catch (Exception ignored) {}
        }

        if (!(value instanceof String)) return value;

        String s = ((String) value).trim();

        // Try plain JSON first (object or array)
        if ((s.startsWith("{") && s.endsWith("}")) || (s.startsWith("[") && s.endsWith("]")) || s.contains(":")) {
            try { return objectMapper.readValue(s, Map.class); } catch (Exception ignored) {}
        }

        if (s.startsWith("{") && s.endsWith("}") && s.contains("=") && !s.contains(":")) {
            String inner = s.substring(1, s.length() - 1).trim();
            Map<String, Object> out = new LinkedHashMap<>();
            if (!inner.isEmpty()) {
                for (String part : inner.split(",\\s*")) {
                    String[] kv = part.split("=", 2);
                    out.put(kv[0].trim(), kv.length > 1 ? kv[1].trim() : "");
                }
            }
            return out;
        }

        if (s.contains("|")) {
            String[] p = s.split("\\|", -1);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("zone",  p.length > 0 ? p[0] : "");
            out.put("state", p.length > 1 ? p[1] : "");
            out.put("city",  p.length > 2 ? p[2] : "");
            out.put("area",  p.length > 3 ? p[3] : "");
            return out;
        }

        // Final attempt to parse as JSON, otherwise return raw string
        try { return objectMapper.readValue(s, Map.class); } catch (Exception ignored) {}
        return s;
    }



}