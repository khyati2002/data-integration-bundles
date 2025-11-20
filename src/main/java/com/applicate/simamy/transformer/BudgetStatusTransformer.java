package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.ProductTagService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.StockService;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.*;
import com.salescode.dim.jooq.impl.SchemeCalculation;
import com.salescode.dim.services.SchemeCalculationService;
import com.salescode.dim.services.SchemeFreeProductInfoService;
import com.salescode.dim.services.SchemeOutletBifurcationService;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.salescode.dim.jooq.generated.Tables.*;

public class BudgetStatusTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private static final Logger logger = LoggerFactory.getLogger(BudgetStatusTransformer.class);

    private static final SchemeOutletBifurcationService schemeOutletBifurcationService = (SchemeOutletBifurcationService) ServiceLocator.lookup(SchemeOutletBifurcations.class);
    private static final SchemeCalculationService schemeCalculationService = (SchemeCalculationService) ServiceLocator.lookup(SchemeCalculation.class);
    private static final SchemeFreeProductInfoService schemeFreeProductInfoService = (SchemeFreeProductInfoService) ServiceLocator.lookup(SchemeFreeproductinfo.class);
    private static final StockService stockService = (StockService) ServiceLocator.lookup(Stock.class);

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        
        double BUDGET_PERCENTAGE = Double.parseDouble(PropertyRegistry.getProperty("budget.percentage.value").get().getValue());
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
            handleItemPromoType(promoCode, supplier, budget, balance, inputMap,BUDGET_PERCENTAGE);
        } else {
            // Check if balance is less than given % of budget
            if (balance < (BUDGET_PERCENTAGE / 100) * budget) {
                schemeOutletBifurcationService.expireBudget(promoCode,supplier);
            } else {
                String errorMsg = "No action required for promo code '" + promoCode + "' and supplier '" + supplier + "' as balance is above the threshold.";
                logger.info(errorMsg);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("schemeId", "123");
        return List.of(response);
    }

    private void handleItemPromoType(String promoCode, String supplier, double budget, double balance, Map<String, Object> inputMap,double BUDGET_PERCENTAGE) {
        // Get the batch code to cost price mapping
        Map<String, String> priceMap = mapBatchCodeToCostPrice(inputMap);

        // Convert the batch codes to a list for the SQL query
        List<String> batchCodes = new ArrayList<>(priceMap.keySet());

        // Fetch batchCode and qty from ck_stock table
        List<Stock> stockResults = stockService.findBySkuCodesAndSupplier(batchCodes,supplier);

        // Create a map of batchCode -> qty
        Map<String, Float> batchCodeToQtyMap = new HashMap<>();
        for (Stock row : stockResults) {
            String batchCode = row.getBatchCode();
            Float qty = row.getQty().floatValue();
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
            schemeOutletBifurcationService.expireBudget(promoCode,supplier);

        } else {
            logger.info("No action required for promo code {} and supplier {}", promoCode, supplier);
        }
    }

    private Map<String, String> mapBatchCodeToCostPrice(Map<String, Object> payload) {
        // Extract promo code from the payload
        String promoCode = (String) payload.get("promo_code");

        // Fetch the batch codes and extended attributes for the given promoCode
        List<SchemeFreeproductinfo> results =schemeFreeProductInfoService.findBySchemeId(promoCode);

        Map<String, String> batchCodeToCostPriceMap = new HashMap<>();

        // Iterate over results and extract batchCode and costPrice from extended_attributes
        for (SchemeFreeproductinfo result : results) {
            String batchCode = result.getBatchCode();
            String extendedAttributesJson = result.getExtendedAttributes().toString();

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
        ObjectNode existingAttributes = schemeFreeProductInfoService.findExtendedByBatchCode(batchCode);

        // Create or update the ObjectNode
        ObjectNode extendedAttributes;
        if (existingAttributes != null && existingAttributes.isObject()) {
            extendedAttributes = existingAttributes;
        } else {
            extendedAttributes = JSONUtils.getObjectMapper().convertValue(new HashMap<>(),ObjectNode.class);
        }
        extendedAttributes.put("COST_PRC", costPrice);

        // Update the extended_attributes for the given batch code
        schemeFreeProductInfoService.setExtendedAttributesByPromoCode(batchCode,extendedAttributes);

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
        String schemeType = schemeCalculationService.getSchemeType(promoCode);

        if (schemeType != null) {
            return schemeType;
        } else {
            throw new DataTransformationService.TransformationException("Promo type not found for promo code: " + promoCode);
        }
    }
}