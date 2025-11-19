//package com.applicate.simamy.transformer;
//
//import com.applicate.services.channelkart.models.AbstractOrderDetails;
//import com.applicate.services.channelkart.models.OrderDetails;
//import com.applicate.services.channelkart.transformers.AbstractTransformer;
//import com.applicate.services.channelkart.utils.JSONUtils;
//import com.applicate.services.channelkart.utils.NullUtils;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.math.BigDecimal;
//import java.math.RoundingMode;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class SeparateFreeItemsAndCouponValue extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
//
//    private static final String FEATURES = "feature";
//    public static final String ORDERDETAILS = "orderDetails";
//    public static final String DISCOUNT = "discount";
//    public static final String COUPON = "coupon";
//    public static final String NORMALIZED_QUANTITY = "normalizedQuantity";
//    public static final String INITIAL_NORMALIZED_QUANTITY = "initialNormalizedQuantity";
//    public static final String EXTENDED_ATTRIBUTES = "extendedAttributes";
//    public static final String BATCH_CODE = "batchCode";
//    public static final String SKU_CODE = "skuCode";
//    public static final String PIECE_QUANTITY = "pieceQuantity";
//    public static final String CASE_QUANTITY = "caseQuantity";
//    public static final String OTHER_UNIT_QUANTITY = "otherUnitQuantity";
//    public static final String INITIAL_PIECE_QUANTITY = "initialPieceQuantity";
//    public static final String INITIAL_CASE_QUANTITY = "initialCaseQuantity";
//    public static final String INITIAL_OTHER_UNIT_QUANTITY = "initialOtherUnitQuantity";
//    private Logger logger = LoggerFactory.getLogger(SeparateFreeItemsAndCouponValue.class);
//
//    @Override
//    public Map<String, Object> transform(Map<String, Object> dataMap) {
//        ObjectMapper mapper = new ObjectMapper();
//        if (NullUtils.isNotNull(dataMap)) {
//            try {
//                ArrayList<Map<String,Object>> features = mapper.convertValue(dataMap.get(FEATURES),new TypeReference<ArrayList<Map<String,Object>>>() {});
//                if(NullUtils.isNotNull(features)) {
//                    ArrayList<Map<String, Object>> result = new ArrayList<>();
//                    Map<String, Object> transformedDataMap = transformOrderBody(features);
//                    ArrayList<Map<String, Object>> transformedDataMapFeatures = mapper.convertValue(transformedDataMap.get(FEATURES),new TypeReference<ArrayList<Map<String,Object>>>() {});
//                    Map<String,Object> feature = transformedDataMapFeatures.get(0);
//                    ArrayList<Map<String, Object>> orderDetails = mapper.convertValue(feature.get(ORDERDETAILS), new TypeReference<ArrayList<Map<String, Object>>>() {});
//                    separateCouponAndDiscountOnOrder(feature);
//                    separateCouponAndDiscountOnOrderDetails(orderDetails);
//                    feature.put(ORDERDETAILS, orderDetails);
//                    result.add(feature);
//                    dataMap.put(FEATURES,result);
//                }
//                return dataMap;
//            } catch (Exception ex) {
//                logger.error("Transformer Exception", ex);
//            }
//        } else {
//            throw new NullPointerException("Either  specification/input json found null");
//        }
//        return new HashMap<>();
//    }
//
//
//    private AbstractOrderDetails createNewOrderDetails(Map<String,Object> orderDetail) {
//        var orderDetails = new OrderDetails();
//        float calculatedNormalizedQuantity = Float.parseFloat(orderDetail.get(NORMALIZED_QUANTITY).toString()) - Float.parseFloat(orderDetail.get(INITIAL_NORMALIZED_QUANTITY).toString());
//        JsonNode extendedAttributes = JSONUtils.toJsonNode(orderDetail.get("extendedAttributes"));
//        float caseToPieceQuantityOrderDetails = NullUtils.isNull(extendedAttributes.get("caseToPieceQuantity")) || extendedAttributes.get("caseToPieceQuantity").floatValue() == 0 ? 1 :extendedAttributes.get("caseToPieceQuantity").floatValue()  ;
//        int calculatedCaseQuantity = (int)(calculatedNormalizedQuantity / caseToPieceQuantityOrderDetails);
//        float calculatedPieceQuantity = calculatedNormalizedQuantity % caseToPieceQuantityOrderDetails;
//        orderDetails.setBatchCode(orderDetail.get(BATCH_CODE).toString());
//        orderDetails.setSkuCode(orderDetail.get(SKU_CODE).toString());
//        orderDetails.setCaseQuantity(calculatedCaseQuantity);
//        orderDetails.setPieceQuantity(calculatedPieceQuantity);
//        ObjectNode ext=JSONUtils.getObjectMapper().createObjectNode();
//        ext.put("blobKey","");
//        orderDetails.setExtendedAttributes(ext);
//        orderDetails.setOtherUnitQuantity(Float.parseFloat(orderDetail.get(OTHER_UNIT_QUANTITY).toString())- Float.parseFloat(orderDetail.get(INITIAL_OTHER_UNIT_QUANTITY).toString()));
//        orderDetails.setNormalizedQuantity(calculatedNormalizedQuantity);
//        return orderDetails;
//    }
//
//    private Map<String, Object> transformOrderBody(ArrayList<Map<String,Object>> features) {
//        Map<String,Object> transformerdDataMap = new HashMap<>();
//        ArrayList<Map<String,Object>> list= new ArrayList<>();
//        ObjectMapper mapper = new ObjectMapper();
//        if(NullUtils.isNotNull(features)) {
//            for (Map<String, Object> feature : features) {
//                List<Map<String,Object>> tempOrderDetailsList = new ArrayList<>();
//                List<Map<String,Object>> orderDetailsList = (List<Map<String,Object>>)feature.get(ORDERDETAILS);
//                if (!orderDetailsList.isEmpty()) {
//                    for (var orderDetails : orderDetailsList) {
//                        tempOrderDetailsList.add(orderDetails);
//                        float normalizedQuantity = Float.parseFloat(orderDetails.get(NORMALIZED_QUANTITY).toString());
//                        float initialNormalizedQuantity = Float.parseFloat(orderDetails.get(INITIAL_NORMALIZED_QUANTITY).toString());
//                        float diff = normalizedQuantity - initialNormalizedQuantity;
//                        if (diff != 0 && initialNormalizedQuantity != 0) {
//                            Map<String,Object> tempOrderDetails = mapper.convertValue(createNewOrderDetails(orderDetails),Map.class);
//                            orderDetails.put(PIECE_QUANTITY,orderDetails.get(INITIAL_PIECE_QUANTITY));
//                            orderDetails.put(CASE_QUANTITY,orderDetails.get(INITIAL_CASE_QUANTITY));
//                            orderDetails.put(NORMALIZED_QUANTITY,orderDetails.get(INITIAL_NORMALIZED_QUANTITY));
//                            tempOrderDetails.put("skuDescription",orderDetails.get("skuDescription"));
//                            tempOrderDetailsList.add(tempOrderDetails);
//                        }
//                    }
//                }
//                feature.put(ORDERDETAILS,tempOrderDetailsList);
//                list.add(feature);
//            }
//        }
//        transformerdDataMap.put(FEATURES,list);
//        return transformerdDataMap;
//    }
//
//    private void separateCouponAndDiscountOnOrder(Map<String,Object> orderMap){
//        ArrayList<Map<String,Object>> discountInfoList = (ArrayList) orderMap.get("discountInfo");
//        BigDecimal totalDiscountValue = BigDecimal.ZERO;
//        BigDecimal discountValue;
//        BigDecimal couponValue = BigDecimal.ZERO;
//        if(NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()){
//            for (Map<String, Object> discountInfo : discountInfoList) {
//                String discountType = discountInfo.getOrDefault("discountType","").toString();
//                String programLevel = discountInfo.getOrDefault("programLevel","").toString();
//                if (discountType.equalsIgnoreCase("Summary")){
//                    totalDiscountValue = BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT,0).toString()));
//                }
//                else if (!discountType.equalsIgnoreCase("Summary") && programLevel.equalsIgnoreCase(COUPON)){
//                    couponValue = couponValue.add(BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT, 0).toString())));
//                }
//            }
//        }
//        discountValue =  totalDiscountValue.subtract(couponValue).setScale(2, RoundingMode.HALF_UP);
//        couponValue = couponValue.setScale(2,RoundingMode.HALF_UP);
//        orderMap.put("totalDiscountValue",totalDiscountValue);
//        orderMap.put("couponValue",couponValue);
//        orderMap.put("discountValue",discountValue);
//    }
//
//    private void separateCouponAndDiscountOnOrderDetails(ArrayList<Map<String,Object>> orderDetails){
//        orderDetails.forEach(orderDetail->{
//            ArrayList<Map<String,Object>> discountInfoList = (ArrayList) orderDetail.get("discountInfo");
//            BigDecimal totalDiscountValue = BigDecimal.ZERO;
//            BigDecimal discountValue = BigDecimal.ZERO;
//            BigDecimal couponValue = BigDecimal.ZERO;
//            if(NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()){
//                for (Map<String, Object> discountInfo : discountInfoList) {
//                    String discountType = discountInfo.getOrDefault("discountType","").toString();
//                    String programLevel = discountInfo.getOrDefault("programLevel","").toString();
//                    if (programLevel.equalsIgnoreCase(COUPON)) {
//                        couponValue = couponValue.add(BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT, 0.0f).toString())));
//                    } else if(!programLevel.equalsIgnoreCase(COUPON) && !(discountType.equalsIgnoreCase("item") || discountType.equalsIgnoreCase("item_each"))) {
//                        discountValue = discountValue.add(BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT, 0.0f).toString())));
//                    }
//                }
//            }
//            totalDiscountValue = discountValue.add(couponValue);
//            discountValue = discountValue.setScale(2,RoundingMode.HALF_UP);
//            couponValue = couponValue.setScale(2,RoundingMode.HALF_UP);
//            orderDetail.put("totalDiscountValue",totalDiscountValue);
//            orderDetail.put("couponValue",couponValue);
//            orderDetail.put("discountValue",discountValue);
//        });
//    }
//}
//
