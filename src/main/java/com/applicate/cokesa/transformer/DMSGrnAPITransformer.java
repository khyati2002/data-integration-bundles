package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.impl.*;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.SalesService;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.jooq.impl.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

/**
 * DMSGrnAPITransformer
 */
public class DMSGrnAPITransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    private final Logger logger = LoggerFactory.getLogger(DMSGrnAPITransformer.class);
    private static final String SUPPLIER = "supplier";
    private static final String SALES_DETAILS = "salesDetails";
    private static final String DISCOUNT_INFO = "discountInfo";
    private final SalesService salesService = (SalesService) ServiceLocator.lookup(Sales.class);
    private final OutletDetailsService outletDetailsService =(OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
    private final UserService userService = (UserService) ServiceLocator.lookup(com.salescode.dim.jooq.impl.User.class);
    private static final String FEATURES = "features";
    private static final String SALESDETAILS = "salesDetails";
    private static final List<String> EXTRA_FIELDS = List.of("grnNumber", "grnRejectionReason", "grnStatus", "orderStatus");

    @Override
    public Map<String, Object> transform(Map<String, Object> dataMap) {
        ObjectMapper mapper = new ObjectMapper();
        if (NullUtils.isNotNull(dataMap)) {
            try {
                ArrayList<Map<String, Object>> features = mapper.convertValue(dataMap.get(FEATURES), new TypeReference<ArrayList<Map<String, Object>>>() {
                });
                ArrayList<Map<String, Object>> result = new ArrayList<>();
                Map<String, Sales> resultMap = new HashMap<>();
                Map<String, Sales> referencedSales = new HashMap<>();
                Map<String, Map<String, Object>> extraFields = new HashMap<>();
                Map<String, Sales> restSales = new HashMap<>();
                for(Map<String, Object> salesD : features) {
                    Map<String, Object> salesEx = new HashMap<>();
                    for (String field : EXTRA_FIELDS) {
                        if (salesD.containsKey(field)) {
                            salesEx.put(field, Optional.ofNullable(salesD.get(field)).orElse(""));
                        }
                    }
                    Sales sales = JSONUtils.getObjectMapper().convertValue(salesD, Sales.class);
                    extraFields.put(sales.getInvoiceNumber(), salesEx);
                    if(sales.getReferenceNumber() != null) {
                        referencedSales.put(sales.getReferenceNumber(), sales);
                    } else {
                        restSales.put(sales.getInvoiceNumber(), sales);
                    }
                }
                for(Sales sa : restSales.values()) {
                    if(referencedSales.containsKey(sa.getInvoiceNumber())) {
                        salesService.calculateCombinedSales(sa, referencedSales.get(sa.getInvoiceNumber()));
                        resultMap.put(sa.getInvoiceNumber(), sa);
                    } else {
                        resultMap.put(sa.getInvoiceNumber(), sa);
                    }
                }
                ArrayList<Map<String, Object>> features1 = mapper.convertValue(resultMap.values(), new TypeReference<ArrayList<Map<String, Object>>>() {
                });
                for (Map<String, Object> feature : features1) {
                    try {
                        feature = getGenerateInvoiceResponse(feature);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                    String invoice_number = Optional.ofNullable(feature.get("invoiceNumber")).orElse("").toString();
                    ArrayList<Map<String, Object>> orderDetails = mapper.convertValue(feature.get(SALESDETAILS), new TypeReference<ArrayList<Map<String, Object>>>() {
                    });
                    if(extraFields.containsKey(invoice_number)) {
                        feature.putAll(extraFields.get(invoice_number));
                    }
                    if(referencedSales.containsKey(invoice_number) && referencedSales.get(invoice_number).getSalesDetails() != null) {
                        populateRejectedQuantities(orderDetails, referencedSales.get(invoice_number).getSalesDetails());
                    }
                    feature.put(SALESDETAILS, orderDetails);
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
        return dataMap;
    }

    private void populateRejectedQuantities(ArrayList<Map<String, Object>> orderDetails, List<SalesDetails> skudetails) {
        for(Map<String, Object> sku : orderDetails) {
            String batchCode = Optional.ofNullable(sku.get("batchCode")).orElse("").toString();
            if(batchCode.isBlank()) continue;
            SalesDetails reCalculated = skudetails.stream().filter(s -> batchCode.equalsIgnoreCase(s.getBatchCode())).findFirst().orElse(null);
            if(reCalculated == null) continue;
            int reCaseQuantity = (int) Math.floor(Optional.ofNullable(reCalculated.getInitialCaseQuantity()).orElse(0f));
            int rePieceQuantity = (int) Math.floor(Optional.ofNullable(reCalculated.getInitialPieceQuantity()).orElse(0f));
            int reOthersQuantity = (int) Math.floor(Optional.ofNullable(reCalculated.getInitialOtherUnitQuantity()).orElse(0f));
            sku.put("rejectedCaseQuantity", Math.abs(reCaseQuantity));
            sku.put("rejectedPieceQuantity", Math.abs(rePieceQuantity));
            sku.put("rejectedOtherUnitQuantity", Math.abs(reOthersQuantity));
        }
    }

    public Map<String, Object> getGenerateInvoiceResponse(Map<String, Object> json) {
        // Null checks for required fields
        if (!json.containsKey("outletCode") || !json.containsKey(SUPPLIER)) {
            throw new IllegalArgumentException("Missing required fields in input JSON");
        }

        // Get outlet details using outletCode
        OutletDetails outletDetails = outletDetailsService.findByOutletCode(json.get("outletCode").toString());
        json.put("outletName", outletDetails != null ? outletDetails.getOutletName() : null);

        // Get supplier details using the supplier login ID
        User user = userService.findByLoginId(json.get(SUPPLIER).toString());
        json.put("supplierName", user != null ? user.getName() : null);

        json.put("totalAmount", Optional.ofNullable(json.get("initialAmount")).orElse(0.0));

        if(json.containsKey(DISCOUNT_INFO) && json.get(DISCOUNT_INFO) != null && !json.get(DISCOUNT_INFO).toString().equalsIgnoreCase("null")) {
            List<Map<String, Object>> discountInfo = (List<Map<String, Object>>) Optional.ofNullable(json.get(DISCOUNT_INFO)).orElse(JSONUtils.getObjectMapper().createArrayNode());
            Optional<String> discount = Optional.ofNullable(discountInfo)
                    .flatMap(info -> StreamSupport.stream(info.spliterator(), false)
                            .filter(node -> "Summary".equals(
                                    Optional.ofNullable(node.get("discountType"))
                                            .orElse("")))
                            .map(node -> Optional.ofNullable(node.get("discount"))
                                    .orElse("").toString())
                            .findFirst());
            discount.ifPresent(s -> json.put("discountValue", s));
        }

        List<Map<String, Object>> salesDetails = (List<Map<String, Object>>) json.get(SALES_DETAILS);

        AtomicReference<Double> totalCaseQuantity = new AtomicReference<>(0d);
        AtomicReference<Double> totalPieceQuantity = new AtomicReference<>(0d);
        AtomicReference<Double> totalOtherQuantity = new AtomicReference<>(0d);
        AtomicReference<Integer> lineCount = new AtomicReference<>(0);

        // Calculate total quantities by iterating over sales details
        salesDetails.forEach(salesDetail -> {
            double initialCaseQuantity = getSafeDoubleValue(salesDetail, "initialCaseQuantity");
            double initialPieceQuantity = getSafeDoubleValue(salesDetail, "initialPieceQuantity");
            double initialOtherQuantity = getSafeDoubleValue(salesDetail, "initialOtherUnitQuantity");

            if(initialCaseQuantity + initialPieceQuantity + initialOtherQuantity > 0) {
                lineCount.updateAndGet(v -> v + 1);
            }

            totalCaseQuantity.updateAndGet(v -> v + initialCaseQuantity);
            totalPieceQuantity.updateAndGet(v -> v + initialPieceQuantity);
            totalOtherQuantity.updateAndGet(v -> v + initialOtherQuantity);
        });

        json.put("totalCaseQuantity", totalCaseQuantity.get());
        json.put("totalPieceQuantity", totalPieceQuantity.get());
        json.put("totalOtherUnitQuantity", totalOtherQuantity.get());
        json.put("lineCount", lineCount.get());

        return json;
    }

    private double getSafeDoubleValue(Map<String,Object> node, String fieldName) {
        return node.containsKey(fieldName) ? Double.parseDouble(node.get(fieldName).toString()) : 0d;
    }
}
