package com.applicate.alsafi.transformer;

import com.applicate.services.channelkart.transformers.impl.JoltTransformer;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ProductDetailsTransformer extends JoltTransformer {

    @Override
    public Object transform(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        BigDecimal retailPrice = getBigDecimal(input, "RETAIL_PRICE");
        BigDecimal caseToPiece = getBigDecimal(input, "CASE_TO_PIECE_FACTOR");
        BigDecimal basePrice = getBigDecimal(input, "BASE_PRICE");

        BigDecimal caseMrp = (retailPrice != null && caseToPiece != null && caseToPiece.compareTo(BigDecimal.ZERO) != 0)
                ? retailPrice.divide(caseToPiece).setScale(2, BigDecimal.ROUND_HALF_UP)
                : null;

        result.put("skuCode", getString(input, "SKU_CODE"));
        result.put("skuName", getString(input, "SKU_NAME"));
        result.put("skuDescription", getString(input, "SKU_NAME"));
        result.put("batchCode", getString(input, "SKU_CODE"));
        result.put("productCode", getString(input, "SKU_CODE"));
        result.put("product", getString(input, "SKU_NAME"));
        result.put("category", getString(input, "CATEGORY"));
        result.put("categoryCode", getString(input, "CATEGORY"));
        result.put("subCategory", getString(input, "SUB_CATEGORY"));
        result.put("subCategoryCode", getString(input, "SUB_CATEGORY"));
        result.put("brand", getString(input, "BRAND"));
        result.put("brandCode", getString(input, "BRAND_CODE"));
        result.put("mrp", retailPrice);
        result.put("caseMrp", caseMrp);
        result.put("basePrice", basePrice);
        result.put("caseToPieceQuantity", BigDecimal.valueOf(1).divide(caseToPiece));
        result.put("otherUnitName", null);
        result.put("unitOfMeasurement", getString(input, "BASE_UOM"));
        result.put("channel", "AlSafi");
        result.put("size", getString(input, "PACK_SIZE"));
        result.put("pieceSize", getString(input, "PACK_SIZE"));
        result.put("priority", 1);
        result.put("display", true);

        return result;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : null;
    }

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        try {
            return new BigDecimal(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}