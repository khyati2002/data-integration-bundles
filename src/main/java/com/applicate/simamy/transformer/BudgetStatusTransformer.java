package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.schemes.services.calculation.SchemeConfigHandler;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BudgetStatusTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private static final Logger logger = LoggerFactory.getLogger(BudgetStatusTransformer.class);
    private final double BUDGET_PERCENTAGE = Double.parseDouble(SpringContext.getBean(SchemeConfigHandler.class).getBudgetPercentageValue());

    private static final String UPDATE_QUERY_TEMPLATE =
            "UPDATE ck_scheme_outlet_bifurcations SET active_status = 'inactive', last_modified_time = NOW() WHERE scheme_id = '%s' AND login_id = '%s'";

    private static final String FETCH_PROMO_TYPE_QUERY =
            "SELECT scheme_type FROM ck_scheme_calculation WHERE scheme_id = ?";

    private static final String UPDATE_FOC_QUERY_TEMPLATE =
            "UPDATE ck_scheme_freeproductinfo " +
                    "SET extended_attributes = JSON_SET(COALESCE(extended_attributes, '{}'), '$.COST_PRC', ?) " +
                    "WHERE batch_code = ?;";

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
                String updateQuery = String.format(UPDATE_QUERY_TEMPLATE, promoCode, supplier);
                JdbcTemplate jdbcTemplate = SpringContext.getBean(JdbcTemplate.class);
                int rowsUpdated = jdbcTemplate.update(updateQuery);

                if (rowsUpdated == 0) {
                    throw new TransformationException("No records found to update for promo code '" + promoCode + "' and supplier '" + supplier + "'.");
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
        JdbcTemplate jdbcTemplate = SpringContext.getBean(JdbcTemplate.class);

        // Get the batch code to cost price mapping using the method from BudgetFOCTransformer
        Map<String, String> priceMap = mapBatchCodeToCostPrice(inputMap);

        // Convert the batch codes to a format suitable for the SQL query
        String batchCodes = priceMap.keySet().stream()
                .map(code -> "'" + code + "'") // Enclose each code in single quotes
                .collect(Collectors.joining(",")); // Join codes with commas

        // Create the query to fetch batchCode and qty for the provided supplier
        String stockQuery = String.format(
                "SELECT batch_code, qty FROM ck_stock WHERE batch_code IN (%s) AND supplier = ?",
                batchCodes
        );

        // Fetch batchCode and qty from ck_stock table
        List<Map<String, Object>> stockResults = jdbcTemplate.queryForList(stockQuery, supplier);

        // Create a map of batchCode -> qty
        Map<String, Float> batchCodeToQtyMap = new HashMap<>();
        for (Map<String, Object> row : stockResults) {
            String batchCode = (String) row.get("batch_code");
            Float qty = (Float) row.get("qty");
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
            throw new TransformationException("No batch code with non-zero quantity found.");
        }

        // Get the price of the selected batch code
        String selectedBatchCodePrice = priceMap.get(selectedBatchCode);
        if (selectedBatchCodePrice == null) {
            throw new TransformationException("Price not found for batch code: " + selectedBatchCode);
        }

        double price = Double.parseDouble(selectedBatchCodePrice);

        // Perform the final calculation
        double budgetPercentage = BUDGET_PERCENTAGE / 100.0;
        double thresholdQuantity = budget * budgetPercentage;
        if (balance / price < thresholdQuantity / price ) {
            String updateQuery = String.format(UPDATE_QUERY_TEMPLATE, promoCode, supplier);

            int rowsUpdated = jdbcTemplate.update(updateQuery);

            if (rowsUpdated == 0) {
                throw new TransformationException("No records found to update for promo code '" + promoCode + "' and supplier '" + supplier + "'.");
            } else {
                logger.info("Updated {} rows to inactive status for promo code {} and supplier {}.", rowsUpdated, promoCode, supplier);
            }
        } else {
            logger.info("No action required for promo code {} and supplier {}", promoCode, supplier);
        }
    }

    
    private Map<String, String> mapBatchCodeToCostPrice(Map<String, Object> payload) {
        JdbcTemplate jdbcTemplate = SpringContext.getBean(JdbcTemplate.class);

        // Extract promo code from the payload
        String promoCode = (String) payload.get("promo_code");

        // Fetch the batch codes and extended attributes (which contains costPrice) for the given promoCode
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT batch_code, extended_attributes FROM ck_scheme_freeproductinfo WHERE scheme_id = ?",
                promoCode
        );

        Map<String, String> batchCodeToCostPriceMap = new HashMap<>();

        // Iterate over results and extract batchCode and costPrice from extended_attributes
        for (Map<String, Object> result : results) {
            String batchCode = (String) result.get("batch_code");
            String extendedAttributesJson = (String) result.get("extended_attributes");

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
        JdbcTemplate jdbcTemplate = SpringContext.getBean(JdbcTemplate.class);

        // Update the cost price directly in extended_attributes for the given batch code
        int rowsUpdated = jdbcTemplate.update(UPDATE_FOC_QUERY_TEMPLATE, costPrice, batchCode);

        logger.info("Updated batch code '{}' with cost price '{}'.", batchCode, costPrice);
    }

    private String validateField(Object fieldValue, String fieldName) {
        if (fieldValue == null || String.valueOf(fieldValue).trim().isEmpty()) {
            throw new TransformationException(
                    "Field '" + fieldName + "' is required and cannot be empty.");
        }
        return String.valueOf(fieldValue);
    }

    private double validateNumberField(Object fieldValue, String fieldName) {
        try {
            return Double.parseDouble(String.valueOf(fieldValue));
        } catch (NumberFormatException e) {
            throw new TransformationException("Field '" + fieldName + "' must be a valid number.");
        }
    }

    private String getPromoType(String promoCode) {
        JdbcTemplate jdbcTemplate = SpringContext.getBean(JdbcTemplate.class);

        // Run a query to fetch the promo type from the database
        List<Map<String, Object>> result = jdbcTemplate.queryForList(FETCH_PROMO_TYPE_QUERY, promoCode);

        if (!result.isEmpty()) {
            return (String) result.get(0).get("scheme_type");
        } else {
            throw new TransformationException("Promo type not found for promo code: " + promoCode);
        }
    }
}

