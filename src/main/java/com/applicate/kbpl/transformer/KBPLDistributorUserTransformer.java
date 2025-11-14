package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.Map;

public class KBPLDistributorUserTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        // 1. activeStatus = setActiveFunctionkbpl(isactive)
        Object isActive = inputMap.get("isactive");
        output.put("activeStatus", setActiveFunctionkbpl(isActive));

        // 2 & 3. loginId and userAccountId = distributorcode (direct)
        Object distributorCode = inputMap.get("distributorcode");
        output.put("loginId", distributorCode);
        output.put("userAccountId", distributorCode);

        // 4. email = email (direct)
        output.put("email", inputMap.get("email"));

        // 5. name = distributorname (direct)
        output.put("name", inputMap.get("distributorname"));

        // 6. address = address (direct)
        output.put("address", inputMap.get("address"));

        // 7. mobile = phone (direct)
        Object phone = inputMap.get("phone");
        output.put("mobile", phone);

        // 8. locationHierarchy = createLocationKbpl(country=india, zip)
        String zip = safeString(inputMap.get("zip"));
        Map<String, Object> locationHierarchy = createLocationKbpl("india", zip);
        output.put("locationHierarchy", locationHierarchy);

        // 9. designation = constant "supplier"
        output.put("designation", toConstant());

        // 10. extendedAttributes = toMap(tenantcode, mobile)
        Object tenantCode = inputMap.get("tenantcode");
        output.put("extendedAttributes", toMap(tenantCode, phone));

        return output;
    }

    // setActiveFunctionkbpl logic: "1" -> "active", else "inactive"
    private String setActiveFunctionkbpl(Object isActive) {
        return (isActive != null && isActive.toString().equals("1")) ? "active" : "inactive";
    }

    // createLocationKbpl per the given JS function logic
    private Map<String, Object> createLocationKbpl(String country, String zip) {
        Map<String, Object> location = new HashMap<>();

        if ((zip == null || zip.isEmpty()) && (country == null || country.isEmpty())) {
            location.put("country", "India");
        } else {
            location.put("country", country);
            if (zip != null && !zip.isEmpty()) {
                location.put("pincode", zip);
            }
        }
        return location;
    }

    // toConstant returns a constant string "supplier"
    private String toConstant() {
        return "supplier";
    }

    // toMap returns a map with tenantcode and mobile keys and respective values
    private Map<String, Object> toMap(Object tenantCode, Object mobile) {
        Map<String, Object> map = new HashMap<>();
        map.put("tenantcode", tenantCode);
        map.put("mobile", mobile);
        return map;
    }

    // Helper: Safely convert an object to string or null if blank
    private static String safeString(Object obj) {
        if (obj == null) return null;
        String s = obj.toString().trim();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }
}

