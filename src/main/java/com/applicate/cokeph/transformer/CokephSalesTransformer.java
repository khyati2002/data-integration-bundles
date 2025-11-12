package com.applicate.cokeph.transformer;

import com.applicate.services.channelkart.services.OutletMetadataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.OutletMetadata;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CokephSalesTransformer extends AbstractTransformer<Map<String, Object>, Object> {

    public static final String NORMALIZED_QUANTITY = "normalizedQuantity";
    public static final String NET_AMOUNT = "netAmount";
    public static final String TOTAL_TAX_AMOUNT = "TotalTaxAmount";
    public static final String EXTENDED_ATTRIBUTES = "extendedAttributes";
    final OutletMetadataService outletMetadataService = (OutletMetadataService) ServiceLocator.lookup(OutletMetadata.class);

    @Override
    public Object transform(Map<String, Object> jsonobj) {
        Map<String, JsonNode> skuMap = new HashMap<>();
        org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode sales = JSONUtils.getObjectMapper().createObjectNode();

//        JsonNode valuesNode = JSONUtils.convert(jsonobj.get("values"), JsonNode.class);
        JsonNode valuesNode = JSONUtils.getObjectMapper().valueToTree(jsonobj.get("values"));

        if (valuesNode.isArray()) {
            for (JsonNode valueNode : valuesNode) {
                String customerId = valueNode.get("CustomerId").asText();
                Optional<String> outletCodeOpt = outletMetadataService.getOutletCodeIfExists(customerId);
                if (outletCodeOpt.isPresent()) {
                    customerId = outletCodeOpt.get();
                }
                String invoiceNumber = valueNode.get("InvoiceNumber").asText();
                String orderNumber = valueNode.get("OrderId").asText();
                String orderDate = formatDate(valueNode.get("CreationTime").asText());
                String billAmount = valueNode.get("TotalPrice").asText();
                String supplier = valueNode.get("Supplier Code").asText();
                List<String> validDistributorCodes = Arrays.asList("0502615941", "0503558429", "0502359930", "0502148945", "0505353136", "0502148915","0504905749","0505421116","0505285873");
                if (!validDistributorCodes.contains(supplier)) {
                    throw new DataTransformationService.TransformationException("Supplier not in validDistributorCodes list: " + supplier);
                }

                String routeCode = valueNode.get("Route").asText();
                String currencyCode = valueNode.get("Currency").asText();
                String voidIndicator = valueNode.get("Void Indicator").asText();
                String itemPriceInBaseUom = valueNode.get("ItemPriceInBaseUOM").asText();
                String lastModifiedTime = formatDate(valueNode.get("UpdateDate").asText());
                String creationTime = formatDate(valueNode.get("CreationTime").asText());
                String status = valueNode.get("Status").asText();
                String activeStatus = status.equals("Unsettled") ? "inactive" : "active";

                ArrayNode itemsNode = (ArrayNode) valueNode.get("Items");
                for (JsonNode item : itemsNode) {
                    updateMap(item, skuMap);
                }
                String combinedInvoiceNumber = invoiceNumber + "-" + supplier;

                sales.put("invoiceNumber",combinedInvoiceNumber);
                sales.put("loginId", supplier);
                sales.put("activeStatus", activeStatus);
                sales.put("beat", routeCode);
                sales.put("creationTime", creationTime);
                sales.put("outletCode", customerId);
                sales.put("orderNumber", orderNumber);
                sales.put("orderedDate", orderDate);
                sales.put("billAmount", billAmount);
                sales.put("lastModifiedTime", lastModifiedTime.isEmpty() ? getCurrentTime() : lastModifiedTime);
                sales.put("netAmount", getUpdatedNetAmount(skuMap));
                sales.put("normalizedQuantity", getUpdatedNormalizedQty(skuMap));
                sales.put("totalQuantity", getUpdatedNormalizedQty(skuMap));
                sales.put("normalizedVolume", 0);

                ObjectNode extended1 = JSONUtils.getObjectMapper().createObjectNode();
                extended1.put("CurrencyCode", currencyCode);
                extended1.put("VoidIndicator", voidIndicator);
                extended1.put("ItemPriceInBaseUOM", itemPriceInBaseUom);
                sales.set(EXTENDED_ATTRIBUTES, extended1);
            }
            sales.set("salesDetails", getSalesDetails(skuMap));
        } else {
            return null;
        }

        return sales;
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            throw new RuntimeException("Date parsing failed", e);
        }
    }

    private void updateMap(JsonNode item, Map<String, JsonNode> skuMap) {
        String skuCode = item.get("ProductId").asText();
        if (skuCode.isEmpty()) {
            return;
        }

        ObjectNode existingObject = (ObjectNode) skuMap.get(skuCode);
        if (existingObject != null) {
            // Update existing SKU
            int finalNQ = existingObject.get(NORMALIZED_QUANTITY).asInt() + item.get("Quantity").asInt();
            int finalPiece = existingObject.get("initialPieceQuantity").asInt() + item.get("TotalEaches").asInt();
            int finalCase = existingObject.get("caseQuantity").asInt() + item.get("TotalCases").asInt();
            existingObject.put(NORMALIZED_QUANTITY, finalNQ);
            existingObject.put("pieceQuantity", finalNQ);
            existingObject.put("initialPieceQuantity", finalPiece);
            existingObject.put("caseQuantity", finalCase);
            existingObject.put("initialCaseQuantity", finalCase);
            existingObject.put("netAmount", existingObject.get("netAmount").asDouble() + item.get("TotalNetPrice").asDouble());
            ObjectNode extended = (ObjectNode) existingObject.get(EXTENDED_ATTRIBUTES);
            double taxAmount = extended.get(TOTAL_TAX_AMOUNT).asDouble() + item.get("TotalTaxPrice").asDouble();
            extended.put(TOTAL_TAX_AMOUNT, taxAmount);
            existingObject.set(EXTENDED_ATTRIBUTES, extended);
        } else {
            // Create new SKU entry
            ObjectNode extended = JSONUtils.getObjectMapper().createObjectNode();
            extended.put("Bottlespercase", "0");
            extended.put("ItemQuantityEaches", item.get("TotalEaches").asText());
            extended.put("ItemPrice", "0");
            extended.put("PTRcase", "0");
            extended.put("saleslineno", item.get("SalesLineNo").asText());
            extended.put("Conversion1", item.get("Conversion1").asText());
            extended.put("TotalDiscountAmount", item.get("TotalDiscount").asText());
            extended.put(TOTAL_TAX_AMOUNT, item.get("TotalTaxPrice").asText());
            extended.put("IsFreeGood", item.get("IsFreeGoods").asText());

            ObjectNode salesDetailsNode = JSONUtils.getObjectMapper().createObjectNode();
            salesDetailsNode.put("initialPieceQuantity", item.get("TotalEaches").asText());
            salesDetailsNode.put("caseQuantity", item.get("TotalCases").asText());
            salesDetailsNode.put("initialCaseQuantity", item.get("TotalCases").asText());
            salesDetailsNode.put("skuCode", skuCode);
            salesDetailsNode.put("batchCode", skuCode);
            salesDetailsNode.put(NORMALIZED_QUANTITY, item.get("Quantity").asText());
            salesDetailsNode.put("pieceQuantity", item.get("Quantity").asText());
            salesDetailsNode.put("netAmount", item.get("TotalNetPrice").asText());
            salesDetailsNode.put("normalizedVolume", 0);
            salesDetailsNode.set(EXTENDED_ATTRIBUTES, extended);
            skuMap.put(skuCode, salesDetailsNode);
        }
    }

//    private JsonNode getSalesDetails(Map<String, JsonNode> skuMap) {
//        return JSONUtils.convert(skuMap.values(), JsonNode.class);
//    }

    private JsonNode getSalesDetails(Map<String, JsonNode> skuMap) {
        return JSONUtils.getObjectMapper().valueToTree(skuMap.values());
    }

    private int getUpdatedNormalizedQty(Map<String, JsonNode> skuMap) {
        return skuMap.values().stream().mapToInt(obj -> obj.get(NORMALIZED_QUANTITY).asInt()).sum();
    }

    private double getUpdatedNetAmount(Map<String, JsonNode> skuMap) {
        return skuMap.values().stream().mapToDouble(obj -> obj.get(NET_AMOUNT).asDouble()).sum();
    }

    private String getCurrentTime() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(new Date());
    }
}
