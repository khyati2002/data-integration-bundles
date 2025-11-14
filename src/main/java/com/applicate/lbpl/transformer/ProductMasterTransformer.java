package com.applicate.lbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.*;

public class ProductMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {

        Map<String, Object> output = new HashMap<>();

        // 1. skuCode = itemcode
        String itemcode = safeString(inputMap.get("itemcode"));
        output.put("skuCode", itemcode);

        // 2. batchCode = UpperCasefunc(itemcode)
        output.put("batchCode", itemcode != null ? itemcode.toUpperCase() : null);

        // 3. skuDescription = itemdescription
        output.put("skuDescription", safeString(inputMap.get("itemdescription")));

        // 4. itemType = categorycode1
        output.put("itemType", safeString(inputMap.get("categorycode1")));

        // 5. brand = categorycode2
        output.put("brand", safeString(inputMap.get("categorycode2")));

        // 6. category = Categoryfunction(categorycode3)
        output.put("category", categoryOrNA(inputMap.get("categorycode3")));

        // 7. pieceSizeDesc = Categoryfunction(categorycode5)
        output.put("pieceSizeDesc", categoryOrNA(inputMap.get("categorycode5")));

        // 8. brandCode = categorycode6
        output.put("brandCode", safeString(inputMap.get("categorycode6")));

        // 9. uom = baseuom
        output.put("uom", safeString(inputMap.get("baseuom")));

        // 10. mrp = mrp
        output.put("mrp", inputMap.get("mrp"));

        // 11. activeStatus = ActiveFunction(isactive)
        Object isactive = inputMap.get("isactive");
        output.put("activeStatus",
                (isactive != null && isactive.toString().equals("1")) ? "active" : "inactive"
        );

        // 12. itemName = shortdescription
        output.put("itemName", safeString(inputMap.get("shortdescription")));

        // 13. pieceToOtherUnitQuantity = pc_conversion
        output.put("pieceToOtherUnitQuantity", inputMap.get("pc_conversion"));

        // 14. caseToOtherUnitQuantity = uc_conversion
        output.put("caseToOtherUnitQuantity", inputMap.get("uc_conversion"));

        // 15. pieceSize = Categoryfunction(categorycode8)
        output.put("pieceSize", categoryOrNA(inputMap.get("categorycode8")));

        // 16. itemId = hierarchycode
        output.put("itemId", safeString(inputMap.get("hierarchycode")));

        // 17. subCategory = Categoryfunction(categorycode4)
        output.put("subCategory", categoryOrNA(inputMap.get("categorycode4")));

        // 18. caseToPieceQuantity = conversion1
        output.put("caseToPieceQuantity", inputMap.get("conversion1"));

        // 19. extendedAttributes = toMap(...)
        Map<String, Object> extended = new HashMap<>();

        List<String> extKeys = Arrays.asList(
                "tenantcode", "categorycode7", "categorycode20", "primary_rc", "secondary_rc",
                "unsorted_rc", "categoryattribute2", "itemshelflife", "weight", "hsncode",
                "stockcoverdays", "manufactureritemcode", "taxgroupcode", "lastmodifieddatetime"
        );

        for (String key : extKeys) {
            extended.put(key, inputMap.get(key));
        }

        output.put("extendedAttributes", extended);

        return output;
    }

    // Helpers
    private static String safeString(Object obj) {
        if (obj == null) return null;
        String s = obj.toString();
        return (s.equalsIgnoreCase("null") || s.trim().isEmpty()) ? null : s;
    }

    private static String categoryOrNA(Object obj) {
        String s = safeString(obj);
        return (s == null || s.trim().isEmpty()) ? "NA" : s;
    }
}
