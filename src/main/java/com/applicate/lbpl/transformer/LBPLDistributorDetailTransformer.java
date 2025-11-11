package com.applicate.lbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.*;

public class LBPLDistributorDetailTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        List<Map<String, Object>> outputList = new ArrayList<>();

            Map<String, Object> output = new HashMap<>();

            // 1. userAccountId and loginId = distributorcode
            output.put("userAccountId", safeString(inputMap.get("distributorcode")));
            output.put("loginid", safeString(inputMap.get("distributorcode")));

            // 2. name = distributorname
            output.put("name", safeString(inputMap.get("distributorname")));

            // 3. activeStatus = ActiveFunction
            Object isactive = inputMap.get("isactive");
            output.put("activeStatus", (isactive != null && isactive.toString().equals("1")) ? "active" : "inactive");

            // 4. email
            output.put("email", safeString(inputMap.get("email")));

            // 5. mobile = countrycodecheck(mobile)
            String mobile = safeString(inputMap.get("mobile"));
            output.put("mobile", normalizeMobile(mobile));

            // 6. locationHierarchy = createLocationLbpl(zip)
            String zip = safeString(inputMap.get("zip"));
            Map<String, Object> locationHierarchy = new HashMap<>();
            locationHierarchy.put("pincode", zip);
            locationHierarchy.put("country", "India");
            output.put("locationHierarchy", locationHierarchy);

            // 7. address
            output.put("address", safeString(inputMap.get("address")));

            // 8. designation = constant "supplier"
            output.put("designation", "supplier");

            // 9. extendedAttributes = toMap(categorycode1, phone, tenantcode, categorycode4)
            Map<String, Object> ext = new HashMap<>();
            ext.put("categorycode1", inputMap.get("categorycode1"));
            ext.put("phone", inputMap.get("phone"));
            ext.put("tenantcode", inputMap.get("tenantcode"));
            ext.put("categorycode4", inputMap.get("categorycode4"));
            output.put("extendedAttributes", ext);

            outputList.add(output);
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