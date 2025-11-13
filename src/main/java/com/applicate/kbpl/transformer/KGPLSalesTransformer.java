package com.applicate.kbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KGPLSalesTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    public static final String KGPL = "-KGPL";
    public static final String NORMALIZED_QUANTITY = "normalizedQuantity";

    public static final String NET_AMOUNT = "netAmount";

    public static final String TOTAL_TAX_AMOUNT = "TotalTaxAmount";

    public static final String EXTENDED_ATTRIBUTES = "extendedAttributes";

    private static final String SALES_LINE = "saleslineno";

    private static final String SI_DETAIL = "sidetailc2";
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public Map<String, Object> transform(Map<String, Object> jsonobj) {

        Map<String, JsonNode> skuMap = new HashMap<>();

        //ObjectNode sales = JSONUtils.getObjectMapper().createObjectNode();
        ObjectNode sales = new ObjectMapper().createObjectNode();
        ArrayNode siDetailC2 = objectMapper.createArrayNode();
        //ArrayNode siDetailC2 = JSONUtils.getObjectMapper().createArrayNode();
        //JsonNode dmssiDetailsC2 = JSONUtils.convert(jsonobj.get("dmssidetailsc2"), JsonNode.class);
        JsonNode dmssiDetailsC2 = objectMapper.valueToTree(jsonobj.get("dmssidetailsc2"));
        //JsonNode dmssiDetailsC2 = JSONUtils.getObjectMapper().convertValue(jsonobj.get("dmssidetailsc2"), JsonNode.class);
//        if (dmssiDetailsC2.get(SI_DETAIL) instanceof ObjectNode) {
//            siDetailC2.add(dmssiDetailsC2.get(SI_DETAIL));
//        } else {
//            siDetailC2 = JSONUtils.getObjectMapper().convertValue(dmssiDetailsC2.get(SI_DETAIL), ArrayNode.class);
//        }
        JsonNode siDetailNode = dmssiDetailsC2.get(SI_DETAIL);
        if (siDetailNode == null) {
            return null; // or handle error
        } else if (siDetailNode.isArray()) {
            siDetailC2 = (ArrayNode) siDetailNode;
        } else if (siDetailNode.isObject()) {
            siDetailC2.add(siDetailNode);
        } else {
            return null; // or handle unexpected JSON type
        }

        if (siDetailC2 != null && siDetailC2.isArray()) {
            ArrayNode dummyNode = objectMapper.createArrayNode();
            siDetailC2.forEach(item -> dummyNode.add(updateMap(item, skuMap)));
            sales.set("salesDetails", getSalesDetails(skuMap));
        } else {
            return null;
        }

        if (jsonobj.get("documentnumber") == null || jsonobj.get("customercode") == null) {
            return null;
        }
        ifEmpty(jsonobj.get("customercode")).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("customercode cannot be empty");
        });


        String invoiceNumber = jsonobj.get("documentnumber").toString() + "-" + jsonobj.get("tenantcode").toString()+ KGPL;
        String outletcode = jsonobj.get("customercode").toString();
        String supplier = jsonobj.get("tenantcode").toString();
        String orderNumber = jsonobj.get("soreferenceno").toString();
        String orderDate = jsonobj.get("orderdate").toString().replace("T", " ").split("\\.")[0];
        String billAmount = jsonobj.get("documentamount").toString();
        String currencyCode = jsonobj.get("currencycode").toString();
        String voidIndicator = jsonobj.get("voidindicator").toString();


        String lastModifiedTime = jsonobj.get("lastmodifieddatetime").toString().replace("T", " ").split("\\.")[0];
        String creationTime = jsonobj.get("documentdate").toString().replace("T", " ").split("\\.")[0];


        sales.put("invoiceNumber", invoiceNumber);
        if (creationTime.isEmpty()) {
            return null;
        } else {
            sales.put("creationTime", creationTime);
        }

        // Setting outletCode as salesman if salesman is empty to not impact eB2B recommendations
        String salesmanCode = jsonobj.getOrDefault("salesmancode", outletcode).toString();
        String finalSalesmanCode = salesmanCode.equals(outletcode) ? outletcode + KGPL : (salesmanCode + "-" + supplier + KGPL);
        sales.put("outletCode", outletcode + KGPL);
        sales.put("loginId", finalSalesmanCode);
        sales.put("orderNumber", orderNumber);
        if (!orderDate.isEmpty()) {
            sales.put("orderedDate", orderDate);
        }
        sales.put("billAmount", billAmount);
        if (lastModifiedTime.isEmpty()) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentLastModifiedTime = format.format(new Date());
            sales.put("lastModifiedTime", currentLastModifiedTime);
        } else {
            sales.put("lastModifiedTime", lastModifiedTime);
        }

        sales.put(NET_AMOUNT, getUpdatedNetAmount(skuMap));
        sales.put("mrp", getUpdatedPriceMrp(skuMap));
        sales.put(NORMALIZED_QUANTITY, getUpdatedNormalizedQty(skuMap));
        sales.put("totalQuantity", getUpdatedNormalizedQty(skuMap));
        sales.put("normalizedVolume", 0);

        //ObjectNode extended1 = JSONUtils.getObjectMapper().createObjectNode();
        ObjectNode extended1 = objectMapper.createObjectNode();
        extended1.put("CurrencyCode", currencyCode);
        extended1.put("VoidIndicator", voidIndicator);
        sales.set(EXTENDED_ATTRIBUTES, extended1);
        sales.put("source", "KGPL");

        //return (Map<String, Object>) sales;
        return objectMapper.convertValue(sales, Map.class);
    }

    private ObjectNode updateMap(JsonNode item, Map<String, JsonNode> skuMap) {
        ifEmpty(item.get("itemcode")).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("itemcode cannot be empty");
        });
        String skuCode = item.get("itemcode").asText() + KGPL;
        if (skuMap.containsKey(skuCode)) {
            ObjectNode existingObject = (ObjectNode) skuMap.get(skuCode);
            int finalNQ = existingObject.get(NORMALIZED_QUANTITY).asInt() + item.get("itemquantity").asInt();
            int finalPiece = existingObject.get("initialPieceQuantity").asInt() + item.get("itemquantity1").asInt();
            int finalCase= existingObject.get("caseQuantity").asInt() + item.get("itemquantity2").asInt();
            existingObject.put(NORMALIZED_QUANTITY, finalNQ);
            existingObject.put("pieceQuantity", finalNQ);
            existingObject.put("initialPieceQuantity", finalPiece);
            existingObject.put("caseQuantity", finalCase);
            existingObject.put("initialCaseQuantity", finalCase);
            existingObject.put(NET_AMOUNT, existingObject.get(NET_AMOUNT).asInt() + item.get("totalnetamount").asInt());
            ObjectNode extended = (ObjectNode) existingObject.get(EXTENDED_ATTRIBUTES);
            double taxAmount = extended.get(TOTAL_TAX_AMOUNT).asDouble() + item.get("totaltaxamount").asDouble();
            extended.put(TOTAL_TAX_AMOUNT, taxAmount);
            existingObject.set(EXTENDED_ATTRIBUTES, extended);
            skuMap.put(skuCode, existingObject);
            return (ObjectNode) skuMap.get(skuCode);
        } else {
            String mrp = item.get("mrp").asText();
            if (mrp.isEmpty()) {
                mrp = "0";
            }
            String normalizedQuantity = item.get("itemquantity").asText();
            String netAmount = item.get("totalnetamount").asText();
            if (skuCode.isEmpty() || normalizedQuantity.isEmpty() || netAmount.isEmpty()) {
                return null;
            }




            //ObjectNode extended = JSONUtils.getObjectMapper().createObjectNode();
            ObjectNode extended = objectMapper.createObjectNode();
            extended.put(SALES_LINE, item.get(SALES_LINE).asText());
            extended.put("Bottlespercase", item.get("conversion1").asText());
            extended.put("ItemQuantityEaches", item.get("itemquantity1").asText());
            extended.put("ItemPrice", item.get("itemprice").asText());
            extended.put("PTRcase", item.get("itempriceinbaseuom").asText());
            extended.put("TotalDiscountAmount", item.get("totaldiscountamount").asText());
            extended.put(TOTAL_TAX_AMOUNT, item.get("totaltaxamount").asText());
            extended.put("IsFreeGood", item.get("isfreegood").asText());

            //ObjectNode salesDetailsNode = JSONUtils.getObjectMapper().createObjectNode();
            ObjectNode salesDetailsNode = objectMapper.createObjectNode();
            salesDetailsNode.put("mrp", mrp);
            salesDetailsNode.put("initialPieceQuantity", item.get("itemquantity1").asText());
            salesDetailsNode.put("caseQuantity", item.get("itemquantity2").asText());
            salesDetailsNode.put("initialCaseQuantity", item.get("itemquantity2").asText());
            salesDetailsNode.put("skuCode", skuCode);
            salesDetailsNode.put("batchCode", skuCode);
            salesDetailsNode.put(NORMALIZED_QUANTITY, normalizedQuantity);
            salesDetailsNode.put("pieceQuantity", normalizedQuantity);
            salesDetailsNode.put(NET_AMOUNT, netAmount);
            salesDetailsNode.put("normalizedVolume", 0);
            salesDetailsNode.set(EXTENDED_ATTRIBUTES, extended);
            skuMap.put(skuCode, salesDetailsNode);
            return salesDetailsNode;
        }
    }

    private JsonNode getSalesDetails(Map<String, JsonNode> skuMap) {
        //return JSONUtils.convert(skuMap.values(), JsonNode.class);
        //return JSONUtils.getObjectMapper().convertValue(skuMap.values(), JsonNode.class);
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (JsonNode node : skuMap.values()) {
            arrayNode.add(node);
        }
        return arrayNode;
    }

    private int getUpdatedNormalizedQty(Map<String, JsonNode> skuMap) {
        return skuMap.values().stream().mapToInt(obj -> obj.get(NORMALIZED_QUANTITY).asInt()).sum();
    }

    private int getUpdatedPriceMrp(Map<String, JsonNode> skuMap) {
        return skuMap.values().stream().mapToInt(obj -> obj.get("mrp").asInt()).sum();
    }

    private double getUpdatedNetAmount(Map<String, JsonNode> skuMap) {
        return skuMap.values().stream().mapToDouble(obj -> obj.get(NET_AMOUNT).asDouble()).sum();
    }

    private static Optional<String> ifEmpty(Object object) {
        if(ObjectUtils.isEmpty(object))  {
            return Optional.empty();
        } else {
            return Optional.of(object.toString());
        }
    }
}
