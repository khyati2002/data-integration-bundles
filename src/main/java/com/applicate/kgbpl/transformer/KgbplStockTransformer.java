package com.applicate.kgbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class KgbplStockTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        Map<String, Object> responseMap = new HashMap<>();

        String itemId = inputMap.get("ItemId") != null ? inputMap.get("ItemId").toString() : "";
        String styleId = inputMap.get("InventStyleId") != null ? inputMap.get("InventStyleId").toString() : "";
        String configId = inputMap.get("configId") != null ? inputMap.get("configId").toString() : "";
        String skuCode = itemId + "_" + styleId + "_" + configId;

        String warehouseId = inputMap.get("InventSiteId") != null ? inputMap.get("InventSiteId").toString() : "";
        String caseQty = inputMap.get("AvailPhysical") != null ? inputMap.get("AvailPhysical").toString() : "";

        responseMap.put("skuCode", skuCode);
        responseMap.put("warehouseId", warehouseId);
        responseMap.put("supplierId", warehouseId);
        responseMap.put("batchId", "unassigned");
        responseMap.put("caseQty", caseQty);
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
}
