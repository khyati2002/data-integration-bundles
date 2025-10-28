package com.applicate.cokesa.transformer;

import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class SeparateFreeItemsAndCouponValueGet extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final String FEATURES = "features";
    private static final String ORDERDETAILS = "orderDetails";
    private static final String EINVOICE = "eInvoice";
    private static final String DISCOUNT_NEW = "finalBenefit";
    private static final String DISCOUNT = "discount";
    private static final String COUPON = "coupon";
    private static final String JOINER_STRING = "\", \"";
    private static final String CASH_DISCOUNT_ID = "cashDiscount";
    private static final String PERCENTAGE_DISCOUNT_TYPE = "percentage";
    private static final String DISCOUNT_TYPE = "discountType";
    private static final int DECIMAL_SCALE = 2;
    private static final String DISCOUNT_INFO = "discount_info";
    private Logger logger = LoggerFactory.getLogger(SeparateFreeItemsAndCouponValueGet.class);
    private EntityUtils em;
    private UserService userService = (UserService) ServiceLocator.lookup(User.class);
    private List<String> orderWithInvoice = new ArrayList<>();

    @Override
    public Map<String, Object> transform(Map<String, Object> dataMap) {
        ObjectMapper mapper = new ObjectMapper();
        if (NullUtils.isNotNull(dataMap)) {
            try {
                ArrayList<Map<String, Object>> features = mapper.convertValue(dataMap.get(FEATURES), new TypeReference<ArrayList<Map<String, Object>>>() {
                });
                if (NullUtils.isNotNull(features)) {
                    processFeature(features);
                    ArrayList<Map<String, Object>> result = new ArrayList<>();
                    Map<String, Map<String, Object>> invoiceMap = getOrderInvoiceMap();
                    for (Map<String, Object> feature : features) {
                        try {
                            String salR = String.valueOf(feature.getOrDefault("loginId", ""));
                            User user = userService.findByLoginId(salR);
                            feature.put("salesRepId", salR);
                            feature.put("salesRepName", user.getName());
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                        }
                        addInvoiceData(feature, invoiceMap);
                        separateCouponAndDiscountOnOrder(feature);
                        ArrayList<Map<String, Object>> orderDetails = mapper.convertValue(feature.get(ORDERDETAILS), new TypeReference<ArrayList<Map<String, Object>>>() {
                        });
                        separateCouponAndDiscountOnOrderDetails(orderDetails);
                        feature.put("totalTaxAmount", totalTaxAmount(orderDetails));
                        feature.put(ORDERDETAILS, orderDetails);
                        result.add(feature);
                    }
                    dataMap.put(FEATURES, result);
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

    private void processFeature(List<Map<String, Object>> features) {
        features.stream()
                .filter(feature -> {
                    String status = (String) Optional.ofNullable(feature.get("status")).orElse("");
                    return !(status.equalsIgnoreCase("pending") || status.equalsIgnoreCase("confirmed"));
                })
                .map(feature -> (String) feature.get("orderNumber"))
                .forEach(orderWithInvoice::add);
    }

    private void addInvoiceData(Map<String, Object> feature, Map<String, Map<String, Object>> invoiceMap) {
        String orderNumber = String.valueOf(feature.get("orderNumber"));
        if(invoiceMap.containsKey(orderNumber)) {
            Map<String, Object> orderInvoiceMap = invoiceMap.get(orderNumber);
            String invoiceNumber = String.valueOf(orderInvoiceMap.getOrDefault("invoiceNumber", ""));
            if (!StringUtils.isNullOrBlank(invoiceNumber)) {
                updateFeatureWithInvoiceDetails(feature, orderInvoiceMap);
            }
        }
    }

    private void updateFeatureWithInvoiceDetails(Map<String, Object> feature, Map<String, Object> orderInvoiceMap) {
        feature.put("invoiceNumber", orderInvoiceMap.get("invoiceNumber"));
        feature.put(EINVOICE, orderInvoiceMap.getOrDefault(EINVOICE, "false"));
        feature.put("einvoiceCancelled",orderInvoiceMap.getOrDefault("einvoiceCancelled","false"));

        String type = String.valueOf(feature.getOrDefault("type", ""));
        if ("primary".equalsIgnoreCase(type)) {
            feature.put("grnStatus", getGRNStatus(String.valueOf(orderInvoiceMap.get("invoiceNumber"))));
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
                String discountType = discountInfo.getOrDefault("discountType", "").toString();
                String programLevel = discountInfo.getOrDefault("programLevel", "").toString();
                String discountId = discountInfo.getOrDefault("id", "").toString();
                BigDecimal discount = BigDecimal.valueOf(Double.valueOf(discountInfo.getOrDefault(DISCOUNT, 0).toString()));
                if (discountType.equalsIgnoreCase("Summary") || discountPromoType.equalsIgnoreCase("Summary")) {
                    totalDiscountValue = discount;
                } else if (!(discountType.equalsIgnoreCase("item") || discountType.equalsIgnoreCase("item_each")) && (programLevel.equalsIgnoreCase(COUPON) || programLevel.equalsIgnoreCase("ILC"))) {
                    couponValue = couponValue.add(discount);
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

    private Map<String,Map<String, Object>> getOrderInvoiceMap() {
        if (this.orderWithInvoice == null || orderWithInvoice.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> dynamicValues = new HashMap<>();
        dynamicValues.put("orderNumbers", "\"".concat(String.join(JOINER_STRING, orderWithInvoice)).concat("\""));

        // Build query with proper SQL parameter binding instead of string concatenation
        String query = "select invoice_number, extended_attributes, order_number from ck_sales where order_number in ({{orderNumbers}}) and type != 'return'";
        String finalQuery = StringUtils.replaceDynamicValues(query, (ObjectNode) JSONUtils.toJsonNode(dynamicValues));

        List<Map<String, Object>> queryResult = (List<Map<String, Object>>) em.findDataByQuery(Map.class, finalQuery, true);

        Map<String, Map<String, Object>> result = new HashMap<>();
        queryResult.forEach(s -> convertToInvoiceMap(result, s));

        return result;
    }

    private void convertToInvoiceMap(Map<String, Map<String, Object>> result, Map<String, Object> resultMap) {
        String invoiceNumber = (String) resultMap.getOrDefault("invoice_number", "");
        String orderNumber = (String) resultMap.getOrDefault("order_number", "");
        JsonNode extendedAttributes = getExtendedAttributes(resultMap);

        result.put(orderNumber, Map.of(
                "invoiceNumber", invoiceNumber,
                EINVOICE, Boolean.toString(extendedAttributes.has("IRNDtls")),
                "einvoiceCancelled", Boolean.toString(extendedAttributes.has("CancelIRNDtls"))
        ));
    }

    private JsonNode getExtendedAttributes(Map<String, Object> resultMap) {
        try {
            return JSONUtils.getObjectMapper().readTree(resultMap.get("extended_attributes").toString());
        } catch (Exception e) {
            return JSONUtils.getObjectMapper().createObjectNode();
        }
    }

    private String getGRNStatus(String invoiceNumber) {
        String query = "select grn_status from ck_grn_info where invoice_number='{}'";
        List<?> queryResult = em.findDataByQuery(List.class,
                StringUtils.format(query, invoiceNumber), true);
        String grnStatus = !queryResult.isEmpty() ? (queryResult.get(0)).toString() : "";
        return grnStatus;
    }
}