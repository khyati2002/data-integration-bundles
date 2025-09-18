package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.Productmetadata;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSON;

import java.util.*;
import java.util.stream.Collectors;

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
        result.put("filename_c", getString(inputMap, "fileName_c"));
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
        result.put("skuCaseVol", getJooqJsonAsString(inputMap, "skuCaseVol"));
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
        result.put("translation", getJooqJsonAsString(inputMap, "translation"));
        result.put("productMetaData", getProductMetaDataList(inputMap));

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

    private String getJooqJsonAsString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return "{}";
        try {
            if (value instanceof JSON) {
                return ((JSON) value).data();
            }
            if (value instanceof JsonNode) {
                return objectMapper.writeValueAsString(value);
            }
            if (value instanceof Map || value instanceof Iterable) {
                return objectMapper.writeValueAsString(value);
            }
            String str = value.toString().trim();
            objectMapper.readTree(str);
            return str;
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<Map<String, Object>> getProductMetaDataList(Map<String, Object> rawMap) {
        Object value = rawMap.get("productMetaData");
        if (value == null) return Collections.emptyList();

        if (value instanceof List<?>) {
            return ((List<?>) value).stream()
                    .filter(Objects::nonNull)
                    .map(item -> {
                        return ProductMetaDataTransformer.transform((Map<String, Object>) item);
                    })
                    .filter(obj -> true)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}


/*
 Test data for ProductDetails

public static String rawStreamingData = "{\n" +
        "  \"requestId\": \"test-req-PRODUCT-001\",\n" +
        "  \"groupId\": \"2025-09-08\",\n" +
        "  \"lob\": \"cktestitcloyalty\",\n" +
        "  \"loginId\": \"integration_user\",\n" +
        "  \"batchNumber\": 1,\n" +
        "  \"transformerInfo\": [\n" +
        "    {\n" +
        "      \"skipPreprocessing\": false,\n" +
        "      \"skipPersist\": false,\n" +
        "      \"entityName\": \"ProductDetails\",\n" +
        "      \"transformerId\": \"genericProductDetailsTransformer\",\n" +
        "      \"operationType\": \"insert\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"features\": [\n" +
        "    {\n" +
        "      \"id\": \"PROD12345\",\n" +
        "      \"activeStatus\": \"ACTIVE\",\n" +
        "      \"activeStatusReason\": \"Valid product entry\",\n" +
        "      \"createdBy\": \"system_user\",\n" +
        "      \"extendedAttributes\": \"{ \\\"productType\\\": \\\"CONSUMER\\\", \\\"isPromotional\\\": false }\",\n" +
        "      \"lob\": \"FMCG\",\n" +
        "      \"modifiedBy\": \"admin_user\",\n" +
        "      \"source\": \"ProductManagementSystem\",\n" +
        "      \"version\": \"1\",\n" +
        "      \"hash\": \"product_hash_abc123\",\n" +
        "      \"skuCode\": \"BSKU001\",\n" +
        "      \"eb2bCode\": \"EB2B_BSKU001\",\n" +
        "      \"batchCode\": \"BATCH_BSKU001_2025\",\n" +
        "      \"productCode\": \"BPRO001\",\n" +
        "      \"product\": \"Bingo Mad Angles Tomato Flavour\",\n" +
        "      \"skuDescription\": \"Bingo Mad Angles Tomato Flavour 10gms\",\n" +
        "      \"size\": \"10G\",\n" +
        "      \"marketSkuCode\": \"MKT_BSKU001\",\n" +
        "      \"marketSku\": \"Bingo Mad Angles 10G\",\n" +
        "      \"category\": \"SNACKS\",\n" +
        "      \"categoryCode\": \"CAT001\",\n" +
        "      \"subCategory\": \"CHIPS\",\n" +
        "      \"subCategoryCode\": \"SUBCAT001\",\n" +
        "      \"brand\": \"BINGO\",\n" +
        "      \"subBrand\": \"MAD ANGLES\",\n" +
        "      \"brandCode\": \"BRAND001\",\n" +
        "      \"ctg\": \"SNACKS_CTG\",\n" +
        "      \"design\": \"TRIANGULAR\",\n" +
        "      \"articleCode\": \"ART001\",\n" +
        "      \"articleDesc\": \"Triangular Snack\",\n" +
        "      \"colorCode\": \"RED\",\n" +
        "      \"color\": \"Tomato Red\",\n" +
        "      \"pieceSize\": \"10\",\n" +
        "      \"pieceSizeDesc\": \"10 Grams\",\n" +
        "      \"unitOfMeasurement\": \"GRAMS\",\n" +
        "      \"caseToPieceQuantity\": \"48.0\",\n" +
        "      \"caseToOtherUnitQuantity\": \"24.0\",\n" +
        "      \"otherUnitToPieceQuantity\": \"2.0\",\n" +
        "      \"pieceToOtherUnitQuantity\": \"0.5\",\n" +
        "      \"pieceToVolume\": \"15.0\",\n" +
        "      \"otherUnitName\": \"PACK\",\n" +
        "      \"skuName\": \"Bingo Mad Angles Tomato 10G\",\n" +
        "      \"itemId\": \"ITEM001\",\n" +
        "      \"itemName\": \"Mad Angles Snack\",\n" +
        "      \"itemDesc\": \"Crunchy triangular snack\",\n" +
        "      \"itemType\": \"FOOD\",\n" +
        "      \"itemClass\": \"SNACK\",\n" +
        "      \"capacity\": \"10G\",\n" +
        "      \"uom\": \"GRAMS\",\n" +
        "      \"purchaseUnit\": \"PIECE\",\n" +
        "      \"flavour\": \"TOMATO\",\n" +
        "      \"tariffCode\": \"1905900000\",\n" +
        "      \"fssaiNumber\": \"12345678901234\",\n" +
        "      \"mrp\": \"5.0\",\n" +
        "      \"caseMrp\": \"240.0\",\n" +
        "      \"otherUnitMrp\": \"10.0\",\n" +
        "      \"schemeDesc\": \"Buy 2 Get 1 Free\",\n" +
        "      \"suggestionText\": \"Bingo Mad Angles Tomato Flavour Chips\",\n" +
        "      \"orderSuggestion\": \"Popular snack item\",\n" +
        "      \"otherProduct\": \"Bingo Mad Angles Masala\",\n" +
        "      \"smartBuy\": \"FAST_MOVING\",\n" +
        "      \"productDescription\": \"Delicious triangular shaped tomato flavoured snack\",\n" +
        "      \"priority\": \"1\",\n" +
        "      \"recPriority\": \"1\",\n" +
        "      \"channel\": \"RETAIL\",\n" +
        "      \"display\": \"SHELF\",\n" +
        "      \"fileName\": \"bingo_mad_angles_tomato.jpg\",\n" +
        "      \"mCode\": \"MC_BINGO_001\",\n" +
        "      \"fileName_a\": \"bingo_mad_angles_tomato_a.jpg\",\n" +
        "      \"fileName_b\": \"bingo_mad_angles_tomato_b.jpg\",\n" +
        "      \"fileName_c\": \"bingo_mad_angles_tomato_c.jpg\",\n" +
        "      \"fileName_f\": \"bingo_mad_angles_tomato_f.jpg\",\n" +
        "      \"fileName_l\": \"bingo_mad_angles_tomato_l.jpg\",\n" +
        "      \"blobKey\": \"blob_key_main_image\",\n" +
        "      \"groupId\": \"GROUP_SNACKS_001\",\n" +
        "      \"blobKey_a\": \"blob_key_image_a\",\n" +
        "      \"blobKey_b\": \"blob_key_image_b\",\n" +
        "      \"blobKey_c\": \"blob_key_image_c\",\n" +
        "      \"blobKey_f\": \"blob_key_image_f\",\n" +
        "      \"blobKey_l\": \"blob_key_image_l\",\n" +
        "      \"style\": \"TRIANGULAR_CHIPS\",\n" +
        "      \"eanNumber\": \"8901030871234\",\n" +
        "      \"lineDiscountGroup\": \"SNACKS_DISCOUNT\",\n" +
        "      \"skuPieceWeight\": \"10.0\",\n" +
        "      \"skuPcWeightUom\": \"GRAMS\",\n" +
        "      \"skuCaseVol\": \"{ \\\"length\\\": 30, \\\"width\\\": 20, \\\"height\\\": 15 }\",\n" +
        "      \"shelfLifeDays\": \"180\",\n" +
        "      \"empties\": \"N\",\n" +
        "      \"emptiesVariant\": \"NONE\",\n" +
        "      \"rgbItemId\": \"RGB_ITEM_001\",\n" +
        "      \"crateRequired\": \"N\",\n" +
        "      \"bbd\": \"BBD_180\",\n" +
        "      \"dod\": \"DOD_SAME_DAY\",\n" +
        "      \"flcd\": \"FLCD_2_DAYS\",\n" +
        "      \"idod\": \"IDOD_1_DAY\",\n" +
        "      \"physicalCases\": \"20\",\n" +
        "      \"uncs\": \"48\",\n" +
        "      \"szcd\": \"SZCD_10G\",\n" +
        "      \"skuLaunchDate\": \"2025-03-07T00:00:00\",\n" +
        "      \"skuDeactivationDate\": \"2025-03-07T00:00:00\",\n" +
        "      \"priceListId\": \"PL_SNACKS_001\",\n" +
        "      \"distributorSkuCode\": \"DIST_BSKU001\",\n" +
        "      \"translation\": \"{ \\\"name_hi\\\": \\\"बिंगो मैड एंगल्स टमाटर\\\", \\\"desc_hi\\\": \\\"स्वादिष्ट त्रिकोणीय नमकीन\\\" }\"\n" +
        "    }\n" +
        "  ]\n" +
        "}";
*/