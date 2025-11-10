package com.applicate.lbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.GenericEntity;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LbplPricingTransformerNew extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    public static final String PRICEPLANDETAIL = "priceplandetail";
    public static final String ITEMCODE = "itemcode";

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        String pricingCode = String.valueOf(inputMap.get("pricingcode"));
        String name = "PricingPlan";
        String id = pricingCode + "-" + name;

        JsonNode priceplandetails = JSONUtils.convert(
                inputMap.get("priceplandetails"),
                new TypeReference<List<Map<String, String>>>() {}
        );

        JsonNode pricePlanDetail = priceplandetails.get(PRICEPLANDETAIL);

        GenericEntity pricePlanObject = new GenericEntity();
        pricePlanObject.setName(name);
        pricePlanObject.setId(id);
        pricePlanObject.setKey1(pricingCode);

        ObjectNode payloadObject = JSONUtils.getObjectMapper().createObjectNode();
        payloadObject.set("val", buildPricingPayload(pricePlanDetail, priceplandetails));

        pricePlanObject.setPayload(payloadObject);

        // ✅ Explicit manual conversion to Map<String, Object>
        Map<String, Object> entityMap = new HashMap<>();
        entityMap.put("id", pricePlanObject.getId());
        entityMap.put("name", pricePlanObject.getName());
        entityMap.put("key1", pricePlanObject.getKey1());
        entityMap.put("payload", pricePlanObject.getPayload());

        return Collections.singletonList(entityMap);
    }

    private static ObjectNode buildPricingPayload(JsonNode pricePlanDetail, JsonNode priceplandetails) {

        ObjectNode payload = JSONUtils.getObjectMapper().createObjectNode();

        if (pricePlanDetail.isArray()) {

            pricePlanDetail.forEach(obj -> {
                ObjectNode skuNode = JSONUtils.getObjectMapper().createObjectNode();

                skuNode.put("mrp", obj.path("mrp").asText());
                skuNode.put("uom", obj.path("baseuom").asText());
                skuNode.put("p", obj.path("salesprice").asDouble());
                skuNode.put("bpc", obj.path("conversion1").asText());
                skuNode.put("IsActive", obj.path("activeindicator").asText());
                skuNode.put(ITEMCODE, obj.path(ITEMCODE).asText());
                skuNode.put("c", obj.path("salespriceinbaseuom").asText());

                payload.set(obj.path(ITEMCODE).asText(), skuNode);
            });

            return payload;
        }

        // Single object case
        JsonNode obj = priceplandetails.get(PRICEPLANDETAIL);
        ObjectNode skuNode = JSONUtils.getObjectMapper().createObjectNode();

        skuNode.put("mrp", obj.path("mrp").asText());
        skuNode.put("uom", obj.path("baseuom").asText());
        skuNode.put("p", obj.path("salesprice").asText());
        skuNode.put("bpc", obj.path("conversion1").asText());
        skuNode.put("IsActive", obj.path("activeindicator").asText());
        skuNode.put(ITEMCODE, obj.path(ITEMCODE).asText());
        skuNode.put("c", obj.path("salespriceinbaseuom").asText());

        payload.set(obj.path(ITEMCODE).asText(), skuNode);
        return payload;
    }


}
