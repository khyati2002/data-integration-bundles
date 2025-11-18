package com.applicate.niine.transformer;

//import com.applicate.services.channelkart.models.CommonDataModel;
//import com.applicate.services.channelkart.models.GRNInfo;
//import com.applicate.services.channelkart.models.Order;
//import com.applicate.services.channelkart.models.Sales;
//import com.applicate.services.channelkart.models.SalesDetails;
//import com.applicate.services.channelkart.models.enums.GRNStatus;
//import com.applicate.services.channelkart.services.OrderService;
//import com.applicate.services.channelkart.services.SalesService;
//import com.applicate.services.channelkart.services.SpringContext;
//import com.applicate.services.channelkart.transformers.AbstractTransformer;
//import com.applicate.services.channelkart.utils.EnrichmentNumber;
//import com.applicate.services.channelkart.utils.JSONUtils;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StockSkuTransformer
 * Transforms data from GRNInfo ( extended CommonDataModel ) to
 * PrimaryOrderRequest. This class extracts relevant details from
 * the input data, including invoice number, GRN number, GRN date, and SKU
 * details with batch codes and
 * normalized quantities.
 *
 * @author abhay.thakur@salescode.ai
 * @since 7/8/24
 */
public class StockSkuTransformer extends AbstractTransformer<CommonDataModel, ObjectNode> {

    private static final Logger logger = LoggerFactory.getLogger(StockSkuTransformer.class);
    private static final String SUPPLIER = "supplier";
    private static final String OUTLET_CODE = "outletCode";
    private static final String SKU_CODE = "skuCode";
    private static final String GRN_NUMBER = "grnNumber";
    private static final DateTimeFormatter INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ISO_INSTANT; // Ensures UTC format

    private final SalesService salesService = SpringContext.getBean(SalesService.class);
    private final EnrichmentNumber enrichmentNumber = SpringContext.getBean(EnrichmentNumber.class);
    private final OrderService orderService = SpringContext.getBean(OrderService.class);

    @Override
    public ObjectNode transform(CommonDataModel commonData) {
        ObjectNode resultNode = JSONUtils.getObjectMapper().createObjectNode();

        try {
            JsonNode jsonNode = parseRequestBody(JSONUtils.stringify(commonData));

            // Extract 'grnNumber', 'grnDate', and 'outletCode' from top-level nodes
            String grnNumber = getFieldValueAsText(jsonNode, GRN_NUMBER);
            String formattedGrnDate = convertDateFormat(getFieldValueAsText(jsonNode, "lastModifiedTime"));

            // Extract sales, order, orderId, and warehouseId
            String invoiceNumber = ((GRNInfo) commonData).getInvoiceNumber();
            String grnStatus = ((GRNInfo) commonData).getGrnStatus();
            Sales sales = salesService.fetchByInvoiceNumber(invoiceNumber);
            Optional<Sales> reCalculatedSales = salesService.findByReferenceNumber(invoiceNumber);
            String orderNumber = sales.getOrderNumber();
            Order order = orderService.findByOrderNumber(orderNumber);
            String orderId = order == null ? sales.getId() : order.getId();

            // Extract SKU details
            List<SalesDetails> salesDetailsNode = sales.getSalesDetails().stream().filter(s -> !"ars".equalsIgnoreCase(s.getSource())).collect(Collectors.toList());

            ObjectNode[] skuDetails = {};
            ObjectNode[] rejectedSkuDetails = {};

            if(GRNStatus.REJECTED.name().equalsIgnoreCase(grnStatus)) {
                String rejectionReason = ((GRNInfo) commonData).getRejectionReason();
                rejectedSkuDetails = salesDetailsNode.stream()
                        .map(sdn -> {
                            ObjectNode node = createSkuNode(sdn, null, false);
                            node.put("reason", Optional.ofNullable(rejectionReason).orElse(""));
                            return node;
                        })
                        .toArray(ObjectNode[]::new);
            } else if((GRNStatus.ACCEPTED.name().equalsIgnoreCase(grnStatus) || GRNStatus.PARTIALLY_REJECTED.name().equalsIgnoreCase(grnStatus)) && reCalculatedSales.isPresent() && reCalculatedSales.get().getSalesDetails() != null && !reCalculatedSales.get().getSalesDetails().isEmpty()) {
                Map<SalesDetails, SalesDetails> salesDetailsMap = salesService.getSalesDetailsMap(salesDetailsNode, reCalculatedSales.get().getSalesDetails().stream().filter(s -> !"ars".equalsIgnoreCase(s.getSource())).collect(Collectors.toList()));
                List<SalesDetails> unChangedSalesDetails = new ArrayList<>();

                for (SalesDetails sd : salesDetailsNode) {
                    boolean exists = salesDetailsMap.values().stream()
                            .anyMatch(s -> sd.getBatchCode().equalsIgnoreCase(s.getBatchCode()));

                    if (!exists) {
                        unChangedSalesDetails.add(sd);
                    }
                }
                ObjectNode[] unChangedSalesDetailsObjNode = unChangedSalesDetails.stream()
                        .map(sdm -> createSkuNode(sdm, null, false))
                        .toArray(ObjectNode[]::new);

                ObjectNode[] skuDetailsObjNode = salesDetailsMap.entrySet().stream()
                        .map(sdm -> createSkuNode(sdm.getKey(), sdm.getValue(), false))
                        .toArray(ObjectNode[]::new);

                skuDetails = Stream.concat(Arrays.stream(unChangedSalesDetailsObjNode), Arrays.stream(skuDetailsObjNode))
                        .toArray(ObjectNode[]::new);

                rejectedSkuDetails = salesDetailsMap.values().stream()
                        .map(sku -> createSkuNode(sku, null, true))
                        .toArray(ObjectNode[]::new);
            } else {
                skuDetails = salesDetailsNode.stream()
                        .map(sdn -> createSkuNode(sdn, null, false))
                        .toArray(ObjectNode[]::new);

            }

            String supplier = Optional.ofNullable(sales.getOutletCode())
                    .filter(code -> !code.isBlank())
                    .orElse(null);

            // Construct the final result node
            resultNode.put("orderId", orderId)
                    .put("warehouseId", "")
                    .put("requestId", grnNumber)
                    .put(SUPPLIER, supplier)
                    .putNull(OUTLET_CODE)
                    .put(GRN_NUMBER, "")
                    .put("grnDate", formattedGrnDate);

            resultNode.putArray("acceptedSkuDetails").addAll(Arrays.asList(skuDetails));
            resultNode.putArray("rejectedSkuDetails").addAll(Arrays.asList(rejectedSkuDetails));

        } catch (Exception e) {
            // Log the error and return a default PrimaryOrderRequest with empty details
            logger.error("Error transforming CommonDataModel to PrimaryOrderRequest: {}", e.getMessage());
            resultNode = JSONUtils.getObjectMapper().createObjectNode();
        }

        return resultNode;
    }

    /**
     * Retrieves the value of a specified field from a JSON node as a String.
     * This method checks if the given field exists within the provided JSON object.
     * If the field is present, it returns its value as a String. If the field is
     * not found, it returns null.
     *
     * @param obj       the JsonNode object from which to retrieve the field value
     * @param fieldName the name of the field whose value is to be extracted
     * @return the field value as a String if it exists, or null if it does not
     */
    private String getFieldValueAsText(JsonNode obj, String fieldName) {
        return obj.path(fieldName).asText(null);
    }

    /**
     * Converts a JSON string to an ObjectNode for further processing.
     *
     * @param jsonBody JSON string to be parsed
     * @return Parsed ObjectNode
     * @throws IOException If there is an error parsing the JSON string
     */
    private ObjectNode parseRequestBody(String jsonBody) throws IOException {
        return (ObjectNode) JSONUtils.getObjectMapper().readTree(jsonBody);
    }

    /**
     * Converts a date string from the input format to the output format.
     * This method parses the input date string using the INPUT_FORMATTER
     * and then formats it using the OUTPUT_FORMATTER.
     *
     * @param dateStr the date string to be converted
     * @return the converted date string in the output format
     * @throws DateTimeParseException if the input date string cannot be parsed
     */
    private String convertDateFormat(String dateStr) throws DateTimeParseException {
        LocalDateTime localDateTime = LocalDateTime.parse(dateStr, INPUT_FORMATTER);
        ZonedDateTime utcDateTime = localDateTime.atZone(ZoneId.systemDefault()) // Convert from system time zone
                .withZoneSameInstant(ZoneId.of("UTC")); // Convert to UTC
        return utcDateTime.format(OUTPUT_FORMATTER);
    }

    /**
     * Creates an ObjectNode representing a SKU (Stock Keeping Unit) from a
     * JsonNode.
     * This method extracts relevant information from the input JsonNode and
     * constructs a new ObjectNode with SKU details.
     *
     * @param detailNode the JsonNode containing SKU details
     * @return an ObjectNode with SKU information including SKU code, quantity,
     *         batch ID, and expiry date
     */
    private ObjectNode createSkuNode(SalesDetails detailNode, SalesDetails reCalculated, boolean includeRejectionReason) {
        String batchCode = detailNode.getBatchCode();
        int currCaseQuantity = (int) Math.floor(Optional.ofNullable(detailNode.getInitialCaseQuantity()).orElse(0f));
        int currPieceQuantity = (int) Math.floor(Optional.ofNullable(detailNode.getInitialPieceQuantity()).orElse(0f));
        int currOthersQuantity = (int) Math.floor(Optional.ofNullable(detailNode.getInitialOtherUnitQuantity()).orElse(0f));

        if(reCalculated != null) {
            int reCaseQuantity = (int) Math.floor(Optional.ofNullable(reCalculated.getInitialCaseQuantity()).orElse(0f));
            int rePieceQuantity = (int) Math.floor(Optional.ofNullable(reCalculated.getInitialPieceQuantity()).orElse(0f));
            int reOthersQuantity = (int) Math.floor(Optional.ofNullable(reCalculated.getInitialOtherUnitQuantity()).orElse(0f));

            currCaseQuantity += reCaseQuantity;
            currPieceQuantity += rePieceQuantity;
            currOthersQuantity += reOthersQuantity;
        }

        // NOTE: to convert recalculated quantities into +ve
        currCaseQuantity = Math.abs(currCaseQuantity);
        currPieceQuantity = Math.abs(currPieceQuantity);
        currOthersQuantity = Math.abs(currOthersQuantity);

        int totalQuantity = currCaseQuantity + currPieceQuantity + currOthersQuantity;

//        String randomBatchId = enrichmentNumber.getEnrichmentNumber("batchId", "randomBatchId", "batchIdSequence", "randomBatchIdPattern", new HashMap<>());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusMonths(3);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDate = futureDate.format(formatter);

        ObjectNode res = JSONUtils.getObjectMapper().createObjectNode()
                .put(SKU_CODE, batchCode)
                .put("caseQty", currCaseQuantity)
                .put("pieceQty", currPieceQuantity)
                .put("otherQty", currOthersQuantity)
                .put("totalQty", totalQuantity)
                .put("batchId", "unassigned") // currently dummy value for demo
                .put("shelfLife", 0) // currently dummy value for demo
                .putNull("mfgDate")
                .put("expiryDate", convertDateFormat(formattedDate));
        if(includeRejectionReason) {
            res.put("reason", detailNode.getRemarks());
        }
        return res;
    }
}