package com.applicate.cokearg.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.Location;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;

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



/* Sample srd

public static String rawStreamingData = "{\n" +
        "  \"requestId\": \"test-req-USER-001\",\n" +
        "  \"groupId\": \"2025-09-08\",\n" +
        "  \"lob\": \"cktestitcloyalty\",\n" +
        "  \"loginId\": \"integration_user\",\n" +
        "  \"batchNumber\": 1,\n" +
        "  \"transformerInfo\": [\n" +
        "    {\n" +
        "      \"skipPreprocessing\": false,\n" +
        "      \"skipPersist\": false,\n" +
        "      \"entityName\": \"User\",\n" +
        "      \"transformerId\": \"genericUserTransformer\",\n" +
        "      \"operationType\": \"insert\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"features\": [\n" +
        "    {\n" +
        "      \"id\": \"USER12345\",\n" +
        "      \"activeStatus\": \"ACTIVE\",\n" +
        "      \"activeStatusReason\": \"Valid user account\",\n" +
        "      \"createdBy\": \"system_admin\",\n" +
        "      \"extendedAttributes\": \"{ \\\"department\\\": \\\"Sales\\\", \\\"experience\\\": \\\"5_years\\\" }\",\n" +
        "      \"lob\": \"FMCG\",\n" +
        "      \"modifiedBy\": \"hr_admin\",\n" +
        "      \"version\": \"1\",\n" +
        "      \"address\": \"456 Residential Complex, Urban Area, Metropolitan City\",\n" +
        "      \"contactType\": \"MOBILE\",\n" +
        "      \"countryCode\": \"IN\",\n" +
        "      \"email\": \"ramesh.kumar@company.com\",\n" +
        "      \"hierarchy\": \"NATIONAL|ZONE01|STATE01|REGION01\",\n" +
        "      \"lastPasswordResetDate\": \"2025-03-07 00:00:00\",\n" +
        "      \"loginid\": \"ramesh.kumar001\",\n" +
        "      \"mobile\": \"+91-9876543210\",\n" +
        "      \"name\": \"Ramesh Kumar\",\n" +
        "      \"password\": \"encrypted_password_hash\",\n" +
        "      \"useraccountid\": \"UA2025001\",\n" +
        "      \"usercontext\": \"SALES_CONTEXT\",\n" +
        "      \"webcontext\": \"WEB_PORTAL\",\n" +
        "  \"locationHierarchy\": {\n" +
        "    \"zone\": \"NDEL\",\n" +
        "    \"state\": \"NDIS\",\n" +
        "    \"city\": \"CITY01\",\n" +
        "    \"area\": \"AREA01\",\n" +
        "    \"country\": \"INDIA\"\n" +
        "      },\n" +
        "      \"designation\": [\"Manager\", \"Team Lead\", \"Field Officer\"],\n" +
        "      \"source\": \"HR_SYSTEM\",\n" +
        "      \"registeredNumber\": \"+91-9876543210\",\n" +
        "      \"facebookpsid\": \"fb_12345678901234567\",\n" +
        "      \"hash\": \"user_hash_abc123\",\n" +
        "      \"dialCode\": \"+91\",\n" +
        "      \"ssoId\": \"sso_ramesh_001\",\n" +
        "      \"deviceId\": \"device_android_xyz789\",\n" +
        "      \"verified\": true,\n" +
        "      \"doa\": \"2025-03-07 00:00:00\",\n" +
        "      \"dob\": \"1990-05-15 00:00:00\",\n" +
        "      \"assignedHierarchy\": \"TERRITORY_001|BEAT_001\",\n" +
        "      \"rowid\": 10005334,\n" +
        "      \"changed\": 1,\n" +
        "      \"blocked\": false,\n" +
        "      \"normalizedHierarchy\": \"ZONE01>STATE01>REGION01>TERRITORY01\",\n" +
        "      \"alternateId\": \"ALT_ID_001\",\n" +
        "      \"externalReferenceId\": \"EXT_REF_HR_001\",\n" +
        "      \"reportPassword\": \"report_encrypted_hash\",\n" +
        "      \"prodauthcode\": \"PROD_AUTH_001\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";

 */