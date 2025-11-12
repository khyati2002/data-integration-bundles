package com.applicate.kbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

public class OutletMasterKGPLTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    public static final String KGPL = "-KGPL";
    public static final String STATE = "state";

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("source", "KGPL");

        ifEmpty(inputMap.get("customercode")).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("customercode cannot be empty"); // outletcode
        });

        ifEmpty(inputMap.get("tenantcode")).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("tenantcode cannot be empty"); // outletcode
        });

        String tenantcode = "tenantcode";
        String customerpricingkey = "customerpricingkey";
        String email = "email";
        String territoryhierarchy = "territoryhierarchy";
        ifEmpty(inputMap.get(tenantcode)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("tenantcode cannot be empty");
        });

        appendKGPL(inputMap, responseMap, "categorycode3", "marketName");
        appendKGPL(inputMap, responseMap, "categorycode4", "channel");
        appendKGPL(inputMap, responseMap, "categorycode5", "subChannel");
        appendKGPL(inputMap, responseMap, "categorycode7", "outletClass");
        appendKGPL(inputMap, responseMap, "categorycode8", "distributionChannel");
        appendKGPL(inputMap, responseMap, "categorycode1", "outletType");
        appendKGPL(inputMap, responseMap, "categorycode2", "marketId");

        appendKGPL(inputMap, responseMap, customerpricingkey, "priceListId");
        appendKGPL(inputMap, responseMap, "customercode", "outletCode");

        ifEmpty(inputMap.get("contactperson")).ifPresent(val -> responseMap.put("contactName", inputMap.get("contactperson").toString()));
        ifEmpty(inputMap.get("geocodey")).ifPresent(val -> responseMap.put("latitude", inputMap.get("geocodey").toString()));
        ifEmpty(inputMap.get("geocodex")).ifPresent(val -> responseMap.put("longitude", inputMap.get("geocodex").toString()));
        ifEmpty(inputMap.get("customername")).ifPresent(val -> responseMap.put("outletName", inputMap.get("customername").toString()));
        ifEmpty(inputMap.get(email)).ifPresent(val -> responseMap.put(email, inputMap.get(email).toString()));
        ifEmpty(inputMap.get("address1")).ifPresent(val -> responseMap.put("address", inputMap.get("address1").toString()));
        ifEmpty(inputMap.get("customerstatus")).ifPresent(val -> responseMap.put("activeStatus", inputMap.get("customerstatus").toString()));
        ifEmpty(inputMap.get("phone")).ifPresentOrElse(val -> responseMap.put("contactno", formatPhoneNumber(inputMap.get("phone").toString())), () -> {
            responseMap.put("contactno", "0000000000");
        });
        ifEmpty(inputMap.get(territoryhierarchy)).ifPresent(val -> responseMap.put("subTerritory", inputMap.get(territoryhierarchy).toString()));

        String city = inputMap.get("city") == null ? "" : inputMap.get("city").toString();
        String state = inputMap.get(STATE) == null ? "" : inputMap.get(STATE).toString();
        String zip = inputMap.get("zip") == null ? "" : inputMap.get("zip").toString();
        responseMap.put("location", createLocation(state,"India", zip, city));

        // Add displayAddress with city and state
        responseMap.put("displayAddress", createDisplayAddress(city, state));

        String supplierId = inputMap.get(tenantcode).toString() + KGPL;
        final String IMM_PARENT = "immediateParent";
        Map<Object, Object> immediateParent1 = Map.of(IMM_PARENT, supplierId);
        List<Map<Object, Object>> parentList = new ArrayList<>();
        parentList.add(immediateParent1);
        responseMap.put(IMM_PARENT, parentList);

        ObjectNode extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();

        extendedAttributes.put(tenantcode, inputMap.get(tenantcode).toString() + KGPL);
        ifEmpty(inputMap.get("istaxable")).ifPresent(val -> extendedAttributes.put("istaxable", val));
        ifEmpty(inputMap.get("totalbalancedue")).ifPresent(val -> extendedAttributes.put("totalbalancedue", val));
        ifEmpty(inputMap.get("notes")).ifPresent(val -> extendedAttributes.put("notes", val));
        ifEmpty(inputMap.get("officeaccount")).ifPresent(val -> extendedAttributes.put("officeaccount", val));
        ifEmpty(inputMap.get("fax")).ifPresent(val -> extendedAttributes.put("fax", val));
        ifEmpty(inputMap.get(territoryhierarchy)).ifPresent(val -> extendedAttributes.put(territoryhierarchy, val));
        ifEmpty(inputMap.get("customernamea")).ifPresent(val -> extendedAttributes.put("customernamea", val));
        ifEmpty(inputMap.get("dateofbirth")).ifPresent(val -> extendedAttributes.put("dateofbirth", val));
        ifEmpty(inputMap.get("defaultpriority")).ifPresent(val -> extendedAttributes.put("defaultpriority", val));
        ifEmpty(inputMap.get("address3")).ifPresent(val -> extendedAttributes.put("address3", val));
        ifEmpty(inputMap.get("address2")).ifPresent(val -> extendedAttributes.put("address2", val));
        ifEmpty(inputMap.get("totalcreditlimit")).ifPresent(val -> extendedAttributes.put("totalcreditlimit", val));
        ifEmpty(inputMap.get("barcodecheckdigit")).ifPresent(val -> extendedAttributes.put("barcodecheckdigit", val));
        ifEmpty(inputMap.get(customerpricingkey)).ifPresent(val -> extendedAttributes.put(customerpricingkey, val));
        ifEmpty(inputMap.get("salesmode")).ifPresent(val -> extendedAttributes.put("salesmode", val));
        ifEmpty(inputMap.get("paymenttype")).ifPresent(val -> extendedAttributes.put("paymenttype", val));

        responseMap.put("extendedAttributes", extendedAttributes);

        return responseMap;
    }


    private static boolean isNotUndefinedOrEmpty(String param) {
        return param != null && !param.isEmpty();
    }

    private static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() > 10) {
            // Trim country code from beginning if it exists
            phoneNumber = phoneNumber.substring(phoneNumber.length() - 10);
        }
        return phoneNumber;
    }

    private static Map<String, Object> createLocation(String state, String country, String zip, String city) {
        Map<String, Object> location = new HashMap<>();

        if (isNotUndefinedOrEmpty(state)) {
            location.put(STATE, state);
        }
        if (isNotUndefinedOrEmpty(country)) {
            location.put("country", country);
        } else {
            location.put("country", "India");
        }
        if (isNotUndefinedOrEmpty(zip)) {
            location.put("pincode", zip);
        }
        if (isNotUndefinedOrEmpty(city)) {
            location.put("city", city);
        }

        return location;
    }

    private static String createDisplayAddress(String city, String state) {
        List<String> addressParts = new ArrayList<>();

        if (isNotUndefinedOrEmpty(city)) {
            addressParts.add(city);
        }
        if (isNotUndefinedOrEmpty(state)) {
            addressParts.add(state);
        }

        return String.join(", ", addressParts);
    }

    private static void appendKGPL(Map<String, Object> inputMap, Map<String, Object> responseMap, String getVal, String putVal) {
        ifEmpty(inputMap.get(getVal)).ifPresentOrElse(val -> responseMap.put(putVal, val + KGPL),
                () -> responseMap.put(putVal, ""));
    }

    private static Optional<String> ifEmpty(Object object) {
        if(ObjectUtils.isEmpty(object))  {
            return Optional.empty();
        } else {
            return Optional.of(object.toString());
        }
    }
}
