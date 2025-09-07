package com.applicate.unnati.transformer;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class OutletDetailsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> outletMap = new LinkedHashMap<>();

        // Identifiers
        outletMap.put("id", inputMap.get("id"));
        outletMap.put("code", inputMap.get("code"));
        outletMap.put("outletName", inputMap.get("outletName"));

        // Location hierarchy
        outletMap.put("address", inputMap.get("address"));
        outletMap.put("branchName", inputMap.get("branchName"));
        outletMap.put("branchCode", inputMap.get("branchCode"));
        outletMap.put("townName", inputMap.get("townName"));
        outletMap.put("townCode", inputMap.get("townCode"));
        outletMap.put("cityName", inputMap.get("cityName"));
        outletMap.put("cityCode", inputMap.get("cityCode"));
        outletMap.put("stateName", inputMap.get("stateName"));
        outletMap.put("stateCode", inputMap.get("stateCode"));
        outletMap.put("zoneName", inputMap.get("zoneName"));
        outletMap.put("zoneCode", inputMap.get("zoneCode"));
        outletMap.put("distributorCode", inputMap.get("distributorCode"));
        outletMap.put("distributorName", inputMap.get("distributorName"));
        outletMap.put("routeCode", inputMap.get("routeCode"));
        outletMap.put("routeName", inputMap.get("routeName"));
        outletMap.put("outletTypeCode", inputMap.get("outletTypeCode"));
        outletMap.put("outletTypeName", inputMap.get("outletTypeName"));
        outletMap.put("classCode", inputMap.get("classCode"));
        outletMap.put("className", inputMap.get("className"));
        outletMap.put("lob", inputMap.get("lob"));
        outletMap.put("extendedDataSource", inputMap.get("extendedDataSource"));
        outletMap.put("activeStatus", inputMap.get("activeStatus"));
        outletMap.put("coordinate", inputMap.get("coordinate"));
        outletMap.put("createdBy", inputMap.get("createdBy"));
        outletMap.put("updatedBy", inputMap.get("updatedBy"));


        if (inputMap.get("averageSales") != null) {
            outletMap.put("averageSales", new BigDecimal(inputMap.get("averageSales").toString()));
        } else {
            outletMap.put("averageSales", null);
        }


        if (inputMap.get("extendedAttributes") != null) {
            try {
                JsonNode node = objectMapper.readTree(inputMap.get("extendedAttributes").toString());
                outletMap.put("extendedAttributes", node);
            } catch (Exception e) {
                outletMap.put("extendedAttributes", null);
            }
        } else {
            outletMap.put("extendedAttributes", null);
        }



        if (inputMap.get("createdDate") != null) {
            outletMap.put("createdDate", LocalDateTime.parse(inputMap.get("createdDate").toString()));
        } else {
            outletMap.put("createdDate", null);
        }

        if (inputMap.get("updatedDate") != null) {
            outletMap.put("updatedDate", LocalDateTime.parse(inputMap.get("updatedDate").toString()));
        } else {
            outletMap.put("updatedDate", null);
        }

        return outletMap;
    }
}
