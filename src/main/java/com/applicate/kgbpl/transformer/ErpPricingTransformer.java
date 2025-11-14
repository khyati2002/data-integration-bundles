package com.applicate.kgbpl.transformer;


import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ErpPricingTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    private static final Set<String> VALID_ACCOUNT_CODES = Set.of("Customer", "CustomerDiscGroup");
    private static final Set<String> VALID_ITEM_CODE = Set.of("Table", "GroupId");
    private static final String FROM_DATE = "FromDate";
    private static final String TO_DATE = "ToDate";
    private static final String ACCOUNT_CODE = "AccountCode";
    private static final String ITEM_CODE = "ItemCode";
    private static final String DATA_AREA_ID = "dataAreaId";

    @Override
    public Map<String, Object> transform(Map<String, Object> input) {
        try {
            Map<String, Object> response = new HashMap<>();
            String dataAreaId = safeStr(input.get(DATA_AREA_ID));

            String itemCode = validateItemCode(safeStr(input.get(ITEM_CODE)));
            String accountCode = validateAccountCode(safeStr(input.get(ACCOUNT_CODE)));

            float schemeSalesPrice = Float.parseFloat(safeStr(input.get("SchemeSalesPrice")));
            float caseMrp = Float.parseFloat(safeStr(input.get("MaximumRetailPrice_IN")));
            float casePrice = Float.parseFloat(safeStr(input.get("SalesPrice")));

            validateMandatoryFields(input);
            validateAccountCode(accountCode);
            validateItemCode(itemCode);
            validatePrice(schemeSalesPrice, caseMrp, casePrice);

            if ("CustomerDiscGroup".equals(accountCode)) {
                response.put("priceListId", safeStrWithDataAreaId(input.get("AccountSelection"), dataAreaId));
            } else if ("Customer".equals(accountCode)) {
                response.put("outletCode", safeStrWithDataAreaId(input.get("AccountSelection"), dataAreaId));
            }

            response.put("itemCode", itemCode);
            response.put("productCode", safeStrWithDataAreaId(input.get("ItemRelation"), dataAreaId));
            response.put("style", safeStrWithDataAreaId(input.get("Style"), dataAreaId));
            response.put("size", safeStrWithDataAreaId(input.get("Size"), dataAreaId));
            response.put("configId", safeStrWithDataAreaId(input.get("Configuration"), dataAreaId));
            response.put("color", safeStrWithDataAreaId(input.get("Color"), dataAreaId));
            response.put("unit", safeStr(input.get("Unit")));
            response.put("fromDate", formatDateString(safeStr(input.get(FROM_DATE))));
            response.put("toDate", formatDateString(safeStr(input.get(TO_DATE))));
            response.put("casePrice", casePrice);
            response.put("ssp", schemeSalesPrice);
            response.put("caseMrp", caseMrp);

            return response;
        } catch (Exception e) {
            throw new DataTransformationService.TransformationException("Error transforming ERP pricing data: " + e.getMessage(), e);
        }
    }

    private void validateMandatoryFields(Map<String, Object> input) {
        List<String> missingFields = new ArrayList<>();
        if (safeStr(input.get(ITEM_CODE)).isEmpty()) missingFields.add(ITEM_CODE);
        if (safeStr(input.get(ACCOUNT_CODE)).isEmpty()) missingFields.add(ACCOUNT_CODE);
        if (safeStr(input.get(DATA_AREA_ID)).isEmpty()) missingFields.add(DATA_AREA_ID);
        if (safeStr(input.get(FROM_DATE)).isEmpty()) missingFields.add(FROM_DATE);

        if (!missingFields.isEmpty()) {
            throw new DataTransformationService.TransformationException("Missing mandatory fields: " + String.join(", ", missingFields));
        }
    }

    private String validateAccountCode(String accountCode) {
        if (!VALID_ACCOUNT_CODES.contains(accountCode)) {
            throw new DataTransformationService.TransformationException("Invalid Account Code. Must be one of: " +
                    String.join(", ", VALID_ACCOUNT_CODES));
        }
        return accountCode;
    }

    private String validateItemCode(String itemCode) {
        if (!VALID_ITEM_CODE.contains(itemCode)) {
            throw new DataTransformationService.TransformationException("Invalid itemCode. Must be one of: " +
                    String.join(", ", VALID_ITEM_CODE));
        }
        return itemCode;
    }

    private void validatePrice(float schemeSalesPrice, float caseMrp, float casePrice) {
        if (schemeSalesPrice < 0 || caseMrp < 0 || casePrice < 0) {
            throw new DataTransformationService.TransformationException("Scheme Sales Price, Case MRP, and Case Price can not be negative.");
        }
    }

    private String formatDateString(String isoDateString) {
        if (isoDateString == null || isoDateString.isEmpty()) return "";
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(isoDateString);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return zdt.toLocalDateTime().format(formatter);
        } catch (Exception e) {
            throw new DataTransformationService.TransformationException("Date " + isoDateString + " is in invalid format. Correct Format is : yyyy-MM-dd HH:mm:ss", e);
        }
    }

    private String safeStr(Object obj) {
        return obj == null ? "" : obj.toString().trim();
    }

    private String safeStrWithDataAreaId(Object obj, String dataAreaId) {
        return obj == null || obj.toString().isBlank() ? "" : obj.toString().trim() + "-" + dataAreaId;
    }

}
