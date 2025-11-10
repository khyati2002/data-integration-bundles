package com.applicate.kbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.impl.GenericEntity;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class KGPLPricingTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
    public static final String PRICEPLANDETAIL = "priceplandetail";
    public static final String KGPL = "-KGPL";

    public static final String ITEMCODE = "itemcode";

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        GenericEntity pricePlanObject = new GenericEntity();
        pricePlanObject.setName("PricingPlan");

        ifEmpty(inputMap.get("pricingcode")).ifPresentOrElse(val -> {
            pricePlanObject.setKey1(val + KGPL);
            pricePlanObject.setId(val + "-KGPL-PricingPlan");
        }, () -> {
            throw new DataTransformationService.TransformationException("pricingcode cannot be empty");
        });

        ifEmpty(inputMap.get("priceplandetails")).ifPresentOrElse(val -> {
            JsonNode priceplandetails = JSONUtils.getObjectMapper().valueToTree(inputMap.get("priceplandetails"));
            JsonNode pricePlanDetail = priceplandetails.get(PRICEPLANDETAIL);
            org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode pricingObject = JSONUtils.getObjectMapper().createObjectNode();
            pricingObject.set("val", getPricingPayload(pricePlanDetail, priceplandetails));
            pricePlanObject.setPayload(pricingObject);

        }, () -> {
            throw new DataTransformationService.TransformationException("priceplandetails can not be empty");
        });

        Map<String, Object> entityMap = JSONUtils.getObjectMapper()
                .convertValue(pricePlanObject, new TypeReference<Map<String, Object>>() {});
        return List.of(entityMap);

    }

    private static Optional<String> ifEmpty(Object object) {
        if(ObjectUtils.isEmpty(object))  {
            return Optional.empty();
        } else {
            return Optional.of(object.toString());
        }
    }

    private static org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode getPricingPayload(JsonNode pricePlanDetail, JsonNode priceplandetails) {
        if (pricePlanDetail.isArray()) {
            org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode payload = JSONUtils.getObjectMapper().createObjectNode();
            pricePlanDetail.forEach(obj -> {
                ObjectNode skuNode = JSONUtils.getObjectMapper().createObjectNode();
                double salesprice = obj.get("salesprice").asDouble();
                String uom = obj.get("baseuom").asText();
                if (NullUtils.isNull(obj.get(ITEMCODE)) || StringUtils.isEmpty(obj.get(ITEMCODE).asText())) {
                    throw new DataTransformationService.TransformationException("itemcode cannot be empty");
                }
                String itemCode = obj.get(ITEMCODE).asText() + KGPL;
                String activeIndicator = obj.get("activeindicator").asText();
                String salespriceinbaseuom = obj.get("salespriceinbaseuom").asText();
                String mrp = obj.get("mrp").asText();
                String conversion1 = obj.get("conversion1").asText();
                skuNode.put("mrp", mrp);
                skuNode.put("uom", uom);
                skuNode.put("p", salesprice);
                skuNode.put("bpc", conversion1);
                skuNode.put("IsActive", activeIndicator);
                skuNode.put(ITEMCODE, itemCode);
                skuNode.put("c", salespriceinbaseuom);
                payload.set(itemCode, skuNode);
            });
            return payload;
        } else {
            org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode payload = JSONUtils.getObjectMapper().createObjectNode();
            org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode pricingObject = JSONUtils.getObjectMapper().createObjectNode();
            String salesPrice = priceplandetails.get(PRICEPLANDETAIL).get("salesprice").asText();
            String uom = priceplandetails.get(PRICEPLANDETAIL).get("baseuom").asText();
            if (NullUtils.isNull(priceplandetails.get(PRICEPLANDETAIL).get(ITEMCODE)) || StringUtils.isEmpty(priceplandetails.get(PRICEPLANDETAIL).get(ITEMCODE).asText())) {
                throw new DataTransformationService.TransformationException("itemcode cannot be empty");
            }
            String itemCode = priceplandetails.get(PRICEPLANDETAIL).get(ITEMCODE).asText() + KGPL;
            String conversion1 = priceplandetails.get(PRICEPLANDETAIL).get("conversion1").asText();
            String activeIndicator = priceplandetails.get(PRICEPLANDETAIL).get("activeindicator").asText();
            String salespriceinbaseuom = priceplandetails.get(PRICEPLANDETAIL).get("salespriceinbaseuom").asText();
            String mrp = priceplandetails.get(PRICEPLANDETAIL).get("mrp").asText();

            pricingObject.put("mrp", mrp);
            pricingObject.put("uom", uom);
            pricingObject.put("p", salesPrice);
            pricingObject.put("bpc", conversion1);
            pricingObject.put("IsActive", activeIndicator);
            pricingObject.put(ITEMCODE, itemCode);
            pricingObject.put("c", salespriceinbaseuom);
            payload.set(itemCode, pricingObject);
            return payload;
        }
    }
}
