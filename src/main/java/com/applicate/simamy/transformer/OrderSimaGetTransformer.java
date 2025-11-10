package com.applicate.simamy.transformer;


import com.applicate.services.channelkart.models.*;
import com.applicate.services.channelkart.services.*;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.transformers.TransformerInfo;
import com.applicate.services.channelkart.utils.*;
import com.bazaarvoice.jolt.*;
import com.fasterxml.jackson.core.type.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.slf4j.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

public class OrderSimaGetTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    public static final String ORDER_DETAILS = "orderDetails";
    private static final String DISCOUNT_ITEM_CODE = "discountItemCode";
    private TransformerInfo transformerInfo;
    private static Logger logger = LoggerFactory.getLogger(OrderSimaGetTransformer.class);
    public static final String DISCOUNT_INFO= "discountInfo";
    public static final String DISCOUNT= "discount";
    public static final String DISCOUNT_BATCH_CODE= "discountBatchCode";
    public static final String FEATURES = "features";

    public Map<String,Object>  transform(Map<String, Object> dataMap) {
        transformerInfo= this.getTransformerInfo();
        ArrayNode code_node= transformerInfo.getCode();
        if(NullUtils.isNotNull(code_node) && NullUtils.isNotNull(dataMap)) {
            try {
                Map<String,Object>transformedDataMap = transformOrderBody(dataMap);
                Object spec = JsonUtils.jsonToObject(String.valueOf(code_node));
                Chainr chainr = Chainr.fromSpec(spec);
                Object transformedOutput = chainr.transform(transformedDataMap);
                Map<String, Object> result = JSONUtils.getObjectMapper().readValue(JsonUtils.toPrettyJsonString(transformedOutput), new TypeReference<Map<String, Object>>() {});
                JsonNode features = JSONUtils.toJsonNode(result).get(FEATURES);
                if(NullUtils.isNotNull(features)){
                    features.forEach( feature->{
                        JsonNode discountInfo = feature.get(DISCOUNT_INFO);
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
                                    node.set("couponMinslab",s.get("minSlab"));
                                    node.put("couponCalcMethod","VALUE");
                                    couponInfo.add(node);
                                }
                            });
                        }
                        ((ObjectNode)feature).set("couponInfo",couponInfo);
                        enrichLineCount(orderDetails);
                        ((ObjectNode) feature).set(ORDER_DETAILS,((ObjectNode) feature).remove(ORDER_DETAILS));
                    });
                    result.put(FEATURES,JSONUtils.getObjectMapper().convertValue(features,List.class));
                    return result;
                }
                else{
                    HashMap res = new HashMap<String,Object>();
                    res.put(FEATURES,new ArrayList<>());
                    return res;
                }

            }catch(Exception ex) {
                logger.error("Jolt Transformer Exception",ex);
            }
        }
        else {
            throw new NullPointerException("Either jolt specification/input json found null");
        }
        return new HashMap<>();
    }

    void enrichLineCount(JsonNode orderDetails){
        AtomicInteger freeCount = new AtomicInteger(1001);
        AtomicInteger orderCount = new AtomicInteger(1);
        HashMap<String,JsonNode> freeItemMap = new HashMap<>();
        HashMap<String,ArrayNode> orderItemMap = new HashMap<>();
        orderDetails.forEach(orderDetail->{
            if(orderDetail.get("initialNormalizedQuantity").asDouble() == 0.0){
                ((ObjectNode)orderDetail).put("lineCount", freeCount.get());
                ((ObjectNode)orderDetail).put("isfreeItem", true);
                freeItemMap.put(orderDetail.get("batchCode").asText(),orderDetail);
                freeCount.getAndIncrement();
            }
            else{
                ((ObjectNode)orderDetail).put("lineCount", orderCount.get());
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
                orderCount.getAndIncrement();
            }
        });
        freeItemMap.forEach((k,v)->{
            ((ObjectNode)v).set(DISCOUNT_INFO, orderItemMap.get(k));
        });
    }

    Map<String,Object> transformOrderBody(Map<String,Object>dataMap){
        Map<String,Object> transformerdDataMap = new HashMap<>();
        ArrayList<Map<String,Object>> list= new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        ArrayList<OrderDetails>tempOrderDetailsList = new ArrayList<>();
        ArrayList<Map<String,Object>> features = mapper.convertValue(dataMap.get(FEATURES),new TypeReference <ArrayList<Map<String,Object>>>() {});
        features.forEach(feature->{
            Order order = mapper.convertValue(feature, Order.class);
            String status = order.getStatus();
            MetaDataService metaDataService = SpringContext.getBean(MetaDataService.class);
            ArrayNode supportedValueMetadata=metaDataService.fetchByValue("supportedValues","Order-status").getDomainValues();
            String code = StreamSupport.stream(supportedValueMetadata.spliterator(), false)
                    .filter(s->s.get("status")!=null && s.get("status").asText().equals(status))
                    .map(s -> s.get("code").asText()).findAny().orElse(null);
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
            Map<String,Object>orderMap = JSONUtils.toMap(order);
            orderMap.put("status",code);
            list.add(orderMap);
            tempOrderDetailsList.clear();
        });
        transformerdDataMap.put(FEATURES,list);
        return transformerdDataMap;
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

    private String getDiscountCode(JsonNode discountMap){
        if(StringUtils.isNotNull(discountMap.get(DISCOUNT_BATCH_CODE)) && StringUtils.isNotEmpty(discountMap.get(DISCOUNT_BATCH_CODE).asText())){
            return DISCOUNT_BATCH_CODE;
        }
        return DISCOUNT_ITEM_CODE;
    }
}
