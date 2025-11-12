package com.applicate.lbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.*;

public class LBPLOutletDetailTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        List<Map<String, Object>> outputList = new ArrayList<>();
        Map<String, Object> output = new HashMap<>();

        // 1. contactName = contactperson
        output.put("contactName", safeString(inputMap.get("contactperson")));

        // 2. latitude = geocodey
        output.put("latitude", safeString(inputMap.get("geocodey")));

        // 3. longitude = geocodex
        output.put("longitude", safeString(inputMap.get("geocodex")));

        // 4. outletType = categorycode1
        output.put("outletType", safeString(inputMap.get("categorycode1")));

        // 5. location = createLocation1(state, categorycode2, zip, city)
        String state = safeString(inputMap.get("state"));
        String categorycode2 = safeString(inputMap.get("categorycode2"));
        String zip = safeString(inputMap.get("zip"));
        String city = safeString(inputMap.get("city"));
        Map<String, Object> location = createLocation1(state, categorycode2, zip, city);
        output.put("location", location);

        // 6. marketId = categorycode2
        output.put("marketId", safeString(inputMap.get("categorycode2")));

        // 7. subTerritory = territoryhierarchy
        output.put("subTerritory", safeString(inputMap.get("territoryhierarchy")));

        // 8. marketName = categorycode3
        output.put("marketName", safeString(inputMap.get("categorycode3")));

        // 9. channel = categorycode4
        output.put("channel", safeString(inputMap.get("categorycode4")));

        // 10. subChannel = categorycode5
        output.put("subChannel", safeString(inputMap.get("categorycode5")));

        // 11. outletCategory = categorycode6
        output.put("outletCategory", safeString(inputMap.get("categorycode6")));

        // 12. outletClass = categorycode7
        output.put("outletClass", safeString(inputMap.get("categorycode7")));

        // 13. distributionChannel = categorycode8
        output.put("distributionChannel", safeString(inputMap.get("categorycode8")));

        // 14. outletDivision = categorycode9
        output.put("outletDivision", safeString(inputMap.get("categorycode9")));

        // 15. account = categorycode10
        output.put("account", safeString(inputMap.get("categorycode10")));

        // 16. immediateParent = createParent(tenantcode)
        String tenantcode = safeString(inputMap.get("tenantcode"));
        List<Map<String, Object>> immediateParent = createParent(tenantcode);
        output.put("immediateParent", immediateParent);

        // 17. outletName = customername
        output.put("outletName", safeString(inputMap.get("customername")));

        // 18. email = email
        output.put("email", safeString(inputMap.get("email")));

        // 19. address = address1
        output.put("address", safeString(inputMap.get("address1")));

        // 20. activeStatus = ActiveFunction(customerstatus)
        Object customerstatus = inputMap.get("customerstatus");
        output.put("activeStatus", activeFunction(customerstatus));

        // 21. contactno = countrycodecheck(phone)
        String phone = safeString(inputMap.get("phone"));
        output.put("contactno", countrycodecheck(phone));

        // 22. outletCode = customercode
        output.put("outletCode", safeString(inputMap.get("customercode")));

        // 23. priceListId = customerpricingkey
        output.put("priceListId", safeString(inputMap.get("customerpricingkey")));

        // 24. extendedAttributes = toMap(...)
        Map<String, Object> extendedAttributes = new HashMap<>();
        String[] keys = {
                "istaxable", "totalbalancedue", "notes", "officeaccount", "tinnumber", "surveykey",
                "fax", "territoryhierarchy", "customernamea", "dateofbirth", "defaultpriority",
                "address3", "address2", "customerpricelevel", "totalcreditlimit", "locationcode",
                "hierarchycode", "barcodecheckdigit", "salesmode", "paymenttype"
        };

        for (String key : keys) {
            extendedAttributes.put(key, inputMap.get(key));
        }

        output.put("extendedAttributes", extendedAttributes);

        outputList.add(output);
        return outputList;
    }

    // ---------------- Helper Functions ----------------

    private static String safeString(Object obj) {
        if (obj == null) return null;
        String str = obj.toString().trim();
        return ("null".equalsIgnoreCase(str) || str.isEmpty()) ? null : str;
    }

    // Equivalent to JS createLocation1()
    private static Map<String, Object> createLocation1(String state, String categorycode2, String zip, String city) {
        Map<String, Object> location = new HashMap<>();
        location.put("state", state);
        location.put("areacode", categorycode2);
        location.put("pincode", zip);
        location.put("city", city);

        if ((state == null || state.isEmpty()) &&
                (zip == null || zip.isEmpty()) &&
                (city == null || city.isEmpty())) {
            location.put("country", "India");
        }

        return location;
    }

    // Equivalent to JS createParent()
    private static List<Map<String, Object>> createParent(String param) {
        if (param == null || param.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Object> parent = new HashMap<>();
        parent.put("immediateParent", param);
        return Collections.singletonList(parent);
    }

    // Equivalent to JS ActiveFunction()
    private static String activeFunction(Object param) {
        if (param != null && "1".equals(param.toString())) {
            return "active";
        }
        return "inactive";
    }

    // Equivalent to JS countrycodecheck()
    private static String countrycodecheck(String mobile) {
        if (mobile == null) return null;
        String strMobile = mobile.trim();
        if (strMobile.length() > 10 && strMobile.startsWith("91")) {
            strMobile = strMobile.substring(2);
        }
        return strMobile;
    }
}
