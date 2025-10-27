package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeparateFreeItemsAndCouponValue extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private static final String FEATURES = "feature";
    private static final String ORDERDETAILS = "orderDetails";
    private static final String DISCOUNT_NEW = "finalBenefit";
    private static final String DISCOUNT = "discount";
    public static final String COUPON = "coupon";
    private Logger logger = LoggerFactory.getLogger(SeparateFreeItemsAndCouponValue.class);

    @Override
    public Map<String, Object> transform(Map<String, Object> dataMap) {
        ObjectMapper mapper = new ObjectMapper();
        if (NullUtils.isNotNull(dataMap)) {
            try {
                ArrayList<Map<String,Object>> features = mapper.convertValue(dataMap.get(FEATURES),new TypeReference<ArrayList<Map<String,Object>>>() {});
                if(NullUtils.isNotNull(features)) {
                    ArrayList<Map<String, Object>> result = new ArrayList<>();
                    Map<String, Object> transformedDataMap = transformOrderBody(features);
                    ArrayList<Map<String, Object>> transformedDataMapFeatures = mapper.convertValue(transformedDataMap.get(FEATURES),new TypeReference<ArrayList<Map<String,Object>>>() {});

                    for(Map<String,Object> feature :transformedDataMapFeatures) {
                        ArrayList<Map<String, Object>> orderDetails = mapper.convertValue(feature.get(ORDERDETAILS), new TypeReference<ArrayList<Map<String, Object>>>() {
                        });
                        separateCouponAndDiscountOnOrder(feature);
                        separateCouponAndDiscountOnOrderDetails(orderDetails);
                        feature.put("totalTaxAmount", totalTaxAmount(orderDetails));
                        feature.put(ORDERDETAILS, orderDetails);
                        result.add(feature);
                        dataMap.put(FEATURES, result);
                    }
                }
                return dataMap;
            } catch (Exception ex) {
                logger.error("Transformer Exception", ex);
            }
        } else {
            throw new NullPointerException("Either  specification/input json found null");
        }
        return new HashMap<>();
    }

    private double totalTaxAmount(ArrayList<Map<String, Object>> transformedDataMapFeatures) {
        BigDecimal totalTaxAmount = BigDecimal.ZERO;
        for(Map<String, Object> od : transformedDataMapFeatures) {
            try {
                Map<String, Object> extendedAttributes = (Map<String, Object>) od.getOrDefault("extendedAttributes", new HashMap<>());
                if (!extendedAttributes.containsKey("taxInfo")) continue;
                List<Map<String, Object>> taxInfo = (List<Map<String, Object>>) extendedAttributes.get("taxInfo");
                if (taxInfo.isEmpty()) continue;
                for (Map<String, Object> tn : taxInfo) {
                    if (tn.containsKey("taxAmount")) {
                        totalTaxAmount = totalTaxAmount.add(BigDecimal.valueOf(Double.parseDouble(String.valueOf(tn.get("taxAmount")))));
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return totalTaxAmount.doubleValue();
    }

    private Map<String, Object> transformOrderBody(ArrayList<Map<String,Object>> features) {
        Map<String,Object> transformerdDataMap = new HashMap<>();
        ArrayList<Map<String,Object>> list= new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        if(NullUtils.isNotNull(features)) {
            for (Map<String, Object> feature : features) {
                ArrayList<Map<String, Object>> orderDetails = mapper.convertValue(feature.get(ORDERDETAILS), new TypeReference<ArrayList<Map<String, Object>>>() {
                });
                feature.put(ORDERDETAILS, orderDetails);
                list.add(feature);
            }
        }
        transformerdDataMap.put(FEATURES,list);
        return transformerdDataMap;
    }

    private void separateCouponAndDiscountOnOrder(Map<String, Object> orderMap) {
        ArrayList<Map<String, Object>> discountInfoList = (ArrayList) orderMap.get("discountInfo");
        BigDecimal totalDiscountValue = BigDecimal.ZERO;
        BigDecimal discountValue;
        BigDecimal couponValue = BigDecimal.ZERO;
        if (NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()) {
            for (Map<String, Object> discountInfo : discountInfoList) {
                String discountType = discountInfo.getOrDefault("discountType", "").toString();
                String programLevel = discountInfo.getOrDefault("programLevel", "").toString();
                if (discountType.equalsIgnoreCase("Summary")) {
                    totalDiscountValue = BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT, 0).toString()));
                } else if (!(discountType.equalsIgnoreCase("item") || discountType.equalsIgnoreCase("item_each")) && (programLevel.equalsIgnoreCase(COUPON) || programLevel.equalsIgnoreCase("ILC"))) {
                    couponValue = couponValue.add(BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT, 0).toString())));
                }
            }
        }
        discountValue = totalDiscountValue.subtract(couponValue).setScale(2, RoundingMode.HALF_UP);
        couponValue = couponValue.setScale(2, RoundingMode.HALF_UP);
        totalDiscountValue = totalDiscountValue.setScale(2, RoundingMode.HALF_UP);
        orderMap.put("totalDiscountValue", totalDiscountValue);
        orderMap.put("couponValue", couponValue);
        orderMap.put("discountValue", discountValue);
    }

    private void separateCouponAndDiscountOnOrderDetails(ArrayList<Map<String, Object>> orderDetails) {
        orderDetails.forEach(orderDetail -> {
            ArrayList<Map<String, Object>> discountInfoList = (ArrayList) orderDetail.get("discountInfo");
            BigDecimal totalDiscountValue = BigDecimal.ZERO;
            BigDecimal discountValue = BigDecimal.ZERO;
            BigDecimal couponValue = BigDecimal.ZERO;
            if (NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()) {
                for (Map<String, Object> discountInfo : discountInfoList) {
                    String discountType = discountInfo.getOrDefault("promoType", "").toString();
                    String programLevel = discountInfo.getOrDefault("programLevel", "").toString();
                    if(!discountType.equalsIgnoreCase("item") && !discountType.equalsIgnoreCase("item_each")) {
                        if (programLevel.equalsIgnoreCase(COUPON) || programLevel.equalsIgnoreCase("ILC")) {
                            couponValue = couponValue.add(BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT_NEW, 0.0f).toString())));
                        } else {
                            discountValue = discountValue.add(BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT_NEW, 0.0f).toString())));
                        }
                    }
                }
            }
            totalDiscountValue = discountValue.add(couponValue);
            discountValue = discountValue.setScale(2, RoundingMode.HALF_UP);
            couponValue = couponValue.setScale(2, RoundingMode.HALF_UP);
            orderDetail.put("totalDiscountValue", totalDiscountValue);
            orderDetail.put("couponValue", couponValue);
            orderDetail.put("discountValue", discountValue);
        });
    }
}