package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

public class SimaProductDetails extends AbstractTransformer<Map<String,Object>,Map<String,Object>> {
    private static  final String MATL_GRP_1 = "MATL_GRP_1";
    private static  final String MATL_GRP_2 = "MATL_GRP_2";
    private static  final String MATL_GRP_3 = "MATL_GRP_3";
    private static  final String MATL_GRP_4 = "MATL_GRP_4";
    private static  final String MATL_GRP_5 = "MATL_GRP_5";
    private static  final String MATL_TYPE = "MATL_TYPE";
    private static  final String BMOB_GROUP = "BMOB_GROUP";
    private static  final String DELYG_PLNT = "DELYG_PLNT";
    private static  final String BRAND = "brand";
    private static  final String CATEGORY = "category";
    private static  final String FLAVOUR = "flavour";
    private static  final String SKU_CODE = "sku_code";
    private static  final String BEVERAGE_PRODUCT = "BEVERAGE_PRODUCT";
    private static  final String TRADE_MARK = "TRADE_MARK";
    private static  final String ICFC_ATTRIBUTE = "ICFC_ATTRIBUTE";
    private static  final String PACKAGE = "PACKAGE";
    private static  final String NET_WEIGHT = "NET_WEIGHT";
    private static  final String GROSS_WEIGHT = "GROSS_WEIGHT";
    private static  final String RETUN_PACK_IND = "RETUN_PACK_IND";
    private static  final String PHYSICAL_STATE = "PHYSICAL_STATE";
    private static  final String SALES_UNIT = "SALES_UNIT";
    private static  final String DENOMINATR = "DENOMINATR";
    private static  final String NUMERATOR = "NUMERATOR";
    private static  final String ITEM_DES_1 = "item_description1";
    private static  final String PACK_TYPE = "packtype";
    private static  final String SALES_ORG = "SALES_ORG";
    private static  final String DISTR_CHAN = "DISTR_CHAN";
    private static  final String PACK_SIZE = "packsize";
    private static  final String BEV_CATEGORY = "BEV_CATEGORY";
    private static  final String BEVERAGE_TYPE = "BEVERAGE_TYPE";
    private static  final String ITEM_TYPE = "itemType";
    private static  final String ZEMP = "ZEMP";
    private static  final String N_A = "NA";
    private static final String SUB_UNITS_PER_CASE = "SubUnitsPerCase";
    private  static final String DISPLAY= "display";
    @Override
    public Object transform(Map<String, Object> stringObjectMap) {
        ObjectNode extended = new ObjectMapper().createObjectNode();
        HashMap<String,Object> finalTransformedObj = new HashMap<>();
        boolean isActive = (boolean) stringObjectMap.get("activeStatus");
        String status = isActive ? "active" : "inactive";
        finalTransformedObj.put("activeStatus", status);
        Object materialsGroups = stringObjectMap.get("MaterialGroups");
        JsonNode materialGroup = JSONUtils.toJsonNode(materialsGroups);
        finalTransformedObj.put(BRAND,stringObjectMap.get(BRAND).toString());
        finalTransformedObj.put("subCategory",stringObjectMap.get(CATEGORY).toString());
        finalTransformedObj.put("pieceSize",stringObjectMap.get(PACK_SIZE).toString());
        finalTransformedObj.put("itemId",stringObjectMap.get(SKU_CODE).toString());
        finalTransformedObj.put("batchCode",stringObjectMap.get(SKU_CODE).toString());
        finalTransformedObj.put("skuCode",stringObjectMap.get(SKU_CODE).toString());
        finalTransformedObj.put("caseToPieceQuantity", Float.parseFloat(stringObjectMap.get(SUB_UNITS_PER_CASE).toString()));
        finalTransformedObj.put("uom",stringObjectMap.get("uom").toString());
        saveDetailsInJsonNode(extended, stringObjectMap);

        if(!materialGroup.isEmpty()) {
            
            if (isNull(MATL_GRP_1, materialGroup.get(0)))
                extended.put(MATL_GRP_1, materialGroup.get(0).get(MATL_GRP_1).asText());
            if (isNull(MATL_GRP_2, materialGroup.get(0)))
                extended.put(MATL_GRP_2, materialGroup.get(0).get(MATL_GRP_2).asText());
            if (isNull(MATL_GRP_3, materialGroup.get(0)))
                extended.put(MATL_GRP_3, materialGroup.get(0).get(MATL_GRP_3).asText());
            if (isNull(MATL_GRP_4, materialGroup.get(0)))
                extended.put(MATL_GRP_4, materialGroup.get(0).get(MATL_GRP_4).asText());
            if (isNull(MATL_GRP_5, materialGroup.get(0)))
                extended.put(MATL_GRP_5, materialGroup.get(0).get(MATL_GRP_5).asText());
        }

        if(isNullOrNot(MATL_TYPE, stringObjectMap) && stringObjectMap.get(MATL_TYPE).equals(ZEMP)){
            extended.put(MATL_TYPE,stringObjectMap.get(MATL_TYPE).toString());
            finalTransformedObj.put(CATEGORY,stringObjectMap.getOrDefault(BEV_CATEGORY,N_A ).toString());
            finalTransformedObj.put(ITEM_TYPE,stringObjectMap.getOrDefault(BEVERAGE_TYPE, N_A).toString());
            finalTransformedObj.put(FLAVOUR,stringObjectMap.getOrDefault(FLAVOUR, N_A).toString());
            extended.put(BEVERAGE_PRODUCT,stringObjectMap.getOrDefault(BEVERAGE_PRODUCT, N_A).toString());
        }else{
            finalTransformedObj.put(CATEGORY,stringObjectMap.get(BEV_CATEGORY ).toString());
            finalTransformedObj.put(ITEM_TYPE,stringObjectMap.get(BEVERAGE_TYPE).toString());
            finalTransformedObj.put(FLAVOUR,stringObjectMap.get(FLAVOUR).toString());
            extended.put(BEVERAGE_PRODUCT,stringObjectMap.get(BEVERAGE_PRODUCT).toString());
        }
        finalTransformedObj.put("fileName",stringObjectMap.get(SKU_CODE).toString());
        finalTransformedObj.put("pieceSizeDesc",stringObjectMap.get(PACK_TYPE).toString());
        finalTransformedObj.put("fileName_a",stringObjectMap.getOrDefault("ean", N_A).toString());
        saveItemDesc(stringObjectMap, finalTransformedObj);
        JsonNode extendedAttribute = JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class);
        finalTransformedObj.put("extendedAttributes", extendedAttribute);
        finalTransformedObj.put(DISPLAY,"false");
        return finalTransformedObj;
    }

    private void saveItemDesc(Map<String, Object> stringObjectMap, HashMap<String, Object> finalTransformedObj) {
        String itemDescription = null;
        itemDescription = ObjectUtils.isEmpty(stringObjectMap.get(ITEM_DES_1))
                ?stringObjectMap.get("item_description2").toString()
                : stringObjectMap.get(ITEM_DES_1).toString();
        finalTransformedObj.put("itemDesc",itemDescription);
        finalTransformedObj.put("skuDescription",itemDescription);
    }

    private void saveDetailsInJsonNode(ObjectNode extended, Map<String, Object> stringObjectMap) {
        extended.put(DISTR_CHAN, "Z1");
        extended.put(SALES_ORG, "0135");
        extended.put(PACK_TYPE, stringObjectMap.get(PACK_TYPE).toString());
        extended.put(CATEGORY, stringObjectMap.get(CATEGORY).toString());
        if(isNullOrNot(TRADE_MARK, stringObjectMap))
            extended.put(TRADE_MARK,stringObjectMap.get(TRADE_MARK).toString());
        if(isNullOrNot(ICFC_ATTRIBUTE, stringObjectMap))
            extended.put(ICFC_ATTRIBUTE,stringObjectMap.get(ICFC_ATTRIBUTE).toString());
        extended.put(PACKAGE,stringObjectMap.get(PACKAGE).toString());
        extended.put(NET_WEIGHT,stringObjectMap.get(NET_WEIGHT).toString());
        extended.put(GROSS_WEIGHT,stringObjectMap.get(GROSS_WEIGHT).toString());
        if(isNullOrNot(RETUN_PACK_IND, stringObjectMap))
            extended.put(RETUN_PACK_IND,stringObjectMap.get(RETUN_PACK_IND).toString());
        if(isNullOrNot(PHYSICAL_STATE, stringObjectMap))
            extended.put(PHYSICAL_STATE,stringObjectMap.get(PHYSICAL_STATE).toString());
        extended.put(SALES_UNIT,stringObjectMap.get(SALES_UNIT).toString());
        if(isNullOrNot(BMOB_GROUP, stringObjectMap))
            extended.put(BMOB_GROUP,stringObjectMap.get(BMOB_GROUP).toString());
        if(isNullOrNot(DELYG_PLNT, stringObjectMap))
            extended.put(DELYG_PLNT,stringObjectMap.get(DELYG_PLNT).toString());
        if(isNullOrNot(DENOMINATR, stringObjectMap))
            extended.put(DENOMINATR,stringObjectMap.get(DENOMINATR).toString());
        if(isNullOrNot(NUMERATOR, stringObjectMap))
            extended.put(NUMERATOR,stringObjectMap.get(NUMERATOR).toString());
    }
    boolean isNull(String field, JsonNode extended){
        return ObjectUtils.isNotEmpty(extended.get(field));
    }
    boolean isNullOrNot(String field, Map<String, Object> input){
        return input.containsKey(field) && input.get(field) != null;
    }
}
