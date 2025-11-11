package com.applicate.kbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class SalesmanEnrichTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    public static final String ENRICH = "-ENRICH";
    public static final String TENANTCODE = "tenantcode";
    public static final String STATE = "state";
    public static final String ADDRESS = "address";

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> responseMap = new HashMap<>();

        ifEmpty(inputMap.get(TENANTCODE)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("tenantcode cannot be empty");
        });

        ifEmpty(inputMap.get("salesmancode")).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("salesmancode cannot be empty");
        });

        String salesmanCode = inputMap.get("salesmancode").toString();
        String tenantCode = inputMap.get(TENANTCODE).toString();

        ifEmpty(inputMap.get("isactive")).ifPresent(val -> responseMap.put("activeStatus", setActiveFunctionEnrich(inputMap.get("isactive").toString())));

        StringBuilder loginIdBuilder = new StringBuilder();
        loginIdBuilder.append(salesmanCode).append("-").append(tenantCode).append(ENRICH);
        responseMap.put("loginId", loginIdBuilder.toString());
        responseMap.put("userAccountId", loginIdBuilder.toString());
        responseMap.put("designation", "mgr");
        responseMap.put("source", "ENRICH");

        ifEmpty(inputMap.get("salesmanname")).ifPresent(val -> responseMap.put("name", inputMap.get("salesmanname").toString()));

        ifEmpty(inputMap.get("phone")).ifPresentOrElse(val -> responseMap.put("mobile", formatPhoneNumber(inputMap.get("phone").toString())),
                () -> responseMap.put("mobile", "0000000000"));

        String city = inputMap.get("city") == null ? "" : inputMap.get("city").toString();
        String state = inputMap.get(STATE) == null ? "" : inputMap.get(STATE).toString();
        String zip = inputMap.get("zip") == null ? "" : inputMap.get("zip").toString();
        String address = inputMap.get(ADDRESS) == null ? "" : inputMap.get(ADDRESS).toString();

        responseMap.put("locationHierarchy", createLocation(state, zip, city));
        responseMap.put(ADDRESS, addressFunction(address, state, city, zip));

        ObjectNode extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
        ifEmpty(tenantCode).ifPresent(val -> {
            Map<Object, Object> immediateParent = Map.of("immediateParent", val + ENRICH);
            responseMap.put("immediateParent", immediateParent);
            extendedAttributes.put(TENANTCODE, val + ENRICH);
        });

        responseMap.put("extendedAttributes", extendedAttributes);

        return responseMap;
    }

    private Object setActiveFunctionEnrich(String isActive) {
        if (Objects.equals(isActive, "1")) {
            return "active";
        } else {
            return "inactive";
        }
    }

    private static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber.length() > 10) {
            // Trim country code from beginning if it exists
            phoneNumber = phoneNumber.substring(phoneNumber.length() - 10);
        }
        return phoneNumber;
    }

    public static String addressFunction(String address, String state, String city, String zip) {
        if (address != null && !address.isEmpty() && !"null".equals(address)) {
            return address;
        } else if (("null".equals(state) && "null".equals(city) && "null".equals(zip)) ||
                (state.isEmpty() && city.isEmpty() && zip.isEmpty())) {
            return "India";
        } else {
            return state + " " + city + " " + zip;
        }
    }

    private static Map<String, Object> createLocation(String state, String zip, String city) {
        Map<String, Object> location = new HashMap<>();

        if (isNotUndefinedOrEmpty(state)) {
            location.put(STATE, state);
        }
        location.put("country", "India");
        if (isNotUndefinedOrEmpty(zip)) {
            location.put("pincode", zip);
        }
        if (isNotUndefinedOrEmpty(city)) {
            location.put("city", city);
        }

        return location;
    }

    private static boolean isNotUndefinedOrEmpty(String param) {
        return param != null && !param.isEmpty();
    }

    private static Optional<String> ifEmpty(Object object) {
        if(ObjectUtils.isEmpty(object))  {
            return Optional.empty();
        } else {
            return Optional.of(object.toString());
        }
    }
}
