package com.applicate.cokesa.task;

import com.applicate.services.channelkart.dto.SupplierInfo;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.models.OutletProductInfo;
import com.applicate.services.channelkart.services.SupplierInfoService;
import com.applicate.services.channelkart.taskexecutors.AbstractTaskExecutor;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PricingTaskCokeSA extends AbstractTaskExecutor {

    private static final String PRODUCT_METADATA_QUERY =
            "SELECT sku_code skuCode, price_list,case_ptr, from_date fromDate FROM ck_productmetadata WHERE active_status = 'active';";

    private static final String OUTLET_QUERY =
            "SELECT outletcode, price_list_id FROM ck_outlet_details WHERE active_status = 'active' AND price_list_id IS NOT NULL";

    private static final String SKU_DETAILS_QUERY =
            "SELECT sku_code, case_to_piece_quantity FROM ck_productdetails where case_to_piece_quantity is not null";

    private static final String EXCISE_TAX_QUERY =
            "SELECT sku_code, tax_rate FROM ck_tax WHERE active_status = 'active' AND tax_type = 'Excise'";


    private final QueryService queryService = SpringContext.getBean(QueryService.class);
    private final GenericEntityService genericEntityService = SpringContext.getBean(GenericEntityService.class);
    private final OutletProductInfoService outletProductInfoService = SpringContext.getBean(OutletProductInfoService.class);

    private final SupplierInfoService supplierInfoService = SpringContext.getBean(SupplierInfoService.class);

    @Override
    public void runTask(com.applicate.services.channelkart.models.Task task) {
        Map<String, Map<String, Object>> productMapByPriceList = getProductMetadataByPriceList();
        Map<String, Map<String, ObjectNode>> finalPricingMap = buildPricingForPriceLists(productMapByPriceList);
        assignPricingToOutlets(finalPricingMap);
    }

    private Map<String, Map<String, Object>> getProductMetadataByPriceList() {
        List<Map<String, Object>> allMetadata = queryService.execute(PRODUCT_METADATA_QUERY);
        Map<String, Map<String, Object>> priceListMap = new HashMap<>();

        for (Map<String, Object> row : allMetadata) {
            String priceListId = row.get("price_list").toString();
            String skuCode = row.get("skuCode").toString();
            priceListMap.computeIfAbsent(priceListId, k -> new HashMap<>()).put(skuCode, row);
        }
        return priceListMap;
    }

    private double fetchVatPercentageFromGenericEntity() {
        List<GenericEntity> taxes = genericEntityService.readModelsByName("TaxDefined");

        return taxes.stream()
                .map(GenericEntity::getPayload)
                .filter(payload -> payload.has("taxProgram") && "VAT".equalsIgnoreCase(payload.get("taxProgram").asText()))
                .map(payload -> payload.has("taxValueIfPercentage") ? payload.get("taxValueIfPercentage").asDouble() :
                        payload.has("value") ? payload.get("value").asDouble() : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(15.0);
    }


    private Map<String, Double> loadExciseTaxMap() {
        List<Map<String, Object>> exciseRows = queryService.execute(EXCISE_TAX_QUERY);
        Map<String, Double> exciseMap = new HashMap<>();
        for (Map<String, Object> row : exciseRows) {
            String skuCode = row.get("sku_code").toString();
            double taxRate = Double.parseDouble(row.get("tax_rate").toString());
            exciseMap.put(skuCode, taxRate);
        }
        return exciseMap;
    }


    private Map<String, Map<String, ObjectNode>> buildPricingForPriceLists(Map<String, Map<String, Object>> priceListMap) {
        Map<String, Double> caseMap = loadCaseSizeMap();
        Map<String, Double> exciseMap = loadExciseTaxMap();
        Map<String, Map<String, ObjectNode>> pricingData = new HashMap<>();

        priceListMap.forEach((String priceListId, Map<String, Object> skuDataMap) -> {
            Map<String, ObjectNode> skuPricingMap = new HashMap<>();

            skuDataMap.forEach((skuCode, metaObj) -> {
                Map<String, Object> meta = (Map<String, Object>) metaObj; // FIX
                ObjectNode pricingNode = createProductPricing(meta, caseMap,exciseMap);
                pricingNode.put("priceListId", priceListId);
                skuPricingMap.put(skuCode, pricingNode);
            });

            pricingData.put(priceListId, skuPricingMap);
        });


        return pricingData;
    }

    private void assignPricingToOutlets(Map<String, Map<String, ObjectNode>> finalPricingMap) {
        List<Map<String, Object>> outletData = queryService.execute(OUTLET_QUERY);
        int totalOutlets = outletData.size();
        int processedCount = 0;
        int skippedCount = 0;

        System.out.println("[PricingTaskCokeSA] Total outlets to process: " + totalOutlets);

        for (Map<String, Object> outlet : outletData) {
            String outletCode = outlet.get("outletcode").toString();
            List<SupplierInfo> suppliers = supplierInfoService.findSuppliersByOutlet(outletCode);
            String distributor = suppliers.isEmpty() ? "" : suppliers.get(0).getLoginId();
            String priceListId = outlet.get("price_list_id").toString();

            Map<String, ObjectNode> pricingForList = finalPricingMap.get(priceListId);
            if (pricingForList == null) {
                System.err.println("[PricingTaskCokeSA] No pricing found for priceListId: " + priceListId + " for outlet: " + outletCode);
                skippedCount++;
                continue;
            }

            for (ObjectNode skuNode : pricingForList.values()) {
                skuNode.put("distributor", distributor);
            }

            OutletProductInfo outletProductInfo = outletProductInfoService.findByOutletCode(outletCode, false);
            if (outletProductInfo == null) {
                outletProductInfo = new OutletProductInfo();
                outletProductInfo.setOutletCode(outletCode);
            }

            JsonNode finalPayload = JSONUtils.toJsonNode(pricingForList);
            outletProductInfo.setProductInfo(finalPayload);
            outletProductInfoService.save(outletProductInfoService.refresh(outletProductInfo));

            processedCount++;
            if (processedCount % 100 == 0) {
                System.out.println("[PricingTaskCokeSA] Processed " + processedCount + " / " + totalOutlets + " outlets...");
            }
        }

        System.out.println("[PricingTaskCokeSA] Processing complete. Processed: " + processedCount + ", Skipped: " + skippedCount);
    }



    private Map<String, Double> loadCaseSizeMap() {
        List<Map<String, Object>> productDetails = queryService.execute(SKU_DETAILS_QUERY);
        Map<String, Double> caseMap = new HashMap<>();

        for (Map<String, Object> details : productDetails) {
            String sku = details.get("sku_code").toString();
            double caseSize = Double.parseDouble(details.get("case_to_piece_quantity").toString());
            caseMap.put(sku, caseSize);
        }

        return caseMap;
    }


    private ObjectNode createProductPricing(Map<String, Object> pd, Map<String, Double> caseMap, Map<String, Double> exciseMap) {
        ObjectNode node = JSONUtils.getObjectMapper().createObjectNode();

        String itemCode = pd.get("skuCode").toString();

        BigDecimal casePtrDecimal = (BigDecimal) pd.get("case_ptr");
        double casePtr = casePtrDecimal != null ? casePtrDecimal.doubleValue() : 0d;

        double caseSize = caseMap.getOrDefault(itemCode, 0d);

        double piecePtr = (caseSize > 0) ? casePtr / caseSize : 0d;
        double vatPercent = fetchVatPercentageFromGenericEntity();


        // Excise value from tax map
        double excise = roundTo2Decimal(exciseMap.getOrDefault(itemCode, 0d));

        node.put("skuCode", itemCode);
        node.put("c", casePtr);
        node.put("p", piecePtr);
        node.put("t", vatPercent);
        node.put("taxAmount", excise);

        return node;
    }

    private double roundTo2Decimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }



    @Override
    public String getTaskType() {
        return "CokeSaPricingTask";
    }
}
