package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class TIStockDeductTransformer extends AbstractTransformer<ObjectNode, ObjectNode> {

    private static final Logger logger = LoggerFactory.getLogger(TIStockDeductTransformer.class);
    private static final String SUPPLIER = "supplier";
    private static final String OUTLET_CODE = "outletCode";
    private static final String SKU_CODE = "skuCode";

    @Override
    public JsonNode transform(ObjectNode commonData) {
        ObjectNode resultNode = JSONUtils.getObjectMapper().createObjectNode();
        ArrayNode reqBody = JSONUtils.getObjectMapper().createArrayNode();
        try {
            // Extract basic fields
            String salesId = commonData.path("id").asText(null);
            String outletCode = commonData.path("outletCode").asText(null);
            String supplier = commonData.path("supplier").asText(null);

            // Extract sales details array
            JsonNode salesDetailsNode = commonData.path("salesDetails");

            List<ObjectNode> orderDetails = new ArrayList<>();
            if (salesDetailsNode.isArray()) {
                for (JsonNode detailNode : salesDetailsNode) {
                    orderDetails.add(createSkuNode(detailNode));
                }
            }

            // Construct the final result node
            resultNode.put("orderId", salesId)
                    .put("customerId", outletCode)
                    .putNull("warehouseId")
                    .put(SUPPLIER, supplier)
                    .putArray("skuDetails").addAll(orderDetails);
            resultNode.set(OUTLET_CODE, null);

        } catch (Exception e) {
            logger.error("Error transforming CommonDataModel to PrimaryOrderRequest: {}", e.getMessage(), e);
            resultNode = JSONUtils.getObjectMapper().createObjectNode();
        }
        return resultNode;

    }

    /**
     * Creates an ObjectNode representing a SKU (Stock Keeping Unit) from a
     * JsonNode.
     * This method extracts relevant information from the input JsonNode and
     * constructs a new ObjectNode with SKU details.
     *
     * @param detailNode the JsonNode containing SKU details
     * @return an ObjectNode with SKU information including SKU code, quantity,
     * batch ID, and expiry date
     */
    private ObjectNode createSkuNode(JsonNode detailNode) {
        String batchCode = detailNode.path("batchCode").asText(null);
        int currCaseQuantity = (int) Math.floor(detailNode.path("caseQuantity").asDouble(0.0));
        int currPieceQuantity = (int) Math.floor(detailNode.path("pieceQuantity").asDouble(0.0));
        int currOthersQuantity = (int) Math.floor(detailNode.path("otherUnitQuantity").asDouble(0.0));
        int preCaseQuantity = (int) Math.floor(detailNode.path("initialCaseQuantity").asDouble(0.0));
        int prePieceQuantity = (int) Math.floor(detailNode.path("initialPieceQuantity").asDouble(0.0));
        int preOthersQuantity = (int) Math.floor(detailNode.path("initialOtherUnitQuantity").asDouble(0.0));

        int totalQuantity = currCaseQuantity + currPieceQuantity + currOthersQuantity +
                preCaseQuantity + prePieceQuantity + preOthersQuantity;

        return JSONUtils.getObjectMapper().createObjectNode()
                .put(SKU_CODE, batchCode)
                .put("caseQty", currCaseQuantity)
                .put("pieceQty", currPieceQuantity)
                .put("otherQty", currOthersQuantity)
//                        .put("previousCaseQty", preCaseQuantity)
//                        .put("previousPieceQty", prePieceQuantity)
//                        .put("previousOtherQty", preOthersQuantity)
                .put("totalQty", totalQuantity);
    }

}
