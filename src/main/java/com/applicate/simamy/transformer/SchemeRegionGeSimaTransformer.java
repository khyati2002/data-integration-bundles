//package com.applicate.simamy.transformer;
//
//import com.applicate.services.channelkart.exceptions.TransformationException;
//import com.applicate.services.channelkart.transformers.AbstractTransformer;
//
//import java.util.*;
//
//public class SchemeRegionGeSimaTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
//    private static final String TRO_CODE = "trocode";
//    private static final String SALES_GROUP = "salesgroup";
//    private static final String RUN_ON_CHANNEL = "runonchannel";
//    private static final String PM_CENTRAL = "PM-Central";
//    private static final String PM_SOUTH = "PM-South";
//    private static final String PM_NORTH = "PM-North";
//    private static final String PM_EAST_COAST = "PM-East Coast";
//    private static final String EM_SABAH = "EM-Sabah";
//    private static final String EM_SARAWAK = "EM-Sarawak";
//
//    @Override
//    public Map<String, Object> transform(Map<String, Object> schemeRegion) {
//        List<String> validationResponses = validateSchemeRegion(schemeRegion);
//
//        if (!validationResponses.isEmpty()) {
//            throw new TransformationException("Transformation Exception Occurred : ", String.valueOf(validationResponses));
//        }
//
//        Map<String, Object> respMap = new HashMap<>();
//        respMap.put("name", "SchemeRegion");
//        respMap.put("key1", schemeRegion.get(TRO_CODE));
//        respMap.put("key2", schemeRegion.get(SALES_GROUP));
//        respMap.put("key3", schemeRegion.get(RUN_ON_CHANNEL));
//
//        List<String> regions = Arrays.asList(PM_CENTRAL, PM_SOUTH, PM_NORTH, PM_EAST_COAST, EM_SABAH, EM_SARAWAK);
//
//        for (int i = 0; i < regions.size(); i++) {
//            String region = regions.get(i);
//            Object value = schemeRegion.get(region);
//            String key = "key" + (4 + i); // key4, key5, etc.
//
//            if (value != null && "YES".equalsIgnoreCase(value.toString())) {
//                respMap.put(key, region);
//            } else {
//                respMap.put(key, "no");
//            }
//        }
//
//        respMap.put("id", schemeRegion.get(TRO_CODE) + "_" + schemeRegion.get(SALES_GROUP) + "_SchemeRegion");
//
//        return respMap;
//    }
//
//
//    public List<String> validateSchemeRegion(Map<String, Object> schemeRegion) {
//        List<String> errors = new ArrayList<>();
//        String troCode = getValue(schemeRegion.get(TRO_CODE));
//        String salesGroup = getValue(schemeRegion.get(SALES_GROUP));
//        String runOnChannel = getValue(schemeRegion.get(RUN_ON_CHANNEL));
//
//        if (troCode != null && troCode.isEmpty()) {
//            errors.add("TRO CODE is null or empty");
//        } else if (!troCode.matches("^[24]\\d{3}$")) {
//            // TRO Code must be 4 digits and start with 2 or 4
//            errors.add("INVALID TRO CODE: must be 4 digits and start with 2xxx or 4xxx");
//        }
//
//        if (salesGroup == null || salesGroup.isEmpty()) {
//            errors.add("SALES GROUP is null or empty");
//        }
//
//        if (runOnChannel == null || runOnChannel.isEmpty()) {
//            errors.add("RUN ON CHANNEL is null or empty");
//        }
//
//        return errors;
//    }
//
//    private String getValue(Object obj) {
//        return obj == null ? null : obj.toString().trim().toLowerCase();
//    }
//}
