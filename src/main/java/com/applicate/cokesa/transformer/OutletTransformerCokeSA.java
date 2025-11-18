package com.applicate.cokesa.transformer;

import com.salescode.dim.etl.transformation.service.DataTransformationService.TransformationException;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutletTransformerCokeSA extends AbstractTransformer<Map<String,Object>,List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createResponse(inputMap));
        return responseList;
    }

    private Map<String, Object> createResponse(Map <String, Object> inputMap){
        Map<String, Object> response = new HashMap<>();
        if(NullUtils.isNull(inputMap.get("OM01_OUTNUM"))) throw new TransformationException("OutletCode cannot be null");
        response.put("outletcode", inputMap.get("OM01_OUTNUM").toString().replaceAll("\\.0$", ""));
        response.put("outletName", inputMap.get("OM01_ADRLIN1").toString());
        response.put("activeStatus", (NullUtils.isNull(inputMap.get("OM01_SPRCOD")) || !inputMap.get("OM01_SPRCOD").equals("S")) ? ActiveStatus.ACTIVE : ActiveStatus.INACTIVE);
        response.put("contactno", NullUtils.isNotNull(inputMap.get("OM01_TELNUM2")) ? inputMap.get("OM01_TELNUM2").toString() : "0000000000");
        response.put("channel", NullUtils.isNotNull(inputMap.get("OM01_TRDCHN")) ? inputMap.get("OM01_TRDCHN").toString() + "-OM01_TRDCHN" : null);
        response.put("outletClass", NullUtils.isNotNull(inputMap.get("OM01_GRADE")) ? inputMap.get("OM01_GRADE").toString() + "-OM01_GRADE" : null);
        response.put("distributionChannel", NullUtils.isNotNull(inputMap.get("OM01_SUBTRDCHN")) ? inputMap.get("OM01_SUBTRDCHN").toString() + "-OM01_SUBTRDCHN" : null);
        response.put(
                "latitude",
                NullUtils.isNotNull(inputMap.get("OM01_LATTUD"))
                        ? (BigDecimal) inputMap.get("OM01_LATTUD")
                        : null
        );

        response.put(
                "longitude",
                NullUtils.isNotNull(inputMap.get("OM01_LNGTUD"))
                        ? (BigDecimal) inputMap.get("OM01_LNGTUD")
                        : null
        );
        response.put("email", toStringOrNull(inputMap.get("OM01_EMLADR")));
        response.put("priceListId", toStringOrNull(inputMap.get("OM01_PRILST1")));

        response.put(
                "immediateParent",
                inputMap.get("OM01_OUTLOC") != null
                        ? setClientHierarchy(inputMap.get("OM01_OUTLOC").toString())
                        : null
        );

        response.put("address", toStringOrNull(inputMap.get("OM01_ADRLIN2")));
        response.put("prodauthcode", toStringOrNull(inputMap.get("OM01_ATHARTGPL")));

        response.put(
                "outletAttr2",
                inputMap.get("OM01_EXCLUSIVITY") != null
                        ? inputMap.get("OM01_EXCLUSIVITY").toString() + "-OM01_EXCLUSIVITY"
                        : null
        );

        response.put("outletAttr1", toStringOrNull(inputMap.get("OM01_OUTLOC")));

        response.put("locationHierarchy", setClientLocationHierarchy(inputMap));

        response.put("beat", toStringOrNull(inputMap.get("OM01_SALRTE")));
        response.put("extendedAttributes",createExtended(inputMap));

        return response;
    }
    private boolean safetyStore(Map <String, Object> inputMap,String s){
        return inputMap.containsKey(s) && NullUtils.isNotNull(inputMap.get(s));
    }

    private JsonNode createExtended(Map<String, Object> inputMap) {
        Map<String, Object> extended = new HashMap<>();

        String[][] mappings = {
                {"OM01_DELLOC", "warehouseLoc"},
                {"OM01_ADRLIN4", "postalCode"},
                {"OM01_CNYCOD", "countryCode"},
                {"OM01_SALGRP", "region"},
                {"OM01_TRDGRP", "groupCode"},
                {"OM01_DELRTE", "deliveryRoute"},
                {"OM01_CRDLIMHDL", "creditLimitCheck"},
                {"OM01_DELDLYCOD", "deliveryDate"},
                {"OM01_POSTALCODE", "postalCode"},
                {"OM01_CRNO", "crNumber"},
                {"OM01_VATNO", "vatNumber"},
                {"OM01_BUILDINGNO", "buildingNumber"}
        };

        for (String[] mapping : mappings) {
            String sourceKey = mapping[0];
            String targetKey = mapping[1];

            if (safetyStore(inputMap, sourceKey)) {
                extended.put(targetKey, inputMap.get(sourceKey).toString());
            }
        }

        if (safetyStore(inputMap, "OM01_ARNAME")) {
            Map<String, String> dsMap = new HashMap<>();
            dsMap.put("ar", inputMap.get("OM01_ARNAME").toString());
            extended.put("ds", dsMap);
        }

        if (safetyStore(inputMap, "OM01_ARADDRESS")) {
            Map<String, String> dsMap = new HashMap<>();
            dsMap.put("ar", inputMap.get("OM01_ARADDRESS").toString());
            extended.put("ad", dsMap);
        }

        if (safetyStore(inputMap, "OM01_DETCOD")) {
            String detCod = inputMap.get("OM01_DETCOD").toString().trim();
            String invoiceType;

            switch (detCod) {
                case "113":
                    invoiceType = "Credit AR";
                    break;
                case "111":
                    invoiceType = "Cash";
                    break;
                case "112":
                    invoiceType = "Credit TCS";
                    break;
                default:
                    invoiceType = "NA";
            }

            extended.put("invoiceType", invoiceType);
        }

        return JSONUtils.toJsonNode(extended);
    }


    private Map<String, Object> setClientHierarchy(String parentDepot){
        if(NullUtils.isNull(parentDepot) || parentDepot.isEmpty()){
            throw new TransformationException("Depot provided for the outlet is either null or empty, unable to set immediate parent");
        }
        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put("immediateParent", parentDepot);
        return hierarchy;
    }

    private String setClientLocationHierarchy(Map<String, Object> inputMap) {
        String area = null;
        String city = null;

        if (NullUtils.isNotNull(inputMap.get("OM01_ADRLIN3"))) {
            List<String> districtCitySplit = splitDistrictCity(inputMap.get("OM01_ADRLIN3").toString());

            if (!districtCitySplit.isEmpty() && NullUtils.isNotNull(districtCitySplit.get(0))) {
                area = districtCitySplit.get(0);
            }

            if (districtCitySplit.size() > 1 && NullUtils.isNotNull(districtCitySplit.get(1))) {
                city = districtCitySplit.get(1);
            }
        }

        String country = (NullUtils.isNull(inputMap.get("OM01_COUCOD"))
                || inputMap.get("OM01_COUCOD").toString().isEmpty())
                ? "KSA"
                : inputMap.get("OM01_COUCOD").toString();

        // Build a plain location string like "Area > City > Country"
        StringBuilder location = new StringBuilder();

        if (area != null && !area.isEmpty()) {
            location.append(area.trim());
        }

        if (city != null && !city.isEmpty()) {
            if (location.length() > 0) location.append(" > ");
            location.append(city.trim());
        }

        if (country != null && !country.isEmpty()) {
            if (location.length() > 0) location.append(" > ");
            location.append(country.trim());
        }

        // Return fallback if everything was null
        return location.length() > 0 ? location.toString() : "KSA";
    }

    private List<String> splitDistrictCity(String districtCity){
        if(districtCity.isEmpty()) return new ArrayList<>();
        String[] parts = districtCity.split(" {2,}", 2);
        String district = parts[0].trim();
        String city = parts.length > 1 ? parts[1].trim() : "";
        ArrayList<String> result = new ArrayList<>(2);
        result.add(district);
        result.add(city);
        return result;
    }

    private String toStringOrNull(Object value) {
        return value != null ? value.toString() : null;
    }

}
