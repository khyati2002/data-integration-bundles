package com.applicate.kgbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
public class KgbplStockTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        Map<String, Object> responseMap = new HashMap<>();

        String itemId = getValue(inputMap, "ItemId");
        String styleId = getValue(inputMap, "InventStyleId");
        String configId = getValue(inputMap, "configId");
        String sizeId = getValue(inputMap, "InventSizeId");
        String dataAreaId = getValue(inputMap, "dataAreaId");
        String colorId = getValue(inputMap, "InventColorId"); // ✅ new field

//        log.info("Transforming input with itemId={}, styleId={}, configId={}, sizeId={}, dataAreaId={}, colorId={}", itemId, styleId, configId, sizeId, dataAreaId, colorId);

        StringBuilder skuBuilder = new StringBuilder();
        skuBuilder.append(itemId).append("_").append(styleId).append("_").append(configId);

        if (!sizeId.isEmpty()) {
            skuBuilder.append("_").append(sizeId);
        }
        if (!colorId.isEmpty()) { // ✅ include colorId if present
            skuBuilder.append("_").append(colorId);
        }

        skuBuilder.append("_").append(dataAreaId);
        String skuCode = skuBuilder.toString();


        String siteId = getValue(inputMap, "InventSiteId");
        String warehouseId = !siteId.isEmpty() && !dataAreaId.isEmpty()
                ? siteId + "-" + dataAreaId
                : siteId;

        // ✅ Changed: caseQty now Double (preserves decimals, no flooring)
        String caseQtyStr = getValue(inputMap, "AvailPhysical");
        Double caseQty = 0.0;
        try {
            caseQty = Double.parseDouble(caseQtyStr);
        } catch (Exception ignored) {}
        responseMap.put("caseQty", caseQty); // ✅ Double
//        log.info("KGBPL skuCode => {}, caseQty => {}", skuCode, caseQty);
        // ✅ Determine supplierId from dataAreaId
        String supplierId = "";
        if ("kbpl".equalsIgnoreCase(dataAreaId)) {
            supplierId = "KBLCompanySupplier";
        } else if ("kgpl".equalsIgnoreCase(dataAreaId)) {
            supplierId = "KGPLCompanySupplier";
        } else if ("eafp".equalsIgnoreCase(dataAreaId)) {
            supplierId = "EnrichCompanySupplier";
        } else if ("wbpl".equalsIgnoreCase(dataAreaId)) {
            supplierId = "WaveCompanySupplier";
        }


        responseMap.put("skuCode", skuCode);
        responseMap.put("warehouseId", warehouseId);
        responseMap.put("supplier", supplierId);
        responseMap.put("batchId", "unassigned");
        responseMap.put("pieceQty", 0);
        responseMap.put("otherQty", 0);

        // mfgDate from prodDate
        if (inputMap.get("prodDate") != null) {
            String prodDateStr = inputMap.get("prodDate").toString();
            responseMap.put("mfgDate", prodDateStr);

            // shelfLife = expDate - prodDate
            if (inputMap.get("expDate") != null) {
                String expDateStr = inputMap.get("expDate").toString();
                try {
                    LocalDate prodDate = LocalDate.parse(prodDateStr, formatter);
                    LocalDate expDate = LocalDate.parse(expDateStr, formatter);
                    long shelfLifeDays = ChronoUnit.DAYS.between(prodDate, expDate);
                    responseMap.put("shelfLife", String.valueOf(shelfLifeDays));
                } catch (Exception e) {
                    responseMap.put("shelfLife", "0"); // fallback
                }
            }
        }

        responseList.add(responseMap);
        return responseList;
    }

    private String getValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val != null) ? val.toString().trim() : "";
    }
}