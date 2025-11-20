package com.applicate.cokeph.transformer;

import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.Map;

public class CokephProductMasterTransformer extends AbstractTransformer <Map<String, Object>, Map<String, Object>>  {

    static final String ITEM_TYPE = "category_code_1" ;

    static final String BRAND = "category_code_2" ;

    static final String PACK_TYPE = "category_code_3" ;

    static final String PACK_SIZE = "category_code_4" ;
    static final String ITEM_CLASS = "item_type_code" ;

    @Override
    public Map<String, Object> transform(Map<String, Object> stringObjectMap) {

//        ObjectNode extended = new ObjectMapper().createObjectNode();
        HashMap<String, Object> finalTransformedObj = new HashMap<>();

        if (stringObjectMap.containsKey("item_code") && NullUtils.isNotNull(stringObjectMap.get("item_code").toString()) && StringUtils.isNotEmpty(stringObjectMap.get("item_code").toString())) {
            String skuCode = stringObjectMap.get("item_code").toString();
            finalTransformedObj.put("skuCode", skuCode);
            finalTransformedObj.put("batchCode",skuCode);
        }
        Object skuDescriptionValue = stringObjectMap.get("item_description");
        finalTransformedObj.put("skuDescription",skuDescriptionValue != null && !ObjectUtils.isEmpty(skuDescriptionValue) ? skuDescriptionValue : "NA");
        finalTransformedObj.put("uom",stringObjectMap.getOrDefault("base_uom","NA").toString());
        finalTransformedObj.put("caseToPieceQuantity",stringObjectMap.getOrDefault("conversion_1","NA").toString());

        if (stringObjectMap.get("is_active").toString().equals("1")) {
            finalTransformedObj.put("activeStatus", "active");
        } else {
            finalTransformedObj.put("activeStatus", "inactive");
        }
        String itemNameVal = String.valueOf(NullUtils.isNotNull(stringObjectMap.get("itemName")));
        finalTransformedObj.put("itemName", itemNameVal != null && !itemNameVal.isEmpty() ? itemNameVal : "NA");
        finalTransformedObj.put("pieceToOtherUnitQuantity",stringObjectMap.getOrDefault("pc_conversion","NA").toString());
        finalTransformedObj.put("caseToOtherUnitQuantity",stringObjectMap.getOrDefault("uc_conversion","NA").toString());
        finalTransformedObj.put("subCategory",stringObjectMap.getOrDefault("sub_category","NA").toString());

        // adding mapping of category codes
        String itemTypeVal = stringObjectMap.get(ITEM_TYPE).toString();
        finalTransformedObj.put("itemType", itemTypeVal != null && !ObjectUtils.isEmpty(itemTypeVal) ? itemTypeVal : "NA" );
        String brandVal = stringObjectMap.get(BRAND).toString();
        finalTransformedObj.put("brand", brandVal != null && !ObjectUtils.isEmpty(brandVal) ? brandVal : "NA" );
        String pieceSizeDescVal = stringObjectMap.get(PACK_TYPE).toString();
        finalTransformedObj.put("pieceSizeDesc", pieceSizeDescVal != null && !ObjectUtils.isEmpty(pieceSizeDescVal) ? pieceSizeDescVal : "NA" );
        String pieceSizeVal = stringObjectMap.get(PACK_SIZE).toString();
        finalTransformedObj.put("pieceSize", pieceSizeVal != null && !ObjectUtils.isEmpty(pieceSizeVal) ? pieceSizeVal : "NA" );
        String categoryVal = (stringObjectMap.get("category_code") != null) ? stringObjectMap.get("category_code").toString() : "NA";
        finalTransformedObj.put("category", categoryVal!= null && !ObjectUtils.isEmpty(categoryVal) ? categoryVal : "NA") ;

        //adding mapping in extended attributes
        Map<String,Object> input = new HashMap<>();
        Object taxGroupCodeValue = stringObjectMap.get("tax_group_code");
        input.put("tax_group_code",taxGroupCodeValue != null && !ObjectUtils.isEmpty(taxGroupCodeValue.toString()) ? taxGroupCodeValue.toString() : "NA");
        Object uomV1alue = stringObjectMap.get("uom_1");
        input.put("uom_1",uomV1alue != null && !ObjectUtils.isEmpty(uomV1alue.toString()) ? uomV1alue.toString() : "NA");
        Object UnitOfMeasureValue = stringObjectMap.get("units_of_measure");
        input.put("units_of_measure",UnitOfMeasureValue != null && !ObjectUtils.isEmpty(UnitOfMeasureValue.toString()) ? UnitOfMeasureValue.toString() : "NA");
        finalTransformedObj.put("extendedAttributes", input);
        Object rawValue = stringObjectMap.get(ITEM_CLASS);
        String itemTypeCodeRaw = rawValue != null ? rawValue.toString().trim() : null;
        Integer itemTypeCode;
        if (itemTypeCodeRaw == null || itemTypeCodeRaw.isEmpty()) {
            itemTypeCode = 0;
        } else if ("0".equals(itemTypeCodeRaw) || "1".equals(itemTypeCodeRaw)) {
            itemTypeCode = Integer.parseInt(itemTypeCodeRaw);
        } else {
            throw new DataTransformationService.TransformationException("Unexpected value for item_type_code: " + itemTypeCodeRaw);
        }

        finalTransformedObj.put("itemClass", itemTypeCode);
        finalTransformedObj.put("priority",0);
        finalTransformedObj.put("caseMrp",0.00);
        finalTransformedObj.put("mrp",0.00);
        finalTransformedObj.put("otherUnitMrp",0.00);
        finalTransformedObj.put("otherUnitToPieceQuantity",0.00);
        finalTransformedObj.put("pieceToVolume",0.00);
        finalTransformedObj.put("skuPieceWeight",0);
        finalTransformedObj.put("skuOtherWeightUom",0);
        finalTransformedObj.put("skuCaseWeight",0);
        return finalTransformedObj;
    }

}
