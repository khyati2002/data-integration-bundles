package com.applicate.lbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.GenericEntity;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LBPLSalesmanTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        // Extract input fields
        String tenantCode = getStringValue(inputMap, "tenantcode");
        String salesmanCode = getStringValue(inputMap, "salesmancode");
        String salesmanName = getStringValue(inputMap, "salesmanname");
        String phone = getStringValue(inputMap, "phone");
        String isActive = getStringValue(inputMap, "isactive");
        String address = getStringValue(inputMap, "address");
        String city = getStringValue(inputMap, "city");
        String state = getStringValue(inputMap, "state");
        String zip = getStringValue(inputMap, "zip");
        String locationCode = getStringValue(inputMap, "locationcode");

        // Apply transformations
        String immediateParent = convertToString(tenantCode);
        String addressField = addressFunction(address, state, city, zip);
        String activeStatus = activeFunction(isActive);
        String mobile = phone;
        String name = salesmanName;
        String userAccountId = concatLoginid(tenantCode, salesmanCode);
        String loginId = concatLoginid(tenantCode, salesmanCode);
        ObjectNode locationHierarchy = createLocationSLMG(state, zip, city);
        String designation = "mgr";
        Map<String, String> extendedAttributes = toMap(new String[]{"locationcode"}, new String[]{locationCode});

        // Create GenericEntity
        GenericEntity entity = new GenericEntity();
        entity.setId(userAccountId); // or loginId, both are same
        entity.setName(name);
        entity.setKey1(tenantCode);

        // Build payload
        ObjectNode payload = JSONUtils.getObjectMapper().createObjectNode();
        payload.put("immediateParent", immediateParent);
        payload.put("address", addressField);
        payload.put("activeStatus", activeStatus);
        payload.put("mobile", mobile);
        payload.put("name", name);
        payload.put("userAccountId", userAccountId);
        payload.put("loginId", loginId);
        payload.set("locationHierarchy", locationHierarchy);
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
    private String convertToString(String param) {
        return param != null ? param.toString() : "";
    }

    private String addressFunction(String address, String state, String city, String zip) {
        if (address != null && !address.isEmpty() && !"null".equals(address)) {
            return address;
        } else if (("null".equals(state) && "null".equals(city) && "null".equals(zip)) ||
                ("".equals(state) && "".equals(city) && "".equals(zip))) {
            return "India";
        } else {
            return state + " " + city + " " + zip;
        }
    }

    private String activeFunction(String param) {
        if ("1".equals(param)) {
            return "active";
        } else {
            return "inactive";
        }
    }

    private String concatLoginid(String param1, String param2) {
        return param1 + "-" + param2;
    }

    private ObjectNode createLocationSLMG(String state, String zip, String city) {
        ObjectNode location = JSONUtils.getObjectMapper().createObjectNode();
        location.put("state", state);
        location.put("country", "India");
        location.put("pincode", zip);
        location.put("city", city);
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