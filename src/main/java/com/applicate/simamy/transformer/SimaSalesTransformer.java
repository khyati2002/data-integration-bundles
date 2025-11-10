package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.models.ProductDetails;
import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SimaSalesTransformer extends AbstractTransformer<Map<String,Object>,Map<String,Object>> {
    ProductDetailsService productDetailsService = SpringContext.getBean(ProductDetailsService.class);
    @Override
    public Object transform(Map<String, Object> stringObjectMap) {
        ObjectNode sales = JSONUtils.getObjectMapper().createObjectNode();
        ArrayNode Items1 = JSONUtils.convert(stringObjectMap.get("Items"), ArrayNode.class);
        ArrayNode sDetails = JSONUtils.getObjectMapper().createArrayNode();
        float totalqty = Float.parseFloat(stringObjectMap.get("TotalUnits").toString());
        double totalamount = Double.parseDouble(stringObjectMap.get("TotalDebitAmount").toString());
        LocalDate date = LocalDate.parse(stringObjectMap.get("DeliveryDate").toString(), DateTimeFormatter.ISO_LOCAL_DATE);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String format = date.atStartOfDay().format(outputFormatter);
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        LocalDateTime date = LocalDateTime.parse(stringObjectMap.get("DeliveryDate").toString(), formatter);
        AtomicReference<Float> totalNormalizedQuantity = new AtomicReference<>((float) 0);
        Items1.forEach(item->{
            ObjectNode salesDetailsNode = JSONUtils.getObjectMapper().createObjectNode();
            ProductDetails product = productDetailsService.findBySkuCode(item.get("ArticleNumber").asText());
            salesDetailsNode.put("caseQuantity", item.get("Units").asText());
            salesDetailsNode.put("initialCaseQuantity", item.get("Units").asText());
            if (product == null) {
                salesDetailsNode.put("pieceQuantity", item.get("Units").asText());
                salesDetailsNode.put("normalizedQuantity", item.get("Units").asText());
                totalNormalizedQuantity.set(totalNormalizedQuantity.get() + item.get("Units").floatValue());
            } else {
                float caseToPieceQuantity = product.getCaseToPieceQuantity() * item.get("Units").floatValue();
                salesDetailsNode.put(
                        "pieceQuantity",
                        caseToPieceQuantity
                );
                salesDetailsNode.put(
                        "normalizedQuantity",
                        caseToPieceQuantity
                );
                totalNormalizedQuantity.set(totalNormalizedQuantity.get() + caseToPieceQuantity);
            }
            salesDetailsNode.put("netAmount", item.get("FinalPrice").asText());
            salesDetailsNode.put("skuCode", item.get("ArticleNumber").asText());
            salesDetailsNode.put("batchCode", item.get("ArticleNumber").asText());
            salesDetailsNode.put("creationTime", format);
            salesDetailsNode.put("normalizedVolume",0);
            salesDetailsNode.put("supplier",stringObjectMap.get("OutletLocation").toString());
            sDetails.add(salesDetailsNode);
        });

        sales.set("salesDetails", JSONUtils.getObjectMapper().convertValue(sDetails, JsonNode.class));
//        Object notebookMessage = stringObjectMap.get("NotebookMessage");
        if(stringObjectMap.containsKey("NotebookMessage")){
            ArrayNode notebookMessage = JSONUtils.convert(stringObjectMap.get("NotebookMessage"), ArrayNode.class);
            sales.set("orderNumber",notebookMessage.get(0));
        }
        JsonNode extended = new ObjectMapper().createObjectNode();
        ((ObjectNode)extended).put("TransactionNumber",stringObjectMap.get("TransactionNumber").toString());
        sales.put("invoiceNumber",stringObjectMap.get("DocumentNumber").toString());
        sales.put("outletCode",stringObjectMap.get("OutletNumber").toString());
        sales.put("loginId",stringObjectMap.get("OutletNumber").toString());
        sales.put("netAmount",totalamount);
        sales.put("totalQuantity",totalqty);
        sales.put("normalizedQuantity",totalNormalizedQuantity.get());
        sales.put("normalizedVolume",0);
        sales.put("supplier",stringObjectMap.get("OutletLocation").toString());
        sales.set("extendedAttributes",extended);

        return sales;
    }
}
