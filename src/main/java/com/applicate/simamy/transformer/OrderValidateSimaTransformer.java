package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.models.*;
import com.applicate.services.channelkart.repository.*;
import com.applicate.services.channelkart.services.*;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.transformers.TransformerInfo;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.slf4j.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderValidateSimaTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    public static final String BATCH_CODE = "batchCode";
    private static final String DISCOUNT_ITEM_CODE = "discountItemCode" ;
    private TransformerInfo transformerInfo;
    private static Logger logger = LoggerFactory.getLogger(OrderValidateSimaTransformer.class);

    public static final String STATUS = "status";
    public static final String DISCOUNT= "discount";
    public static final String ERRORCODE = "errorCode";
    public static final String MESSAGE = "message";
    public static final String DISCOUNT_INFO= "discountInfo";
    public static final String DISCOUNT_BATCH_CODE= "discountBatchCode";
    public static final String FEATURE = "feature";
    public static final String ORDER_DETAILS = "orderDetails";
    private ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> transform(Map<String, Object> dataMap) {
        transformerInfo = this.getTransformerInfo();
        ArrayNode code_node = transformerInfo.getCode();
        if (NullUtils.isNotNull(code_node) && NullUtils.isNotNull(dataMap)) {
            try {
                String status = String.valueOf(dataMap.get(STATUS));
                if(StringUtils.isNotNull(status)){
                    if (status.equalsIgnoreCase("Success")) {
                        Map<String,Object>transformedDataMap = transformOrderBody(dataMap);
                        Object spec = JsonUtils.jsonToObject(String.valueOf(code_node));
                        Chainr chainr = Chainr.fromSpec(spec);
                        Object transformedOutput = chainr.transform(transformedDataMap);
                        Map<String, Object> result = JSONUtils.getObjectMapper().readValue(JsonUtils.toPrettyJsonString(transformedOutput), new TypeReference<Map<String, Object>>() {});
                        JsonNode features = JSONUtils.toJsonNode(result).get(FEATURE);
                        features.forEach( feature-> {
                            JsonNode discountInfo =feature.get(DISCOUNT_INFO);
                            JsonNode orderDetails = feature.get(ORDER_DETAILS);
                            ArrayNode couponInfo =JSONUtils.getObjectMapper().createArrayNode();
                            if(NullUtils.isNotNull(discountInfo)&& discountInfo.size()>0){
                                discountInfo.forEach(s->{
                                    if(s.get("discountType").asText().equalsIgnoreCase("value")){
                                        ObjectNode node = JSONUtils.getObjectMapper().createObjectNode();
                                        node.set("couponNumber",s.get("discountId"));
                                        node.set(DISCOUNT,s.get(DISCOUNT));
                                        node.set("couponAmount",s.get(DISCOUNT));
                                        node.set("finalAmount",s.get("finalAmount"));
                                        node.set("actualAmount",s.get("actualAmount"));
                                        node.put("couponType","DISCOUNT");
                                        node.set("couponMinSlab",s.get("minSlab"));
                                        node.put("couponCalcMethod","VALUE");
                                        couponInfo.add(node);
                                    }
                                });
                            }
                            ((ObjectNode)feature).set("couponInfo",couponInfo);
                            enrichFreeItems(orderDetails);
                            ((ObjectNode) feature).set(ORDER_DETAILS,((ObjectNode) feature).remove(ORDER_DETAILS));
                        });
                        result.put(FEATURE,JSONUtils.getObjectMapper().convertValue(features,List.class));
                        return result;
                    } else {
                        return handleErrorCase(dataMap);
                    }
                }
                else{
                    if(StringUtils.isNotNull(dataMap.get(MESSAGE))) {
                        return Map.of(MESSAGE, dataMap.get(MESSAGE));
                    }
                }
            } catch (Exception ex) {
                logger.error("Jolt Transformer Exception", ex);
            }
        } else {
            throw new NullPointerException("Either jolt specification/input json found null");
        }
        return new HashMap<>();
    }

    public Map<String, Object> handleErrorCase(Map<String, Object> dataMap) {
        JsonNode enrichment = JSONUtils.toJsonNode(dataMap).get("enrichment");
        JsonNode validation = JSONUtils.toJsonNode(dataMap).get("validation");
        if (StringUtils.isNotNull(enrichment) && !enrichment.get("enrichmentResults").isEmpty()) {
            JsonNode enrichmentResults = enrichment.get("enrichmentResults").get(0);
            String message = String.valueOf(enrichmentResults.get(MESSAGE).asText(null));
            String errorCode = String.valueOf(enrichmentResults.get(ERRORCODE).asText(null));
            return Map.of(MESSAGE, message, ERRORCODE, errorCode);
        } else if (StringUtils.isNotNull(validation) && !validation.get("violations").isEmpty()) {
            JsonNode violations = validation.get("violations").get(0);
            String message = String.valueOf(violations.get(MESSAGE).asText(null));
            String errorCode = String.valueOf(violations.get(ERRORCODE).asText(null));
            return Map.of(MESSAGE, message, ERRORCODE, errorCode);
        }
        return new HashMap<>();
    }

    private OrderDetails createNewOrderDetails(OrderDetails orderDetail) {
        var orderDetails = new OrderDetails();
        ObjectNode extendedAttributes = (ObjectNode) orderDetail.getExtendedAttributes();
        double discountedPrice = NullUtils.isNotNull(extendedAttributes.get("discounted_price"))? Double.parseDouble(extendedAttributes.get("discounted_price").toString()): 0;
        float normalizedQuantity = orderDetail.getNormalizedQuantity()-orderDetail.getInitialNormalizedQuantity();
        double netAmount = discountedPrice * normalizedQuantity;
        double caseToPieceQuantity = NullUtils.isNotNull(extendedAttributes.get("caseToPieceQuantity"))? Double.parseDouble(extendedAttributes.get("caseToPieceQuantity").toString()): 1;
        int calculatedCaseQuantity = (int)(normalizedQuantity / caseToPieceQuantity);
        float calculatedPieceQuantity = (float)(normalizedQuantity % caseToPieceQuantity);
        float casePrice = (float)(discountedPrice * caseToPieceQuantity);
        orderDetails.setPrice((float)discountedPrice);
        orderDetails.setCasePrice(casePrice);
        orderDetails.setNetAmount(netAmount);
        orderDetails.setBillAmount(netAmount);
        orderDetails.setBatchCode(orderDetail.getBatchCode());
        orderDetails.setSkuCode(orderDetail.getSkuCode());
        orderDetails.setPieceQuantity(calculatedPieceQuantity);
        orderDetails.setCaseQuantity(calculatedCaseQuantity);
        orderDetails.setNormalizedQuantity(normalizedQuantity);
        return orderDetails;
    }

    Map<String,Object> transformOrderBody(Map<String,Object>dataMap){
        Map<String,Object> transformerdDataMap = new HashMap<>();
        ArrayList<Map<String,Object>> list= new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<OrderDetails>tempOrderDetailsList = new ArrayList<>();
        ArrayList<Map<String,Object>> features = mapper.convertValue(dataMap.get(FEATURE),new TypeReference <ArrayList<Map<String,Object>>>() {});
        features.forEach(feature->{
            Order order = mapper.convertValue(feature, Order.class);
            List<OrderDetails> orderDetailsList = order.getOrderDetails();
            if(!orderDetailsList.isEmpty()){
                orderDetailsList.forEach((orderDetails->{
                    float normalizedQuantity = orderDetails.getNormalizedQuantity();
                    float initialNormalizedQuantity = orderDetails.getInitialNormalizedQuantity();
                    float diff = normalizedQuantity -initialNormalizedQuantity;
                    if(diff != 0 && orderDetails.getInitialNormalizedQuantity() != 0){
                        OrderDetails tempOrderDetails=createNewOrderDetails(orderDetails);
                        orderDetails.setPieceQuantity(orderDetails.getInitialPieceQuantity());
                        orderDetails.setCaseQuantity(orderDetails.getInitialCaseQuantity());
                        orderDetails.setNormalizedQuantity(orderDetails.getInitialNormalizedQuantity());
                        orderDetails.setNetAmount(orderDetails.getNetAmount() - tempOrderDetails.getNetAmount());
                        orderDetails.setBillAmount(orderDetails.getBillAmount()-tempOrderDetails.getBillAmount());
                        tempOrderDetailsList.add(tempOrderDetails);
                    }
                }));
            }
            tempOrderDetailsList.forEach(tempOrderDetails->{
                order.getOrderDetails().add(tempOrderDetails);
            });
            list.add(JSONUtils.toMap(order));
            tempOrderDetailsList.clear();
        });
        ((ArrayList<Map<String,Object>>)list.get(0).get(ORDER_DETAILS)).forEach(s->{
            ProductDetails pd = SpringContext.getBean(ProductDetailsRepository.class).findByBatchCode(s.get(BATCH_CODE).toString());
            if(NullUtils.isNotNull(pd)){
                s.put("skuDescription",pd.getSkuDescription());
            }
        });
        transformerdDataMap.put(FEATURE,list);
        return transformerdDataMap;
    }

    void enrichFreeItems(JsonNode orderDetails){
        HashMap<String,JsonNode> freeItemMap = new HashMap<>();
        HashMap<String,ArrayNode> orderItemMap = new HashMap<>();
        orderDetails.forEach(orderDetail->{
            if(orderDetail.get("initialNormalizedQuantity").asDouble() == 0.0){
                ((ObjectNode)orderDetail).put("isfreeItem", true);
                freeItemMap.put(orderDetail.get(BATCH_CODE).asText(),orderDetail);
            }
            else{
                if(NullUtils.isNotNull(orderDetail.get(DISCOUNT_INFO))){
                    orderDetail.get(DISCOUNT_INFO).forEach(discountMap->{
                        String discountCode =getDiscountCode(discountMap);
                        if(StringUtils.isNotNull(discountMap.get(discountCode))){
                            ArrayNode orderItemArray = orderItemMap.getOrDefault(discountMap.get(discountCode).asText(),JSONUtils.getObjectMapper().createArrayNode());
                            orderItemArray.add(discountMap);
                            orderItemMap.put(discountMap.get(discountCode).asText(),orderItemArray);
                        }
                    });
                }
            }
        });
        freeItemMap.forEach((k,v)->{
            ((ObjectNode)v).set(DISCOUNT_INFO, orderItemMap.get(k));
        });
    }
    private String getDiscountCode(JsonNode discountMap){
        if(StringUtils.isNotNull(discountMap.get(DISCOUNT_BATCH_CODE)) && StringUtils.isNotEmpty(discountMap.get(DISCOUNT_BATCH_CODE).asText())){
            return DISCOUNT_BATCH_CODE;
        }
        return DISCOUNT_ITEM_CODE;
    }
}