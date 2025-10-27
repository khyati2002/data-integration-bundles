package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.commandline.RestUtils;
import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.OperationResult.StepResult;
import com.applicate.services.channelkart.exceptions.CustomRuntimeException;
import com.applicate.services.channelkart.models.Order;
import com.applicate.services.channelkart.models.OrderDetails;
import com.applicate.services.channelkart.models.ProductDetails;
import com.applicate.services.channelkart.schemes.services.external.ExternalDiscountCalculationHelper;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.OperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExternalDiscountEnrichment extends AbstractEnrichment<Order> {
    private static final String EVALUATOR = "evaluator";
    private static final String INVOICE = "invoice";
    private static final String BATCH_CODE = "batchCode";
    private static final String DISCOUNT_DETAILS_DTO_LIST = "discountDetailsDtoList";
    private static final String FINAL_BENEFIT = "finalBenefit";
    private static final String PROMO_TYPE = "promoType";

    private static final Logger log = LoggerFactory.getLogger(ExternalDiscountEnrichment.class);

    private final ExternalDiscountCalculationHelper externalDiscountCalculationHelper = SpringContext.getBean(ExternalDiscountCalculationHelper.class);

    @Override
    public OperationResult.StepResult apply(Order cdm) {

        JsonNode extendedAttributes = cdm.getExtendedAttributes();

        // Check if extendedAttributes contains "isNewSchemes" key
        if (extendedAttributes != null && extendedAttributes.has("isNewSchemes")) {

            ObjectNode newOrderDTO = ExternalDiscountCalculationHelper.convertOrderToNewOrderDTO(cdm);

            if (NullUtils.isNull(newOrderDTO)) {
                return OperationResult.StepResult.ERROR;
            }

            HttpEntity<String> request = externalDiscountCalculationHelper.prepareApiHeader(newOrderDTO);

            String discountServiceUrl = externalDiscountCalculationHelper.getPromoServiceRequestUrl();

            try {
                ResponseEntity<String> response = RestUtils.restTemplate().exchange(discountServiceUrl, HttpMethod.POST, request, String.class);

                String responseDiscount = response.getBody();

                JsonNode x = JSONUtils.getObjectMapper().readTree(responseDiscount).get("data");
                if (JSONUtils.getObjectMapper().readTree(responseDiscount).get("data") != null && x.isEmpty()) {
                    return OperationResult.StepResult.OK;
                }

                log.info( "Discount Obj from schemes service {}" , x);

                // promoType = item
                Map<String, List<Map<String, Object>>> itemDiscountMap = createItemDiscountMap(responseDiscount);

                // rest all cases
                Map<String, List<Map<String, Object>>> resultMap = createDiscountMap(responseDiscount);


                // Adjust discounts
                adjustValueDiscounts(resultMap, cdm);
                adjustItemDiscounts(itemDiscountMap, cdm);

                cdm.setNormalizedQuantity(adjustNormalisedQty(cdm));

                return OperationResult.StepResult.OK;
            } catch (Exception e) {
                log.error(e.getMessage());
                return OperationResult.StepResult.ERROR;
            }
        } else {
            return OperationResult.StepResult.OK;
        }

    }

    private float adjustNormalisedQty(Order cdm) {
        float totalNormalisedQty = 0;
        for(OrderDetails o : cdm.getOrderDetails()){
            totalNormalisedQty = totalNormalisedQty + o.getNormalizedQuantity();
        }
        return totalNormalisedQty;
    }

    private void adjustItemDiscounts(Map<String, List<Map<String, Object>>> itemDiscountMap, Order cdm) {
        Map<String, OrderDetails> updatedDetailsMap = initializeUpdatedDetailsMap(cdm);
        List<OrderDetails> updatedOrderDetails = new ArrayList<>(cdm.getOrderDetails());

        for (OrderDetails orderDetail : cdm.getOrderDetails()) {
            String batchCode = orderDetail.getBatchCode();

            if (itemDiscountMap.containsKey(batchCode)) {
                List<Map<String, Object>> discountList = itemDiscountMap.get(batchCode);

                for (Map<String, Object> discount : discountList) {
                    OrderDetails currentOrderDetail = updatedDetailsMap.get(batchCode); // get the recent value of od
                    JsonNode discountNode = JSONUtils.getObjectMapper().valueToTree(discount);
                    OrderDetails matchedOrderDetail = findMatchedOrderDetail(updatedOrderDetails, (String) discount.get("freeBatchCode"));

                    if (matchedOrderDetail != null) {
                        updateExistingOrderDetail(updatedOrderDetails, matchedOrderDetail, discountNode, discount);
                        updatedDetailsMap.put(matchedOrderDetail.getBatchCode(), matchedOrderDetail);
                    } else {
                        OrderDetails newOrderDetail = addNewOrderDetail(currentOrderDetail, updatedOrderDetails, discountNode, discount, cdm.getSupplierHierarchy());
                        updatedDetailsMap.put(newOrderDetail.getBatchCode(), newOrderDetail);
                    }
                }
            }
        }

        cdm.setOrderDetails(updatedOrderDetails);
    }

    private Map<String, OrderDetails> initializeUpdatedDetailsMap(Order cdm) {
        Map<String, OrderDetails> updatedDetailsMap = new HashMap<>();
        for (OrderDetails orderDetail : cdm.getOrderDetails()) {
            updatedDetailsMap.put(orderDetail.getBatchCode(), orderDetail);
        }
        return updatedDetailsMap;
    }

    private OrderDetails findMatchedOrderDetail( List<OrderDetails> updatedOrderDetails, String freeBatchCode) {
        for (OrderDetails od : updatedOrderDetails) {
            if (od.getBatchCode().equals(freeBatchCode)) {
                return od;
            }
        }
        return null;
    }

    private void updateExistingOrderDetail(List<OrderDetails> updatedOrderDetails, OrderDetails matchedOrderDetail, JsonNode discountNode, Map<String, Object> discount) {
        String freeBatchCodeUnit = (String) discount.get("freeBatchCodeUnit");
        double finalBenefit = (double) discount.get(FINAL_BENEFIT);
        ObjectMapper mapper = JSONUtils.getObjectMapper();

        matchedOrderDetail = externalDiscountCalculationHelper.addQuantity(freeBatchCodeUnit, finalBenefit, matchedOrderDetail);
        JsonNode freeBatchDiscountInfo = matchedOrderDetail.getDiscountInfo() != null ? matchedOrderDetail.getDiscountInfo().deepCopy() : mapper.createArrayNode();
        freeBatchDiscountInfo = setDiscountInfo(freeBatchDiscountInfo, discountNode);
        matchedOrderDetail.setDiscountInfo(freeBatchDiscountInfo);
        updatedOrderDetails.remove(matchedOrderDetail);
        updatedOrderDetails.add(matchedOrderDetail);
    }

    private OrderDetails addNewOrderDetail(OrderDetails orderDetail, List<OrderDetails> updatedOrderDetails, JsonNode discountNode, Map<String, Object> discount, String supplierHierarchy) {
        ObjectMapper mapper = JSONUtils.getObjectMapper();

        String freeBatchCode = (String) discount.get("freeBatchCode");
        float finalBenefit = ((Double) discount.get(FINAL_BENEFIT)).floatValue();
        String freeBatchCodeUnit = (String) discount.get("freeBatchCodeUnit");
        JsonNode extendedAttributes = mapper.createObjectNode();

        ProductDetails pd = externalDiscountCalculationHelper.getFreeProductDetails(freeBatchCode);

        if (NullUtils.isNull(pd)) {
            return orderDetail; // Exit early if no free product details found
        }

        OrderDetails updatedOrderDetail = mapper.convertValue(orderDetail, OrderDetails.class);
        JsonNode originalDiscountInfo = updatedOrderDetail.getDiscountInfo() != null ? updatedOrderDetail.getDiscountInfo().deepCopy() : mapper.createArrayNode();
        JsonNode updatedDiscountInfo = setDiscountInfo(originalDiscountInfo, discountNode);
        updatedOrderDetail.setDiscountInfo(updatedDiscountInfo);

        updatedOrderDetails.removeIf(existingOrderDetail -> existingOrderDetail.getBatchCode().equals(orderDetail.getBatchCode()));
        updatedOrderDetails.add(updatedOrderDetail);

        OrderDetails newOrderDetail = externalDiscountCalculationHelper.createNewOrderDetailsAsPerScheme(freeBatchCode, pd, finalBenefit, freeBatchCodeUnit);

        ((ObjectNode) extendedAttributes).put("discountItem", "true");
        newOrderDetail.setExtendedAttributes(extendedAttributes);
        newOrderDetail.setDiscountInfo(null);
        newOrderDetail.setSupplierHierarchy(supplierHierarchy);
        externalDiscountCalculationHelper.addNewProductDetails(newOrderDetail, pd);
        updatedOrderDetails.add(newOrderDetail);

        return updatedOrderDetail;
    }

    private void adjustValueDiscounts(Map<String, List<Map<String, Object>>> resultMap, Order cdm) {

        List<OrderDetails> newOrderDetails = new ArrayList<>();

        double totalNetAmount = 0;
        double totalBillAmount = 0;
        double totalDiscount = 0;

        for (OrderDetails orderDetail : cdm.getOrderDetails()) {
            OrderDetails odCopy = new OrderDetails();
            odCopy = JSONUtils.getObjectMapper().convertValue(orderDetail, odCopy.getClass());
            List<Map<String, Object>> ls = resultMap.get(orderDetail.getBatchCode());
            if (NullUtils.isNotNull(ls)) {
                double totalBatchCodeDiscount = 0;
                for (Map<String, Object> discount : ls) {
                    totalBatchCodeDiscount = totalBatchCodeDiscount + (double) discount.get(FINAL_BENEFIT);

                    ObjectMapper mapper = JSONUtils.getObjectMapper();
                    JsonNode discountNode = mapper.valueToTree(discount);

                    // need to return json node , otherwise changes wont persist
                    JsonNode updatedDiscountInfo = setDiscountInfo(odCopy.getDiscountInfo().deepCopy(), discountNode);
                    odCopy.setDiscountInfo(updatedDiscountInfo);

                }

                BigDecimal netAmount = BigDecimal.valueOf(orderDetail.getNetAmount());
                BigDecimal batchCodeDiscount = BigDecimal.valueOf(totalBatchCodeDiscount);
                BigDecimal netPrice = netAmount.subtract(batchCodeDiscount);


                totalNetAmount += netPrice.doubleValue();
                totalBillAmount += netPrice.doubleValue();

                totalDiscount = totalDiscount + totalBatchCodeDiscount;

                odCopy.setNetAmount(netPrice.doubleValue());
                odCopy.setBillAmount(netPrice.doubleValue());
                newOrderDetails.add(odCopy);

            } else {
                totalNetAmount += orderDetail.getNetAmount();
                totalBillAmount += orderDetail.getBillAmount();
                odCopy.setDiscountInfo(JSONUtils.getObjectMapper().createArrayNode());
                newOrderDetails.add(odCopy);
            }

        }

        cdm.setNetAmount(totalNetAmount);
        cdm.setBillAmount(totalBillAmount);
        cdm.setOrderDetails(newOrderDetails.isEmpty() ? cdm.getOrderDetails() : newOrderDetails);

        cdm.setDiscountInfo(setTotalOrderDiscountInfo(cdm, totalDiscount));

        log.error("Total Value Benefit calculated from backend size {} ", totalDiscount);
    }


    private JsonNode setTotalOrderDiscountInfo(Order cdm, double totalDiscount) {
        ObjectMapper mapper = JSONUtils.getObjectMapper();

        ObjectNode discountInfoNode = mapper.createObjectNode();
        ArrayNode discountArrayNode = mapper.createArrayNode();

        double actualAmount = cdm.getTotalAmount();
        double finalAmount = actualAmount - totalDiscount;

        discountInfoNode.put("discountType", "Summary");
        discountInfoNode.put("actualAmount", actualAmount);
        discountInfoNode.put("discount", totalDiscount);
        discountInfoNode.put("finalAmount", finalAmount);

        discountArrayNode.add(discountInfoNode);
        return discountArrayNode;
    }

    private JsonNode setDiscountInfo(JsonNode discountInfo, JsonNode discount) {
        ObjectMapper mapper = JSONUtils.getObjectMapper();

        if (discountInfo == null || discountInfo.isNull()) {
            ArrayNode newDiscountArray = mapper.createArrayNode();
            newDiscountArray.add(discount);
            return newDiscountArray;
        } else if (discountInfo.isArray()) {
            ((ArrayNode) discountInfo).add(discount);
        }
        return discountInfo;
    }

    private Map<String, List<Map<String, Object>>> createDiscountMap(String responseDiscount) throws CustomRuntimeException {
        Map<String, Object> responseMap;
        try {
            responseMap = JSONUtils.getObjectMapper().readValue(responseDiscount, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new CustomRuntimeException(e);
        }
        List<Map<String, Object>> discountList = (List<Map<String, Object>>) responseMap.get("data");

        Map<String, List<Map<String, Object>>> resultMap = new HashMap<>();

        for (Map<String, Object> item : discountList) {
            String promoType = (String) item.get(PROMO_TYPE);
            String evaluator = (String) item.get(EVALUATOR);

            // Only include promos of types other than "item"
            if (!"item".equals(promoType) && !INVOICE.equals(evaluator)) {
                List<Map<String, Object>> discountDetailsList = (List<Map<String, Object>>) item.get(DISCOUNT_DETAILS_DTO_LIST);

                if (discountDetailsList != null) {
                    for (Map<String, Object> discountValues : discountDetailsList) {
                        Map<String, Object> flattenedMap = new HashMap<>(item);
                        flattenedMap.putAll(discountValues);
                        flattenedMap.remove(DISCOUNT_DETAILS_DTO_LIST);

                        // Add discountId = promoId
                        String promoId = (String) item.get("promoId");
                        flattenedMap.put("discountId", promoId);

                        String batchCode = (String) discountValues.get(BATCH_CODE);

                        resultMap.computeIfAbsent(batchCode, k -> new ArrayList<>());
                        resultMap.get(batchCode).add(flattenedMap);
                    }
                }
            }
        }
        return resultMap;
    }


    private Map<String, List<Map<String, Object>>> createItemDiscountMap(String responseDiscount) throws CustomRuntimeException {
        ObjectMapper mapper = JSONUtils.getObjectMapper();
        Map<String, Object> responseMap;
        try {
            responseMap = mapper.readValue(responseDiscount, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new CustomRuntimeException(e);
        }
        List<Map<String, Object>> discountList = (List<Map<String, Object>>) responseMap.get("data");

        Map<String, List<Map<String, Object>>> resultMap = new HashMap<>();

        for (Map<String, Object> item : discountList) {
            String promoType = (String) item.get(PROMO_TYPE);
            String evaluator = (String) item.get(EVALUATOR);

            // Only process items with promoType="item"
            if ("item".equals(promoType) && !INVOICE.equals(evaluator)) {
                List<Map<String, Object>> discountDetailsList = (List<Map<String, Object>>) item.get(DISCOUNT_DETAILS_DTO_LIST);

                if (discountDetailsList != null) {
                    for (Map<String, Object> discountValues : discountDetailsList) {
                        Map<String, Object> combinedMap = new HashMap<>(item);
                        combinedMap.putAll(discountValues);

                        combinedMap.remove(DISCOUNT_DETAILS_DTO_LIST);

                        // Add discountId = promoId
                        String promoId = (String) item.get("promoId");
                        combinedMap.put("discountId", promoId);

                        String batchCode = (String) discountValues.get(BATCH_CODE);

                        resultMap.computeIfAbsent(batchCode, k -> new ArrayList<>());
                        resultMap.get(batchCode).add(combinedMap);
                    }
                }
            }
        }
        return resultMap;
    }

}

