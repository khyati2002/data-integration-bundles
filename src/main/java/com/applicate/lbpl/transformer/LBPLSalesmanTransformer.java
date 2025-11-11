package com.applicate.lbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.*;

public class LBPLSalesmanTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        List<Map<String, Object>> outputList = new ArrayList<>();


            Map<String, Object> output = new HashMap<>();

            // 1. immediateParent = convertToString(tenantcode)
            Object tenantcode = inputMap.get("tenantcode");
            output.put("immediateParent", tenantcode != null ? tenantcode.toString() : null);

            // 2. address = addressFunction(address,state,city,zip)
            String address = safeString(inputMap.get("address"));
            String state = safeString(inputMap.get("state"));
            String city = safeString(inputMap.get("city"));
            String zip = safeString(inputMap.get("zip"));

            if (address != null) {
                output.put("address", address);
            } else if (isNullOrEmpty(state) && isNullOrEmpty(city) && isNullOrEmpty(zip)) {
                output.put("address", "India");
            } else {
                output.put("address", state + " " + city + " " + zip);
            }

            // 3. activeStatus = ActiveFunction(isactive)
            Object isActive = inputMap.get("isactive");
            output.put("activeStatus", (isActive != null && isActive.toString().equals("1")) ? "active" : "inactive");

            // 4. mobile = phone
            output.put("mobile", inputMap.get("phone"));

            // 5. name = salesmanname
            output.put("name", safeString(inputMap.get("salesmanname")));

            // 6. userAccountId = concatLoginid(tenantcode, salesmancode)
            String salesmancode = safeString(inputMap.get("salesmancode"));
            output.put("userAccountId", tenantcode + "-" + salesmancode);

            // 7. loginId = same concat
            output.put("loginId", tenantcode + "-" + salesmancode);

            // 8. locationHierarchy = createLocationSLMG
            Map<String, Object> locationHierarchy = new HashMap<>();
            locationHierarchy.put("state", state);
            locationHierarchy.put("country", "India");
            locationHierarchy.put("pincode", zip);
            locationHierarchy.put("city", city);
            output.put("locationHierarchy", locationHierarchy);

            // 9. designation = constant "mgr"
            output.put("designation", "mgr");

            // 10. extendedAttributes = toMap (locationcode)
            Object locationcode = inputMap.get("locationcode");
            Map<String, Object> extendedAttrs = new HashMap<>();
            extendedAttrs.put("locationcode", locationcode);
            output.put("extendedAttributes", extendedAttrs);

            outputList.add(output);
            
        return outputList;
    }

    private static String safeString(Object obj) {
        if (obj == null) return null;
        String s = obj.toString();
        return s.equalsIgnoreCase("null") || s.trim().isEmpty() ? null : s;
    }

    private static boolean isNullOrEmpty(String str) {
        return (str == null || str.trim().isEmpty() || str.equalsIgnoreCase("null"));
    }


}