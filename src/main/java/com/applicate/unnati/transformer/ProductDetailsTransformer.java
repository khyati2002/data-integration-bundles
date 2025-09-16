package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProductDetailsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", getString(inputMap, "id"));
        result.put("activeStatus", getActiveStatus(inputMap, "activeStatus"));
        result.put("activeStatusReason", getString(inputMap, "activeStatusReason"));
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("source", getString(inputMap, "source"));
        result.put("version", getInteger(inputMap, "version"));
        result.put("hash", getString(inputMap, "hash"));

        result.put("skuCode", getString(inputMap, "skuCode"));
        result.put("eb2bCode", getString(inputMap, "eb2bCode"));
        result.put("batchCode", getString(inputMap, "batchCode"));
        result.put("productCode", getString(inputMap, "productCode"));
        result.put("product", getString(inputMap, "product"));
        result.put("skuDescription", getString(inputMap, "skuDescription"));
        result.put("size", getString(inputMap, "size"));
        result.put("marketSkuCode", getString(inputMap, "marketSkuCode"));
        result.put("marketSku", getString(inputMap, "marketSku"));


        result.put("category", getString(inputMap, "category"));
        result.put("categoryCode", getString(inputMap, "categoryCode"));
        result.put("subCategory", getString(inputMap, "subCategory"));
        result.put("subCategoryCode", getString(inputMap, "subCategoryCode"));
        result.put("brand", getString(inputMap, "brand"));
        result.put("subBrand", getString(inputMap, "subBrand"));
        result.put("brandCode", getString(inputMap, "brandCode"));
        result.put("ctg", getString(inputMap, "ctg"));

        result.put("design", getString(inputMap, "design"));
        result.put("articleCode", getString(inputMap, "articleCode"));
        result.put("articleDesc", getString(inputMap, "articleDesc"));
        result.put("colorCode", getString(inputMap, "colorCode"));
        result.put("color", getString(inputMap, "color"));

        result.put("pieceSize", getString(inputMap, "pieceSize"));
        result.put("pieceSizeDesc", getString(inputMap, "pieceSizeDesc"));
        result.put("unitOfMeasurement", getString(inputMap, "unitOfMeasurement"));
        result.put("caseToPieceQuantity", getFloat(inputMap, "caseToPieceQuantity"));
        result.put("caseToOtherUnitQuantity", getFloat(inputMap, "caseToOtherUnitQuantity"));
        result.put("otherUnitToPieceQuantity", getFloat(inputMap, "otherUnitToPieceQuantity"));
        result.put("pieceToOtherUnitQuantity", getFloat(inputMap, "pieceToOtherUnitQuantity"));
        result.put("pieceToVolume", getFloat(inputMap, "pieceToVolume"));
        result.put("otherUnitName", getString(inputMap, "otherUnitName"));

        // Item details
        result.put("skuName", getString(inputMap, "skuName"));
        result.put("itemId", getString(inputMap, "itemId"));
        result.put("itemName", getString(inputMap, "itemName"));
        result.put("itemDesc", getString(inputMap, "itemDesc"));
        result.put("itemType", getString(inputMap, "itemType"));
        result.put("itemClass", getString(inputMap, "itemClass"));
        result.put("capacity", getString(inputMap, "capacity"));
        result.put("uom", getString(inputMap, "uom"));
        result.put("purchaseUnit", getString(inputMap, "purchaseUnit"));

        result.put("flavour", getString(inputMap, "flavour"));
        result.put("tariffCode", getString(inputMap, "tariffCode"));
        result.put("fssaiNumber", getString(inputMap, "fssaiNumber"));

        result.put("mrp", getFloat(inputMap, "mrp"));
        result.put("caseMrp", getFloat(inputMap, "caseMrp"));
        result.put("otherUnitMrp", getFloat(inputMap, "otherUnitMrp"));

        result.put("schemeDesc", getString(inputMap, "schemeDesc"));
        result.put("suggestionText", getString(inputMap, "suggestionText"));
        result.put("orderSuggestion", getString(inputMap, "orderSuggestion"));
        result.put("otherProduct", getString(inputMap, "otherProduct"));
        result.put("smartBuy", getString(inputMap, "smartBuy"));
        result.put("productDescription", getString(inputMap, "productDescription"));
        result.put("priority", getInteger(inputMap, "priority"));
        result.put("recPriority", getInteger(inputMap, "recPriority"));
        result.put("channel", getString(inputMap, "channel"));
        result.put("display", getString(inputMap, "display"));

        // Image and blob fields
        result.put("fileName", getString(inputMap, "fileName"));
        result.put("mCode", getString(inputMap, "mCode"));
        result.put("fileName_a", getString(inputMap, "fileName_a"));
        result.put("fileName_b", getString(inputMap, "fileName_b"));
        result.put("fileName_c", getString(inputMap, "fileName_c"));
        result.put("fileName_f", getString(inputMap, "fileName_f"));
        result.put("fileName_l", getString(inputMap, "fileName_l"));
        result.put("blobKey", getString(inputMap, "blobKey"));
        result.put("groupId", getString(inputMap, "groupId"));
        result.put("blobKey_a", getString(inputMap, "blobKey_a"));
        result.put("blobKey_b", getString(inputMap, "blobKey_b"));
        result.put("blobKey_c", getString(inputMap, "blobKey_c"));
        result.put("blobKey_f", getString(inputMap, "blobKey_f"));
        result.put("blobKey_l", getString(inputMap, "blobKey_l"));

        result.put("style", getString(inputMap, "style"));
        result.put("eanNumber", getString(inputMap, "eanNumber"));
        result.put("lineDiscountGroup", getString(inputMap, "lineDiscountGroup"));
        result.put("skuPieceWeight", getFloat(inputMap, "skuPieceWeight"));
        result.put("skuPcWeightUom", getString(inputMap, "skuPcWeightUom"));
        result.put("skuCaseVol", getJsonNode(inputMap, "skuCaseVol"));
        result.put("shelfLifeDays", getInteger(inputMap, "shelfLifeDays"));

        result.put("empties", getString(inputMap, "empties"));
        result.put("emptiesVariant", getString(inputMap, "emptiesVariant"));
        result.put("rgbItemId", getString(inputMap, "rgbItemId"));
        result.put("crateRequired", getString(inputMap, "crateRequired"));

        result.put("bbd", getString(inputMap, "bbd"));
        result.put("dod", getString(inputMap, "dod"));
        result.put("flcd", getString(inputMap, "flcd"));
        result.put("idod", getString(inputMap, "idod"));
        result.put("physicalCases", getInteger(inputMap, "physicalCases"));
        result.put("uncs", getInteger(inputMap, "uncs"));
        result.put("szcd", getString(inputMap, "szcd"));

        result.put("skuLaunchDate", getString(inputMap, "skuLaunchDate"));
        result.put("skuDeactivationDate", getString(inputMap, "skuDeactivationDate"));

        result.put("priceListId", getString(inputMap, "priceListId"));
        result.put("distributorSkuCode", getString(inputMap, "distributorSkuCode"));

        result.put("translation", getJsonNode(inputMap, "translation"));

        return result;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Float getFloat(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ActiveStatus getActiveStatus(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof ActiveStatus) {
                return (ActiveStatus) value;
            }
            String statusStr = value.toString().toUpperCase().trim();
            return ActiveStatus.valueOf(statusStr);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode getJsonNode(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof JsonNode) {
                return (JsonNode) value;
            }
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
