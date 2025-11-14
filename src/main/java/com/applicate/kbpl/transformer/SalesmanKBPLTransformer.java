package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.Map;

public class SalesmanKBPLTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        // 1. activeStatus = setActiveFunctionkbpl(isactive)
        Object isActive = inputMap.get("isactive");
        output.put("activeStatus", setActiveFunctionkbpl(isActive));

        // 2 & 3. loginId and userAccountId = concatLoginid(salesmancode, tenantcode)
        String salesmanCode = safeString(inputMap.get("salesmancode"));
        String tenantCode = safeString(inputMap.get("tenantcode"));
        String loginId = concatLoginid(salesmanCode, tenantCode);
        output.put("loginId", loginId);
        output.put("userAccountId", loginId);

        // 4. name = salesmanname (direct)
        output.put("name", inputMap.get("salesmanname"));

        // 5. immediateParent = convertToString(tenantcode)
        output.put("immediateParent", convertToString(inputMap.get("tenantcode")));

        // 6. designation = constant "mgr"
        output.put("designation", toConstant());

        // 7. locationHierarchy = createLocation(state, country=india, zip, city)
        String city = safeString(inputMap.get("city"));
        String state = safeString(inputMap.get("state"));
        String zip = safeString(inputMap.get("zip"));
        Map<String, Object> locationHierarchy = createLocation(state, "india", zip, city);
        output.put("locationHierarchy", locationHierarchy);

        // 8. mobile = phone (direct)
        output.put("mobile", inputMap.get("phone"));

        // 9. address = address (direct)
        output.put("address", inputMap.get("address"));

        // 10. extendedAttributes = toMap(tenantcode)
        output.put("extendedAttributes", toMap(inputMap.get("tenantcode")));

        return output;
    }

    // setActiveFunctionkbpl logic: "1" -> "active", else "inactive"
    private String setActiveFunctionkbpl(Object isActive) {
        return (isActive != null && isActive.toString().equals("1")) ? "active" : "inactive";
    }

    // concatLoginid concatenates salesmancode and tenantcode with "-" between them
    private String concatLoginid(String param1, String param2) {
        if (param1 == null) param1 = "";
        if (param2 == null) param2 = "";
        return param1 + "-" + param2;
    }

    // convertToString converts input param to String safely
    private String convertToString(Object param) {
        return param == null ? null : param.toString();
    }

    // createLocation builds location map using provided parameters, defaults country to "india"
    private Map<String, Object> createLocation(String state, String country, String zip, String city) {
        Map<String, Object> location = new HashMap<>();
        if (isNotUndefinedOrEmpty(state)) location.put("state", state);
        if (isNotUndefinedOrEmpty(country)) location.put("country", country);
        else location.put("country", "india");
        if (isNotUndefinedOrEmpty(zip)) location.put("pincode", zip);
        if (isNotUndefinedOrEmpty(city)) location.put("city", city);
        return location;
    }

    // Helper to check non-null and not-empty strings
    private boolean isNotUndefinedOrEmpty(String param) {
        return param != null && !param.trim().isEmpty() && !"undefined".equalsIgnoreCase(param);
    }

    // toConstant returns constant string "mgr"
    private String toConstant() {
        return "mgr";
    }

    // toMap constructs a map with tenantcode key and its value
    private Map<String, Object> toMap(Object tenantcode) {
        Map<String, Object> map = new HashMap<>();
        map.put("tenantcode", tenantcode);
        return map;
    }

    // Helper to safely convert object to string or null if blank
    private static String safeString(Object obj) {
        if (obj == null) return null;
        String s = obj.toString().trim();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }
}