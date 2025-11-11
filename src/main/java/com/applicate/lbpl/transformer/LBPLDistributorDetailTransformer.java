package com.applicate.lbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.*;

public class LBPLDistributorDetailTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        List<Map<String, Object>> outputList = new ArrayList<>();

        List<Map<String, Object>> features = (List<Map<String, Object>>) inputMap.get("features");
        if (features == null) return outputList;

        for (Map<String, Object> feature : features) {

            Map<String, Object> output = new HashMap<>();

            // 1. userAccountId and loginId = distributorcode
            output.put("userAccountId", safeString(feature.get("distributorcode")));
            output.put("loginId", safeString(feature.get("distributorcode")));

            // 2. name = distributorname
            output.put("name", safeString(feature.get("distributorname")));

            // 3. activeStatus = ActiveFunction
            Object isactive = feature.get("isactive");
            output.put("activeStatus", (isactive != null && isactive.toString().equals("1")) ? "active" : "inactive");

            // 4. email
            output.put("email", safeString(feature.get("email")));

            // 5. mobile = countrycodecheck(mobile)
            String mobile = safeString(feature.get("mobile"));
            output.put("mobile", normalizeMobile(mobile));

            // 6. locationHierarchy = createLocationLbpl(zip)
            String zip = safeString(feature.get("zip"));
            Map<String, Object> locationHierarchy = new HashMap<>();
            locationHierarchy.put("pincode", zip);
            locationHierarchy.put("country", "India");
            output.put("locationHierarchy", locationHierarchy);

            // 7. address
            output.put("address", safeString(feature.get("address")));

            // 8. designation = constant "supplier"
            output.put("designation", "supplier");

            // 9. extendedAttributes = toMap(categorycode1, phone, tenantcode, categorycode4)
            Map<String, Object> ext = new HashMap<>();
            ext.put("categorycode1", feature.get("categorycode1"));
            ext.put("phone", feature.get("phone"));
            ext.put("tenantcode", feature.get("tenantcode"));
            ext.put("categorycode4", feature.get("categorycode4"));
            output.put("extendedAttributes", ext);

            outputList.add(output);
        }

        return outputList;
    }

    private static String safeString(Object obj) {
        if (obj == null) return null;
        String str = obj.toString();
        return ("null".equalsIgnoreCase(str) || str.trim().isEmpty()) ? null : str;
    }

    // Same logic as "countrycodecheck" JS function
    private static String normalizeMobile(String mobile) {
        if (mobile == null) return null;

        String strMobile = mobile.trim();

        if (strMobile.length() > 10 && strMobile.startsWith("91")) {
            return strMobile.substring(2);
        }

        return strMobile;
    }

}