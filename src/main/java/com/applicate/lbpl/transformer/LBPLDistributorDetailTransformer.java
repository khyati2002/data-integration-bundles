package com.applicate.lbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.GenericEntity;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LBPLDistributorDetailTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        // Extract input fields
        String distributorCode = getStringValue(inputMap, "distributorcode");
        String distributorName = getStringValue(inputMap, "distributorname");
        String isActive = getStringValue(inputMap, "isactive");
        String email = getStringValue(inputMap, "email");
        String mobile = getStringValue(inputMap, "mobile");
        String zip = getStringValue(inputMap, "zip");
        String address = getStringValue(inputMap, "address");
        String categoryCode1 = getStringValue(inputMap, "categorycode1");
        String phone = getStringValue(inputMap, "phone");
        String tenantCode = getStringValue(inputMap, "tenantcode");
        String categoryCode4 = getStringValue(inputMap, "categorycode4");

        // Apply transformations
        String userAccountId = distributorCode;
        String loginId = distributorCode;
        String name = distributorName;
        String activeStatus = activeFunction(isActive);
        String emailField = email;
        String mobileField = countryCodeCheck(mobile);
        ObjectNode locationHierarchy = createLocationLbpl(zip);
        String addressField = address;
        String designation = "supplier";
        Map<String, String> extendedAttributes = toMap(
                new String[]{"categorycode1", "phone", "tenantcode", "categorycode4"},
                new String[]{categoryCode1, phone, tenantCode, categoryCode4}
        );

        // Create GenericEntity
        GenericEntity entity = new GenericEntity();
        entity.setId(userAccountId);
        entity.setName(name);
        entity.setKey1(distributorCode);

        // Build payload
        ObjectNode payload = JSONUtils.getObjectMapper().createObjectNode();
        payload.put("userAccountId", userAccountId);
        payload.put("loginId", loginId);
        payload.put("name", name);
        payload.put("activeStatus", activeStatus);
        payload.put("email", emailField);
        payload.put("mobile", mobileField);
        payload.set("locationHierarchy", locationHierarchy);
        payload.put("address", addressField);
        payload.put("designation", designation);

        // Add extendedAttributes
        ObjectNode extendedAttrsNode = JSONUtils.getObjectMapper().createObjectNode();
        extendedAttributes.forEach(extendedAttrsNode::put);
        payload.set("extendedAttributes", extendedAttrsNode);

        entity.setPayload(payload);

        // Convert to Map
        Map<String, Object> entityMap = new HashMap<>();
        entityMap.put("id", entity.getId());
        entityMap.put("name", entity.getName());
        entityMap.put("key1", entity.getKey1());
        entityMap.put("payload", entity.getPayload());

        return Collections.singletonList(entityMap);
    }

    // Helper method to safely get string values
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : "";
    }

    // Conversion functions
    private String activeFunction(String param) {
        if (param != null && !param.isEmpty() && "1".equals(param)) {
            return "active";
        } else {
            return "inactive";
        }
    }

    private String countryCodeCheck(String mobile) {
        if (mobile == null || mobile.isEmpty()) {
            return mobile;
        }

        String strMobile = String.valueOf(mobile);

        if (strMobile.length() > 10) {
            if (strMobile.startsWith("91")) {
                strMobile = strMobile.substring(2);
            }
        }
        return strMobile;
    }

    private ObjectNode createLocationLbpl(String state) {
        ObjectNode location = JSONUtils.getObjectMapper().createObjectNode();
        location.put("pincode", (String) null);
        location.put("country", "India");
        return location;
    }

    private Map<String, String> toMap(String[] keys, String[] values) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keys.length && i < values.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }
}