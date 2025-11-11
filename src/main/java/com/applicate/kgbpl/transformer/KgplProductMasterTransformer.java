package com.applicate.kgbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;

public class KgplProductMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final List<String> requiredFields = Arrays.asList(
            "ProductSearchName",  // skuDescription
            "ItemId",             // used in skuCode, mCode
            "ConfigurationId",    // used in skuCode
            "StyleId",            // used in skuCode
            "Deactive"            //used in activeStatus
    );

    @Override
    public Map<String, Object> transform(Map<String, Object> source) {
        validateRequiredFields(source);

        HashMap<String, Object> result = new HashMap<>();
        String dataAreaId = getValueOrNA(source.get("dataAreaId"));

        result.put("mCode", validateAndReturn(source.get("ItemId"), "ItemId"));
        result.put("source", dataAreaId);
        result.put("display", "false");
        result.put("style", validateAndReturn(source.get("StyleId"), "StyleId") + "-" + dataAreaId);
        result.put("itemId", validateAndReturn(source.get("ItemId"), "ItemId") + "-" + dataAreaId);
        result.put("skuDescription", validateAndReturn(source.get("ProductSearchName"), "ProductSearchName"));
        result.put("tariffCode", formatWithDataAreaId(getValueOrNA(source.get("HSNCode")), dataAreaId));
        result.put("caseToPieceQuantity", getNumericValue(source.get("NOB"), "caseToPieceQuantity"));
        result.put("itemType", getValueOrNA(source.get("AcxItemType")));
        result.put("skuName", validateAndReturn(source.get("ProductSearchName"), "ProductSearchName"));

        // Existing size handling
        String sizeId = getValueOrNA(source.get("SizeId"));
        boolean hasValidSize = !"NA".equals(sizeId);
        result.put("size", formatWithDataAreaId(sizeId, dataAreaId)); // Always present

        // NEW colorCode handling same as size
        String colorId = getValueOrNA(source.get("ColorId"));
        boolean hasValidColor = !"NA".equals(colorId);
        result.put("colorCode", formatWithDataAreaId(colorId, dataAreaId)); // Always present

        result.put("productCode", formatWithDataAreaId(getValueOrNA(source.get("AcxPriceGroup")), dataAreaId));
        result.put("pieceSize", formatWithDataAreaId(getValueOrNA(source.get("PackSize")), dataAreaId));
        result.put("pieceSizeDesc", formatWithDataAreaId(getValueOrNA(source.get("PackTypeGroup")), dataAreaId));
        result.put("subCategory", formatWithDataAreaId(getValueOrNA(source.get("PackType")), dataAreaId));

        String productSegment = getValueOrNA(source.get("ProductSegment"));
        result.put("categoryCode", formatWithDataAreaId(productSegment, dataAreaId));
        result.put("category", formatWithDataAreaId(productSegment, dataAreaId));
        result.put("brandCode", formatWithDataAreaId(getValueOrNA(source.get("Brand")), dataAreaId));
        result.put("flavour", formatWithDataAreaId(getValueOrNA(source.get("BrandCategory")), dataAreaId));

        Float palletSize = getNumericValue(source.get("PalletSize"), "PalletSize");
        Float nob = getNumericValue(source.get("NOB"), "NOB");

        if (nob != null) {
            float effectivePalletSize = (palletSize != null && palletSize > 0) ? palletSize : 1.0f;
            Float otherUnitToPieceQuantity = effectivePalletSize * nob;
            result.put("otherUnitToPieceQuantity", otherUnitToPieceQuantity);
        } else {
            throw new DataTransformationService.TransformationException("NOB value is required for otherUnitToPieceQuantity calculation but is missing or null");
        }

        String configIdRaw = getValueOrNA(source.get("ConfigurationId"));
        try {
            Float.parseFloat(configIdRaw);
            result.put("mrp", configIdRaw);
        } catch (NumberFormatException e) {
            result.put("mrp", "0");
        }

        result.put("capacity", formatWithDataAreaId(configIdRaw, dataAreaId));
        result.put("flcd", formatWithDataAreaId(getValueOrNA(source.get("FLCD")), dataAreaId));
        result.put("szcd", formatWithDataAreaId(getValueOrNA(source.get("SZCD")), dataAreaId));
        result.put("lineDiscountGroup", formatWithDataAreaId(getValueOrNA(source.get("LineDisc")), dataAreaId));

        String itemId = validateAndReturn(source.get("ItemId"), "ItemId");
        String styleId = validateAndReturn(source.get("StyleId"), "StyleId");
        String configId = validateAndReturn(source.get("ConfigurationId"), "ConfigurationId");

        // Build skuCode with optional size and colorCode
        StringBuilder skuCodeBuilder = new StringBuilder();
        skuCodeBuilder.append(itemId).append("_").append(styleId).append("_").append(configId);
        if (hasValidSize) {
            skuCodeBuilder.append("_").append(sizeId);
        }
        if (hasValidColor) {
            skuCodeBuilder.append("_").append(colorId);
        }


        skuCodeBuilder.append("_").append(dataAreaId);

        String skuCode = skuCodeBuilder.toString();
        result.put("skuCode", skuCode);
        result.put("batchCode", skuCode);

        String deactive = getValueOrNA(source.get("Deactive")).toLowerCase();
        if ("yes".equals(deactive)) {
            result.put("activeStatus", "inactive");
        } else if ("no".equals(deactive)) {
            result.put("activeStatus", "active");
        } else {
            throw new DataTransformationService.TransformationException("Invalid value for Deactive field: " + deactive);
        }

        ObjectNode extended = new ObjectMapper().createObjectNode();
        try {
            extended.put("skuCaseWeight", Double.parseDouble(getValueOrNA(source.get("NetProductWeight"))));
        } catch (NumberFormatException e) {
            extended.put("skuCaseWeight", 0.0);
        }
        Object rawPalletSize = source.get("PalletSize");
        if (rawPalletSize == null || ObjectUtils.isEmpty(rawPalletSize.toString().trim())) {
            extended.putNull("palletSize");
        } else {
            extended.put("palletSize", rawPalletSize.toString()); // no trim/parse, preserve exactly
        }

        result.put("extendedAttributes", JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class));
        result.put("skuPieceWeight", Optional.ofNullable(calculateSkuPcWeight(source.get("NetProductWeight"), source.get("NOB"))).orElse(0.0f));
        result.put("empties", getValueOrNA(source.get("RGBItemId")));
        result.put("crateRequired", getValueOrNA(source.get("CrateItemId")));
        result.put("emptiesVariant", getValueOrNA(source.get("RGBRetailVariantId")));

        result.put("bbd", getValueOrNA(source.get("BBD")));
        result.put("dod", getValueOrNA(source.get("DOD")));
        result.put("idod", getValueOrNA(source.get("IDOD")));
        result.put("eanNumber", getValueOrNA(source.get("EanCode")));

        return result;
    }

    private void validateRequiredFields(Map<String, Object> source) {
        for (String key : requiredFields) {
            Object value = source.get(key);
            if (value == null || ObjectUtils.isEmpty(value.toString().trim())) {
                throw new DataTransformationService.TransformationException("Missing or blank required field: " + key);
            }
        }
    }

    private String validateAndReturn(Object value, String fieldName) {
        if (value == null || ObjectUtils.isEmpty(value.toString().trim())) {
            throw new DataTransformationService.TransformationException("Missing or blank field: " + fieldName);
        }
        return value.toString().trim();
    }

    private String getValueOrNA(Object value) {
        return (value != null && !ObjectUtils.isEmpty(value.toString())) ? value.toString().trim() : "NA";
    }

    private Float getNumericValue(Object value, String fieldName) {
        try {
            return Float.parseFloat(getValueOrNA(value));
        } catch (NumberFormatException e) {
            throw new DataTransformationService.TransformationException("Invalid numeric value for " + fieldName + ": " + value);
        }
    }

    private String formatWithDataAreaId(String baseValue, String dataAreaId) {
        return "NA".equals(baseValue) ? "NA" : baseValue + "-" + dataAreaId;
    }

    private Float calculateSkuPcWeight(Object weight, Object nob) {
        try {
            double netWeight = Double.parseDouble(getValueOrNA(weight));
            double numberOfBottles = Double.parseDouble(getValueOrNA(nob));
            if (numberOfBottles == 0) {
                return null;
            }
            double result = netWeight / numberOfBottles;
            return Math.round(result * 100.0) / 100.0f;  // rounded to 2 decimal places
        } catch (NumberFormatException e) {
            return null;
        }
    }

}