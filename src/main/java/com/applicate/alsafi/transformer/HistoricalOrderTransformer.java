package com.applicate.alsafi.transformer;

import com.salescode.dim.etl.transformation.service.DataTransformationService.TransformationException;
//import com.applicate.services.channelkart.services.ScoreProgramService;
//import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.SupplierInfoService;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.text.SimpleDateFormat;
import java.util.*;

public class HistoricalOrderTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final String ORDER_ID = "orderid";
    private static final String VALUE = "value";
    private static final String ORDER_DATE = "orderDate";
    private static final String SALES_REP_ID = "salesrepId";
    private static final String OUTLET_ID = "outletId";
    private static final String SKU_CODE = "skuCode";
    private static final String TOTAL_SKU_ORDERED = "totalSkuOrdered";
    private static final String NET_AMOUNT = "netAmount";
    private static final SimpleDateFormat INPUT_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
    private static final SimpleDateFormat OUTPUT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {{
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Extract order ID
            String orderId = String.valueOf(inputMap.getOrDefault(ORDER_ID, ""));
            if (orderId.isEmpty()) {
                throw new IllegalArgumentException("Missing orderId in input data");
            }

            // Parse the "value" field into a list of items
            String jsonString = String.valueOf(inputMap.getOrDefault(VALUE, "[]"));
            List<Map<String, Object>> orderItems = objectMapper.readValue(
                    jsonString,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            if (orderItems.isEmpty()) {
                throw new IllegalArgumentException("No order items found in input data");
            }

            // Initialize transformed order data
            Map<String, Object> transformedOrders = new HashMap<>();
            List<Map<String, Object>> orderDetails = new ArrayList<>();
            double totalSku = 0.0;
            double totalAmount = 0.0;
            Map<String, Object> firstOrder = orderItems.get(0);
            String orderedDate = String.valueOf(firstOrder.get(ORDER_DATE));
            String salesRep = String.valueOf(firstOrder.get(SALES_REP_ID));
            String supplier = String.valueOf(firstOrder.get("supplier"));
            String outletCode  = String.valueOf(firstOrder.get(OUTLET_ID));
            String date =formatOrderDate(orderedDate);
            // Process each order item
            for (Map<String, Object> order : orderItems) {
                Map<String, Object> orderDetail = new HashMap<>();

                String skuCode = null;
                if (order.get(SKU_CODE)!=null)
                    skuCode = String.valueOf(order.get(SKU_CODE));
                else
                    continue;
                double netAmount = Double.parseDouble(String.valueOf(order.get(NET_AMOUNT)));
                double skuQuantity = Double.parseDouble(String.valueOf(order.get(TOTAL_SKU_ORDERED)));

                totalSku += skuQuantity;
                totalAmount += netAmount;

                orderDetail.put("skuCode",skuCode);
                orderDetail.put("batchCode",skuCode);
                orderDetail.put("creationTime",date);
                orderDetail.put("orderNumber",orderId);
                orderDetail.put("source","integration_user_2");
                orderDetail.put("pieceQuantity", skuQuantity);
                orderDetail.put("supplierHierarchy",supplier );
                orderDetail.put("normalizedQuantity", skuQuantity);
                orderDetail.put("billAmount", netAmount);
                orderDetail.put("caseQuantity", 0);
                orderDetail.put("initialAmount", netAmount);
                orderDetail.put("initialCaseQuantity", 0);
                orderDetail.put("initialNormalizedQuantity", skuQuantity);
                orderDetail.put("initialOtherUnitQuantity", 0);
                orderDetail.put("initialPieceQuantity", skuQuantity);
                orderDetail.put("initialQuantity", skuQuantity);
                orderDetail.put("netAmount", netAmount);
                orderDetail.put("normalizedVolume", skuQuantity);
                orderDetail.put("otherUnitPrice",0);
                orderDetail.put("price",0);
                orderDetail.put("salesQuantity",skuQuantity);
                orderDetail.put("salesValue",netAmount);
                orderDetail.put("casePrice",0);
                orderDetail.put("otherUnitQuantity",0);
                orderDetail.put("mrp",0);
                orderDetails.add(orderDetail);
            }

            // Get order metadata from the first item


            // Build final response
            transformedOrders.put("orderNumber", orderId);
            transformedOrders.put("orderDetails", orderDetails);
            transformedOrders.put("totalQuantity", totalSku);
            transformedOrders.put("source", "Hist");
            transformedOrders.put("netAmount", totalAmount);
            transformedOrders.put("loginId", salesRep);
            transformedOrders.put("creationTime", date);
            transformedOrders.put("outletCode",outletCode);
            transformedOrders.put("salesDate",date );
            transformedOrders.put("locationHierarchy","India" );
            transformedOrders.put("billAmount",totalAmount);
            transformedOrders.put("initialNormalizedQuantity",totalSku);
            transformedOrders.put("normalizedVolume",totalSku);
            transformedOrders.put("normalizedQuantity",totalSku);
            transformedOrders.put("totalAmount",totalAmount);
            transformedOrders.put("totalInitialAmt",totalAmount);
            transformedOrders.put("totalInitialQuantity",totalAmount);
            transformedOrders.put("supplierHierarchy",supplier);
            transformedOrders.put("totalMrp",totalAmount);
            transformedOrders.put("salesValue",totalAmount);
            transformedOrders.put("supplier",supplier);

            return transformedOrders;

        } catch (Exception e) {
            throw new IllegalArgumentException("Error processing input data: " + e.getMessage());
        }
    }
    private String formatOrderDate(String dateString) {
        try {
            if (dateString == null || dateString.isEmpty()) {
                throw new TransformationException("null date received");
            }

            // Parse the input date
            java.util.Date date = INPUT_DATE_FORMAT.parse(dateString);

            // Format to the required output format
            return OUTPUT_DATE_FORMAT.format(date);
        } catch (Exception e) {
            // If there's any parsing error, return the original string
            throw new TransformationException("Error Parsing Date");
        }
    }
}