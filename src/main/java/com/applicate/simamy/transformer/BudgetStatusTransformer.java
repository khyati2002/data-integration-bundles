package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.salescode.dim.jooq.generated.Tables.*;

public class BudgetStatusTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private static final Logger logger = LoggerFactory.getLogger(BudgetStatusTransformer.class);
    private final double BUDGET_PERCENTAGE = Double.parseDouble(SpringContext.getBean(SchemeConfigHandler.class).getBudgetPercentageValue());
    private final DSLContext dsl = ServiceLocator.getDslContext();

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        if (inputMap != null && inputMap.containsKey("COST_PRC") && inputMap.containsKey("PRD_CD")) {
            double costPrice = validateNumberField(inputMap.get("COST_PRC"), "cost_price");
            String productCode = validateField(inputMap.get("PRD_CD"), "product_code");

            // Execute FOC flow
            processFOCScheme(productCode, String.valueOf(costPrice));

            // Return early after FOC flow execution
            Map<String, Object> response = new HashMap<>();
            response.put("schemeId", "123");
            return List.of(response);
        }

        // Proceed with the normal flow if FOC conditions are not met

        // Validate required fields
        String promoCode = validateField(inputMap.get("promo_code"), "promo_code");
        String supplier = validateField(inputMap.get("supplier"), "supplier");
        double budget = validateNumberField(inputMap.get("budget"), "budget");
        double balance = validateNumberField(inputMap.get("balance"), "balance");

        // Check the promo code type from the ck_scheme_calculation table
        String promoType = getPromoType(promoCode);

        // Handle 'item' promo type separately
        if ("item".equalsIgnoreCase(promoType)) {
            handleItemPromoType(promoCode, supplier, budget, balance, inputMap);
        } else {
            // Check if balance is less than given % of budget
            if (balance < (BUDGET_PERCENTAGE / 100) * budget) {
                int rowsUpdated = dsl.update(CK_SCHEME_OUTLET_BIFURCATIONS)
                        .set(CK_SCHEME_OUTLET_BIFURCATIONS.ACTIVE_STATUS, ActiveStatus.INACTIVE)
                        .set(CK_SCHEME_OUTLET_BIFURCATIONS.LAST_MODIFIED_TIME, DSL.currentLocalDateTime())
                        .where(CK_SCHEME_OUTLET_BIFURCATIONS.SCHEME_ID.eq(promoCode))
                        .and(CK_SCHEME_OUTLET_BIFURCATIONS.LOGIN_ID.eq(supplier))
                        .execute();

                if (rowsUpdated == 0) {
                    throw new DataTransformationService.TransformationException("No records found to update for promo code '" + promoCode + "' and supplier '" + supplier + "'.");
                }
                logger.info("Updated {} rows to inactive status for promo code {} and supplier {}.", rowsUpdated, promoCode, supplier);
            } else {
                String errorMsg = "No action required for promo code '" + promoCode + "' and supplier '" + supplier + "' as balance is above the threshold.";
                logger.info(errorMsg);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("schemeId", "123");
        return List.of(response);
    }

    private void handleItemPromoType(String promoCode, String supplier, double budget, double balance, Map<String, Object> inputMap) {
        // Get the batch code to cost price mapping
        Map<String, String> priceMap = mapBatchCodeToCostPrice(inputMap);

        // Convert the batch codes to a list for the SQL query
        List<String> batchCodes = new ArrayList<>(priceMap.keySet());

        // Fetch batchCode and qty from ck_stock table
        Result<Record2<String, Double>> stockResults = dsl.select(CK_STOCK.BATCH_CODE, CK_STOCK.QTY)
                .from(CK_STOCK)
                .where(CK_STOCK.BATCH_CODE.in(batchCodes))
                .and(CK_STOCK.SUPPLIER.eq(supplier))
                .fetch();

        // Create a map of batchCode -> qty
        Map<String, Float> batchCodeToQtyMap = new HashMap<>();
        for (Record row : stockResults) {
            String batchCode = row.get(CK_STOCK.BATCH_CODE);
            Float qty = row.get(CK_STOCK.QTY).floatValue();
            batchCodeToQtyMap.put(batchCode, qty);
        }

        // Sort batch codes in ascending order
        List<String> sortedBatchCodes = new ArrayList<>(batchCodeToQtyMap.keySet());
        sortedBatchCodes.sort(String::compareTo);

        // Find the batch code with the lowest non-zero quantity
        String selectedBatchCode = null;
        for (String batchCode : sortedBatchCodes) {
            Float qty = batchCodeToQtyMap.get(batchCode);
            if (qty != null && qty > 0) {
                selectedBatchCode = batchCode;
                break;
            }
        }

        if (selectedBatchCode == null) {
            throw new DataTransformationService.TransformationException("No batch code with non-zero quantity found.");
        }

        // Get the price of the selected batch code
        String selectedBatchCodePrice = priceMap.get(selectedBatchCode);
        if (selectedBatchCodePrice == null) {
            throw new DataTransformationService.TransformationException("Price not found for batch code: " + selectedBatchCode);
        }

        double price = Double.parseDouble(selectedBatchCodePrice);

        // Perform the final calculation
        double budgetPercentage = BUDGET_PERCENTAGE / 100.0;
        double thresholdQuantity = budget * budgetPercentage;
        if (balance / price < thresholdQuantity / price) {
            int rowsUpdated = dsl.update(CK_SCHEME_OUTLET_BIFURCATIONS)
                    .set(CK_SCHEME_OUTLET_BIFURCATIONS.ACTIVE_STATUS, "inactive")
                    .set(CK_SCHEME_OUTLET_BIFURCATIONS.LAST_MODIFIED_TIME, DSL.currentLocalDateTime())
                    .where(CK_SCHEME_OUTLET_BIFURCATIONS.SCHEME_ID.eq(promoCode))
                    .and(CK_SCHEME_OUTLET_BIFURCATIONS.LOGIN_ID.eq(supplier))
                    .execute();

            if (rowsUpdated == 0) {
                throw new DataTransformationService.TransformationException("No records found to update for promo code '" + promoCode + "' and supplier '" + supplier + "'.");
            } else {
                logger.info("Updated {} rows to inactive status for promo code {} and supplier {}.", rowsUpdated, promoCode, supplier);
            }
        } else {
            logger.info("No action required for promo code {} and supplier {}", promoCode, supplier);
        }
    }

    private Map<String, String> mapBatchCodeToCostPrice(Map<String, Object> payload) {
        // Extract promo code from the payload
        String promoCode = (String) payload.get("promo_code");

        // Fetch the batch codes and extended attributes for the given promoCode
        Result<Record2<String, org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode>> results = dsl.select(CK_SCHEME_FREEPRODUCTINFO.BATCH_CODE, CK_SCHEME_FREEPRODUCTINFO.EXTENDED_ATTRIBUTES)
                .from(CK_SCHEME_FREEPRODUCTINFO)
                .where(CK_SCHEME_FREEPRODUCTINFO.SCHEME_ID.eq(promoCode))
                .fetch();

        Map<String, String> batchCodeToCostPriceMap = new HashMap<>();

        // Iterate over results and extract batchCode and costPrice from extended_attributes
        for (Record result : results) {
            String batchCode = result.get(CK_SCHEME_FREEPRODUCTINFO.BATCH_CODE);
            String extendedAttributesJson = result.get(CK_SCHEME_FREEPRODUCTINFO.EXTENDED_ATTRIBUTES, String.class);

            // Parse the extended_attributes JSON to extract cost price
            String costPrice = extractCostPriceFromJson(extendedAttributesJson);

            if (costPrice != null) {
                batchCodeToCostPriceMap.put(batchCode, costPrice);
            }
        }

        return batchCodeToCostPriceMap;
    }

    private String extractCostPriceFromJson(String extendedAttributesJson) {
        // Assuming extended_attributes is a JSON string, parse it to extract the cost price
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode extendedAttributesNode = objectMapper.readTree(extendedAttributesJson);

            // Extract the "COST_PRC" field from the JSON
            JsonNode costPriceNode = extendedAttributesNode.get("COST_PRC");
            return costPriceNode != null ? costPriceNode.asText() : null;
        } catch (Exception e) {
            logger.error("Error parsing extended_attributes JSON: {}", e.getMessage());
            return null;
        }
    }

    private void processFOCScheme(String batchCode, String costPrice) {
        // Fetch existing extended_attributes
        ObjectNode existingAttributes = dsl.select(CK_SCHEME_FREEPRODUCTINFO.EXTENDED_ATTRIBUTES)
                .from(CK_SCHEME_FREEPRODUCTINFO)
                .where(CK_SCHEME_FREEPRODUCTINFO.BATCH_CODE.eq(batchCode))
                .fetchOneInto(ObjectNode.class);

        // Create or update the ObjectNode
        ObjectNode extendedAttributes;
        if (existingAttributes != null && existingAttributes.isObject()) {
            extendedAttributes = existingAttributes;
        } else {
            extendedAttributes = JSONUtils.getObjectMapper().convertValue(new HashMap<>(),ObjectNode.class);
        }
        extendedAttributes.put("COST_PRC", costPrice);

        // Update the extended_attributes for the given batch code
        int rowsUpdated = dsl.update(CK_SCHEME_FREEPRODUCTINFO)
                .set(CK_SCHEME_FREEPRODUCTINFO.EXTENDED_ATTRIBUTES, extendedAttributes)
                .where(CK_SCHEME_FREEPRODUCTINFO.BATCH_CODE.eq(batchCode))
                .execute();

        logger.info("Updated batch code '{}' with cost price '{}'.", batchCode, costPrice);
    }

    private String validateField(Object fieldValue, String fieldName) {
        if (fieldValue == null || String.valueOf(fieldValue).trim().isEmpty()) {
            throw new DataTransformationService.TransformationException(
                    "Field '" + fieldName + "' is required and cannot be empty.");
        }
        return String.valueOf(fieldValue);
    }

    private double validateNumberField(Object fieldValue, String fieldName) {
        try {
            return Double.parseDouble(String.valueOf(fieldValue));
        } catch (NumberFormatException e) {
            throw new DataTransformationService.TransformationException("Field '" + fieldName + "' must be a valid number.");
        }
    }

    private String getPromoType(String promoCode) {
        // Run a query to fetch the promo type from the database
        String schemeType = dsl.select(CK_SCHEME_CALCULATION.SCHEME_TYPE)
                .from(CK_SCHEME_CALCULATION)
                .where(CK_SCHEME_CALCULATION.SCHEME_ID.eq(promoCode))
                .fetchOneInto(String.class);

        if (schemeType != null) {
            return schemeType;
        } else {
            throw new DataTransformationService.TransformationException("Promo type not found for promo code: " + promoCode);
        }
    }
}