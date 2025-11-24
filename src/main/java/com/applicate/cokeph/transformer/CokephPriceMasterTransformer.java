package com.applicate.cokeph.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class CokephPriceMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> stringObjectMap) {
        HashMap<String, Object> finalTransformedObj = new HashMap<>();
        Map<String,Object> extended = new HashMap<>();
//        String effectiveDate = stringObjectMap.get("effective_date") != null ? stringObjectMap.get("effective_date").toString() : "";
//        if (effectiveDate == null || effectiveDate.isEmpty()) {
//            effectiveDate = "2021-01-01 00:00:00";
//        } else {
//            effectiveDate = effectiveDate.replace("T", " ");
//        }

        String effectiveDate = stringObjectMap.get("effective_date") != null
                ? stringObjectMap.get("effective_date").toString()
                : null;

        LocalDateTime effDateObj;

        try {
            if (effectiveDate == null || effectiveDate.isEmpty()) {
                effDateObj = LocalDateTime.parse("2021-01-01T00:00:00");
            } else {
                effectiveDate = effectiveDate.replace(" ", "T");
                effDateObj = LocalDateTime.parse(effectiveDate);
            }
        } catch (Exception e) {
            // fallback default
            effDateObj = LocalDateTime.parse("2021-01-01T00:00:00");
        }

        finalTransformedObj.put("fromDate", effDateObj);
        finalTransformedObj.put("mrp",stringObjectMap.get("mrp").toString()) ;
        finalTransformedObj.put("skuCode",stringObjectMap.get("item_code").toString());
        finalTransformedObj.put("batchCode",stringObjectMap.get("item_code").toString());
        finalTransformedObj.put("packPtr",stringObjectMap.get("sales_price").toString());
        finalTransformedObj.put("casePtr",stringObjectMap.get("sales_price_in_base_uom").toString());
        finalTransformedObj.put("priceList",stringObjectMap.get("pricing_code").toString());
        extended.put("base_uom",stringObjectMap.get("base_uom").toString());
        extended.put("tenant_code",stringObjectMap.get("tenant_code").toString());
        finalTransformedObj.put("extendedAttributes", extended);
        finalTransformedObj.put("source", "default");

        return finalTransformedObj;
    }
}
