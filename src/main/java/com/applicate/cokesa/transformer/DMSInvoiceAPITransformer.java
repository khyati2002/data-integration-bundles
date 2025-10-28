package com.applicate.cokesa.transformer;

import com.salescode.dim.jooq.impl.AccountInfo;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.services.AccountInfoService;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * DMSInvoiceAPITransformer
 */
public class DMSInvoiceAPITransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    private final Logger logger = LoggerFactory.getLogger(DMSInvoiceAPITransformer.class);
    private static final String FEATURES = "features";
    private static final String CASH_DISCOUNT_ID = "cashDiscount";
    private static final String DISCOUNT_NEW = "finalBenefit";
    public static final String COUPON = "coupon";
    private static final String DISCOUNT = "discount";
    private UserService userService = (UserService) ServiceLocator.lookup(User.class);
    public static final String SALESDETAILS = "salesDetails";

    private final AccountInfoService accountInfoService =(AccountInfoService) ServiceLocator.lookup(AccountInfo.class);

    @Override
    public Map<String, Object> transform(Map<String, Object> dataMap) {
        ObjectMapper mapper = new ObjectMapper();
        if (NullUtils.isNotNull(dataMap)) {
            try {
                ArrayList<Map<String, Object>> features = mapper.convertValue(dataMap.get(FEATURES), new TypeReference<ArrayList<Map<String, Object>>>() {
                });
                ArrayList<Map<String, Object>> result = new ArrayList<>();
                for (Map<String, Object> feature : features) {
                    try {
                        String salR = String.valueOf(feature.getOrDefault("loginId", ""));
                        User user = userService.findByLoginId(salR);
                        feature.put("salesRepId", salR);
                        feature.put("salesRepName", user.getName());
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                    separateCouponAndDiscountOnOrder(feature);
                    transformSalesNode(feature);
                    ArrayList<Map<String, Object>> orderDetails = mapper.convertValue(feature.get(SALESDETAILS), new TypeReference<ArrayList<Map<String, Object>>>() {
                    });
                    separateCouponAndDiscountOnOrderDetails(orderDetails);
                    feature.put(SALESDETAILS, orderDetails);
                    result.add(feature);
                }
                dataMap.put(FEATURES, result);
                return dataMap;
            } catch (Exception ex) {
                logger.error("Transformer Exception", ex);
            }
        } else {
            throw new NullPointerException("Either  specification/input json found null");
        }
        return dataMap;
    }

    private void transformSalesNode(Map<String, Object> salesNode) {
        populateSalesNode(salesNode);
    }

    private void populateSalesNode(Map<String, Object> salesNode) {
        String outletCode = salesNode.get("outletCode").toString();
        if(outletCode != null && !outletCode.isEmpty()) {
            salesNode.put("hasGstin", isGstNoPresent(outletCode));
        }
    }

    private void separateCouponAndDiscountOnOrder(Map<String, Object> orderMap) {
        ArrayList<Map<String, Object>> discountInfoList = (ArrayList) orderMap.get("discountInfo");
        BigDecimal totalDiscountValue = BigDecimal.ZERO;
        BigDecimal discountValue;
        BigDecimal couponValue = BigDecimal.ZERO;
        BigDecimal additionalDistributorDiscount=BigDecimal.ZERO;
        if (NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()) {
            for (Map<String, Object> discountInfo : discountInfoList) {
                String discountPromoType = discountInfo.getOrDefault("promoType", "").toString();
                boolean tov = Optional.ofNullable(discountInfo.get("promoAttribute"))
                        .filter(List.class::isInstance)
                        .map(obj -> (List<?>) obj)
                        .orElse(List.of()) // Handle the case where promoAttributes is not a List or is null
                        .stream()
                        .filter(Objects::nonNull)
                        .anyMatch(promo -> "tov".equalsIgnoreCase(promo.toString()));
                String discountType = discountInfo.getOrDefault("discountType", "").toString();
                String discountId = discountInfo.getOrDefault("id", "").toString();
                BigDecimal discount = BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT, 0).toString()));
                if (discountType.equalsIgnoreCase("Summary") || discountPromoType.equalsIgnoreCase("Summary")) {
                    totalDiscountValue = discount;
                } else if (!(discountType.equalsIgnoreCase("item") || discountType.equalsIgnoreCase("item_each")) && (tov)) {
                    couponValue = couponValue.add(BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault("actualBenefit", 0).toString())));
                }else if(discountId.equalsIgnoreCase(CASH_DISCOUNT_ID)){
                    additionalDistributorDiscount = BigDecimal.valueOf(
                            Optional.ofNullable(discountInfo.get("discount"))
                                    .map(Object::toString)
                                    .map(Double::parseDouble)
                                    .orElse(0.0)
                    );
                }
            }
        }
        discountValue = totalDiscountValue.subtract(couponValue).subtract(additionalDistributorDiscount).setScale(2, RoundingMode.HALF_UP);
        couponValue = couponValue.setScale(2, RoundingMode.HALF_UP);
        totalDiscountValue = totalDiscountValue.setScale(2, RoundingMode.HALF_UP);
        orderMap.put("totalDiscountValue", totalDiscountValue);
        orderMap.put("couponValue", couponValue);
        orderMap.put("discountValue", discountValue);
        orderMap.put("additionalDistributorDiscount", additionalDistributorDiscount);
    }

    private void separateCouponAndDiscountOnOrderDetails(ArrayList<Map<String, Object>> orderDetails) {
        if(orderDetails == null || orderDetails.isEmpty()) return;
        orderDetails.forEach(orderDetail -> {
            ArrayList<Map<String, Object>> discountInfoList = (ArrayList) orderDetail.get("discountInfo");
            BigDecimal totalDiscountValue = BigDecimal.ZERO;
            BigDecimal discountValue = BigDecimal.ZERO;
            BigDecimal couponValue = BigDecimal.ZERO;
            if (NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()) {
                for (Map<String, Object> discountInfo : discountInfoList) {
                    String discountPromoType = discountInfo.getOrDefault("promoType", "").toString();
                    String discountType = discountInfo.getOrDefault("discountType", "").toString();
                    String programLevel = discountInfo.getOrDefault("programLevel", "").toString();
                    if(!discountType.equalsIgnoreCase("item") && !discountType.equalsIgnoreCase("item_each") && !discountPromoType.equalsIgnoreCase("item") && !discountPromoType.equalsIgnoreCase("item_each")) {
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


    private boolean isGstNoPresent(String outletCode) {
        AccountInfo accountInfo = accountInfoService.findByLoginId(outletCode);
        if(accountInfo == null) {
            return false;
        }
        String gstin = accountInfoService.decryptAccount(accountInfo).getGstin();
        return gstin != null && !gstin.isEmpty() && !gstin.isBlank();
    }
}
