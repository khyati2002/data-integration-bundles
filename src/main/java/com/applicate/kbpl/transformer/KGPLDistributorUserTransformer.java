package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.Map;

public class KGPLDistributorUserTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        // 1. activeStatus = setActiveFunctionkbpl(isactive)
        Object isActive = inputMap.get("isactive");
        output.put("activeStatus", setActiveFunctionkbpl(isActive));

        // 2 & 3. loginId and userAccountId = appendKGPL(distributorcode)
        String distributorCode = safeString(inputMap.get("distributorcode"));
        String appendedCode = appendKGPL(distributorCode);
        output.put("loginId", appendedCode);
        output.put("userAccountId", appendedCode);

        // 4. email = email (direct)
        output.put("email", inputMap.get("email"));

        // 5. name = distributorname (direct)
        output.put("name", safeString(inputMap.get("distributorname")));

        // 6. address (direct)
        output.put("address", safeString(inputMap.get("address")));

        // 7. mobile = phone (direct)
        output.put("mobile", inputMap.get("phone"));

        // 8. locationHierarchy = createLocationKbpl(country=India, zip)
        String zip = safeString(inputMap.get("zip"));
        Map<String, Object> locationHierarchy = createLocationKbpl("India", zip);
        output.put("locationHierarchy", locationHierarchy);

        // 9. designation = constant "supplier"
        output.put("designation", "supplier");

        // 10. extendedAttributes = toMap(tenantcode)
        Object tenantCode = inputMap.get("tenantcode");
        Map<String, Object> extendedAttrs = toMap("tenantcode", tenantCode);
        output.put("extendedAttributes", extendedAttrs);

        return output;
    }

    private static String safeString(Object obj) {
        if (obj == null) return null;
        String s = obj.toString();
        return s.equalsIgnoreCase("null") || s.trim().isEmpty() ? null : s;
    }

    // Mimics the setActiveFunctionkbpl transformation logic
    private String setActiveFunctionkbpl(Object isActive) {
        return (isActive != null && isActive.toString().equals("1")) ? "active" : "inactive";
    }

    // Mimics the appendKGPL function (appends "KGPL" suffix)
    private String appendKGPL(String code) {
        return code != null ? code + "KGPL" : null;
    }

    // Mimics createLocationKbpl(country, zip) function from provided JS
    private Map<String, Object> createLocationKbpl(String country, String zip) {
        Map<String, Object> location = new HashMap<>();
        location.put("country", country);
        if (zip != null && !zip.isEmpty()) {
            location.put("pincode", zip);
        }
        return location;
    }

    // Mimics toMap function that puts one key-value pair in a map
    private Map<String, Object> toMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}


