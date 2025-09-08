package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.Location;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSON;

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
    private Location parseLocationHierarchy(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        Location location = new Location();
        if (value instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) value;
            location.setCountry((String) m.get("country"));
            location.setRegion((String) m.get("region"));
            location.setState((String) m.get("state"));
            location.setCity((String) m.get("city"));
            location.setPincode((String) m.get("pincode"));
            location.setZone((String) m.get("zone"));
            location.setCountryCode((String) m.get("countryCode"));
            location.setRegionCode((String) m.get("regionCode"));
            location.setStateCode((String) m.get("stateCode"));
            location.setCityCode((String) m.get("cityCode"));
            location.setZoneCode((String) m.get("zoneCode"));
            return location;
        }
        return null;
    }



}