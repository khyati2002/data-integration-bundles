package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.Map;

public class ENRICHDistributorUserTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        // 1. activeStatus = setActiveFunctionkbpl(isactive)
        Object isActive = inputMap.get("isactive");
        output.put("activeStatus", setActiveFunctionkbpl(isActive));

        // 2 & 3. loginId and userAccountId = appendEnrich(distributorcode)
        String distributorCode = safeString(inputMap.get("distributorcode"));
        String appendedCode = appendEnrich(distributorCode);
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
        output.put("designation", toConstant());

        // 10. extendedAttributes = toMap(tenantcode, parentdistributorcode)
        Object tenantCode = inputMap.get("tenantcode");
        Object parentDistributorCode = inputMap.get("parentdistributorcode");
        Map<String, Object> extendedAttributes = toMap(tenantCode, parentDistributorCode);
        output.put("extendedAttributes", extendedAttributes);

        return output;
    }

    // Safely convert object to string, return null if blank or "null"
    private static String safeString(Object obj) {
        if (obj == null) return null;
        String s = obj.toString();
        return s.trim().isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }

    // setActiveFunctionkbpl logic: "1" -> "active", else "inactive"
    private String setActiveFunctionkbpl(Object isActive) {
        return (isActive != null && isActive.toString().equals("1")) ? "active" : "inactive";
    }

    // appendEnrich appends "-ENRICH" suffix if input is not null
    private String appendEnrich(String code) {
        return code != null ? code + "-ENRICH" : null;
    }

    // createLocationKbpl creates map with country and optional pincode
    private Map<String, Object> createLocationKbpl(String country, String zip) {
        Map<String, Object> location = new HashMap<>();
        location.put("country", country);
        if (zip != null && !zip.isEmpty()) {
            location.put("pincode", zip);
        }
        return location;
    }

    // toConstant returns a constant string "supplier"
    private String toConstant() {
        return "supplier";
    }

    // toMap returns a map with tenantcode and parentdistributorcode keys and values
    private Map<String, Object> toMap(Object tenantCode, Object parentDistributorCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("tenantcode", tenantCode);
        map.put("parentdistributorcode", parentDistributorCode);
        return map;
    }
}

