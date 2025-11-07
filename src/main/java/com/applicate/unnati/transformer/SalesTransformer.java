package com.applicate.unnati.transformer;


import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalesTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    private final Logger logger = LoggerFactory.getLogger(SalesTransformer.class);
    private static final String FEATURES = "feature";
    private static final String SALES_DETAILS = "salesDetails";
    private static final String DISCOUNT_INFO = "discountInfo";
    private static final String CASH_DISCOUNT_ID = "cashDiscount";
    private static final String DISCOUNT = "discount";
    private static final String DISCOUNT_NEW = "finalBenefit";
    public static final String COUPON = "coupon";

    @Override
    public Map<String, Object> transform(Map<String, Object> dataMap) {
        ObjectMapper mapper = new ObjectMapper();
        if (NullUtils.isNotNull(dataMap)) {
            try {
                ArrayList<Map<String, Object>> features = mapper.convertValue(dataMap.get(FEATURES),
                        new TypeReference<ArrayList<Map<String, Object>>>() {});
                ArrayList<Map<String, Object>> result = new ArrayList<>();

                for (Map<String, Object> feature : features) {
                    try {
                        String loginId = String.valueOf(feature.getOrDefault("loginId", ""));
                        feature.put("salesRepId", loginId);

                        feature.put("totalAmount", feature.getOrDefault("initialAmount", 0.0));
                    } catch (Exception e) {
                        logger.error("Error populating user/outlet data: {} ", e.getMessage());
                    }

                    transformSalesNode(feature);
                    separateCouponAndDiscountOnOrder(feature);

                    if (feature.containsKey(SALES_DETAILS)) {
                        ArrayList<Map<String, Object>> salesDetails = mapper.convertValue(
                                feature.get(SALES_DETAILS),
                                new TypeReference<ArrayList<Map<String, Object>>>() {});

                        float lineCount = salesDetails.stream()
                                                  .filter(detail -> Float.parseFloat(detail.get("initialNormalizedQuantity").toString()) > 0)
                                                  .count();

                        separateCouponAndDiscountOnSalesDetails(salesDetails);
                        feature.put(SALES_DETAILS, salesDetails);
                        feature.put("lineCount", lineCount);
                    }

                    result.add(feature);
                }

                dataMap.put(FEATURES, result);
                return dataMap;
            } catch (Exception ex) {
                logger.error("Transformer Exception", ex);
            }
        } else {
            throw new NullPointerException("Either specification/input json found null");
        }

        Object discountInfo = dataMap.get("discountInfo");
        if (discountInfo instanceof List) {
            try {
                dataMap.put("discountInfo", new ObjectMapper().writeValueAsString(discountInfo));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return dataMap;
    }

    private void transformSalesNode(Map<String, Object> salesNode) {
        populateSalesNode(salesNode);
    }

    private void populateSalesNode(Map<String, Object> salesNode) {
        String outletCode = salesNode.get("outletCode").toString();
        if (outletCode != null && !outletCode.isEmpty()) {
            salesNode.put("hasGstin", isGstNoPresent(outletCode));
        }
    }

    private void separateCouponAndDiscountOnOrder(Map<String, Object> salesMap) {
        ArrayList<Map<String, Object>> discountInfoList = (ArrayList) salesMap.get(DISCOUNT_INFO);
        BigDecimal totalDiscountValue = BigDecimal.ZERO;
        BigDecimal discountValue;
        BigDecimal couponValue = BigDecimal.ZERO;
        BigDecimal additionalDistributorDiscount = BigDecimal.ZERO;

        if (NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()) {
            for (Map<String, Object> discountInfo : discountInfoList) {
                String discountPromoType = discountInfo.getOrDefault("promoType", "").toString();
                String discountType = discountInfo.getOrDefault("discountType", "").toString();
                String programLevel = discountInfo.getOrDefault("programLevel", "").toString();
                String discountId = discountInfo.getOrDefault("id", "").toString();
                BigDecimal discount = BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT, 0).toString()));

                if (discountType.equalsIgnoreCase("Summary") || discountPromoType.equalsIgnoreCase("Summary")) {
                    totalDiscountValue = discount;
                } else if (!(discountType.equalsIgnoreCase("item") || discountType.equalsIgnoreCase("item_each"))
                                   && (programLevel.equalsIgnoreCase(COUPON) || programLevel.equalsIgnoreCase("ILC"))) {
                    couponValue = couponValue.add(discount);
                } else if (discountId.equalsIgnoreCase(CASH_DISCOUNT_ID)) {
                    additionalDistributorDiscount = BigDecimal.valueOf(
                            Double.valueOf(discountInfo.getOrDefault(DISCOUNT, 0).toString()));
                }
            }
        }

        discountValue = totalDiscountValue.subtract(couponValue)
                                .subtract(additionalDistributorDiscount)
                                .setScale(2, RoundingMode.HALF_UP);
        couponValue = couponValue.setScale(2, RoundingMode.HALF_UP);
        totalDiscountValue = totalDiscountValue.setScale(2, RoundingMode.HALF_UP);

        salesMap.put("totalDiscountValue", totalDiscountValue);
        salesMap.put("couponValue", couponValue);
        salesMap.put("discountValue", discountValue);
        salesMap.put("additionalDistributorDiscount", additionalDistributorDiscount);
    }

    private void separateCouponAndDiscountOnSalesDetails(ArrayList<Map<String, Object>> salesDetails) {
        if (salesDetails == null || salesDetails.isEmpty()) return;

        salesDetails.forEach(salesDetail -> {
            ArrayList<Map<String, Object>> discountInfoList = (ArrayList) salesDetail.get(DISCOUNT_INFO);
            BigDecimal totalDiscountValue = BigDecimal.ZERO;
            BigDecimal discountValue = BigDecimal.ZERO;
            BigDecimal couponValue = BigDecimal.ZERO;

            if (NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()) {
                for (Map<String, Object> discountInfo : discountInfoList) {
                    String discountPromoType = discountInfo.getOrDefault("promoType", "").toString();
                    String discountType = discountInfo.getOrDefault("discountType", "").toString();
                    String programLevel = discountInfo.getOrDefault("programLevel", "").toString();

                    if (!discountType.equalsIgnoreCase("item")
                                && !discountType.equalsIgnoreCase("item_each")
                                && !discountPromoType.equalsIgnoreCase("item")
                                && !discountPromoType.equalsIgnoreCase("item_each")) {

                        if (programLevel.equalsIgnoreCase(COUPON) || programLevel.equalsIgnoreCase("ILC")) {
                            couponValue = couponValue.add(BigDecimal.valueOf(
                                    Double.valueOf(discountInfo.getOrDefault(DISCOUNT_NEW, 0.0f).toString())));
                        } else {
                            discountValue = discountValue.add(BigDecimal.valueOf(
                                    Double.valueOf(discountInfo.getOrDefault(DISCOUNT_NEW, 0.0f).toString())));
                        }
                    }
                }
            }

            totalDiscountValue = discountValue.add(couponValue);
            discountValue = discountValue.setScale(2, RoundingMode.HALF_UP);
            couponValue = couponValue.setScale(2, RoundingMode.HALF_UP);

            salesDetail.put("totalDiscountValue", totalDiscountValue);
            salesDetail.put("couponValue", couponValue);
            salesDetail.put("discountValue", discountValue);
        });
    }

    private boolean isGstNoPresent(String outletCode) {
        AccountInfo accountInfo = accountInfoService.findByLoginId(outletCode);
        if (accountInfo == null) {
            return false;
        }
        String gstin = accountInfoService.decryptAccount(accountInfo).getGstin();
        return gstin != null && !gstin.isEmpty() && !gstin.isBlank();
    }
}