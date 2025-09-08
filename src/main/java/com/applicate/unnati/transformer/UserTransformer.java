package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.Location;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UserTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>>  {

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
        result.put("version", getInteger(inputMap, "version"));

        // User specific fields
        result.put("address", getString(inputMap, "address"));
        result.put("contactType", getString(inputMap, "contactType"));
        result.put("countryCode", getString(inputMap, "countryCode"));
        result.put("email", getString(inputMap, "email"));
        result.put("hierarchy", getString(inputMap, "hierarchy"));
        result.put("lastPasswordResetDate", getString(inputMap, "lastPasswordResetDate"));
        result.put("loginid", getString(inputMap, "loginid"));
        result.put("mobile", getString(inputMap, "mobile"));
        result.put("name", getString(inputMap, "name"));
        result.put("password", getString(inputMap, "password"));
        result.put("useraccountid", getString(inputMap, "useraccountid"));
        result.put("usercontext", getString(inputMap, "usercontext"));
        result.put("webcontext", getString(inputMap, "webcontext"));
        result.put("locationHierarchy", parseLocationHierarchy(inputMap, "locationHierarchy"));
        result.put("source", getString(inputMap, "source"));
        result.put("registeredNumber", getString(inputMap, "registeredNumber"));
        result.put("facebookpsid", getString(inputMap, "facebookpsid"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("dialCode", getString(inputMap, "dialCode"));
        result.put("ssoId", getString(inputMap, "ssoId"));
        result.put("deviceId", getString(inputMap, "deviceId"));
        result.put("verified", getBoolean(inputMap, "verified"));
        result.put("doa", getString(inputMap, "doa"));
        result.put("dob", getString(inputMap, "dob"));
        result.put("assignedHierarchy", getString(inputMap, "assignedHierarchy"));
        result.put("rowid", getInteger(inputMap, "rowid"));
        result.put("changed", getByte(inputMap, "changed"));
        result.put("blocked", getBoolean(inputMap, "blocked"));
        result.put("normalizedHierarchy", getString(inputMap, "normalizedHierarchy"));
        result.put("alternateId", getString(inputMap, "alternateId"));
        result.put("externalReferenceId", getString(inputMap, "externalReferenceId"));
        result.put("reportPassword", getString(inputMap, "reportPassword"));
        result.put("prodauthcode", getString(inputMap, "prodauthcode"));
        result.put("designation", getDesignation(inputMap, "designation"));


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


    private Location parseLocationHierarchy(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        String str = value.toString();
        String[] parts = str.split("\\|");
        Location location = new Location();
        if (parts.length > 0) location.setZone(parts[0]);
        if (parts.length > 1) location.setState(parts[1]);
        if (parts.length > 2) location.setCity(parts[2]);
        if (parts.length > 3) location.setArea(parts[3]);
        return location;
    }


    @SuppressWarnings("unchecked")
    private Set<String> getDesignation(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return Collections.emptySet();
        if (value instanceof Set)
            return ((Set<?>) value).stream().map(Object::toString).collect(Collectors.toSet());

        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.isEmpty()) return Collections.emptySet();
            return Arrays.stream(s.split("[,|]"))
                    .map(String::trim).filter(p -> !p.isEmpty()).collect(Collectors.toSet());
        }

        return Collections.singleton(value.toString());
    }


}