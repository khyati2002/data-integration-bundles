package com.applicate.alsafi.transformer;
import com.applicate.services.channelkart.transformers.impl.JoltTransformer;
import java.util.HashMap;
import java.util.Map;
public class ProductDetailsTransformer extends JoltTransformer {
    @Override
    public Object transform(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        result.put("skuCode", input.get("ItemNo"));
        result.put("skuName", input.get("ItemNameE"));
        result.put("skuDescription", input.get("ItemNameEDesc"));
        result.put("batchCode", input.get("ItemNo"));
        result.put("productCode", input.get("ProductCode"));
        result.put("product", input.get("ProductName"));
        result.put("category", input.get("Category"));
        result.put("categoryCode", input.get("CategoryCode"));
        result.put("subCategory", input.get("SubCategory"));
        result.put("subCategoryCode", input.get("SubCategoryCode"));
        result.put("brand", input.get("Brand"));
        result.put("brandCode", input.get("BrandCode"));
        result.put("subBrand", input.get("SubBrand"));
        result.put("mrp", input.get("MRP"));
        result.put("caseMrp", input.get("CaseMRP"));
        result.put("otherUnitMrp", input.get("OtherUnitMRP"));
        result.put("caseToPieceQuantity", input.get("CaseToPiece"));
        result.put("caseToOtherUnitQuantity", input.get("CaseToOtherUnit"));
        result.put("otherUnitToPieceQuantity", input.get("OtherUnitToPiece"));
        result.put("pieceToOtherUnitQuantity", input.get("PieceToOtherUnit"));
        result.put("otherUnitName", input.get("OtherUnitName"));
        result.put("unitOfMeasurement", input.get("UOM"));
        result.put("channel", input.get("Channel"));
        result.put("size", input.get("Size"));
        result.put("pieceSize", input.get("PieceSize"));
        result.put("eanNumber", input.get("EANNumber"));
        result.put("fileName", input.get("ImageFileName"));
        result.put("blobKey", input.get("ImageURL"));
        result.put("priority", input.get("Priority"));
        result.put("display", input.get("Display"));

        return result;
    }
}