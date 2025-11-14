package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import java.util.HashMap;
import java.util.Map;

public class KBPLProductMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        // Direct mappings
        output.put("category", inputMap.get("categorycode3"));
        output.put("skuDescription", inputMap.get("itemdescription"));
        output.put("skuName", inputMap.get("itemdescription"));
        output.put("pieceSizeDesc", inputMap.get("categorycode4"));
        output.put("subCategory", inputMap.get("categorycode5"));
        output.put("flavour", inputMap.get("categorycode6"));
        output.put("caseToPieceQuantity", inputMap.get("conversion1"));
        output.put("pieceSize", inputMap.get("categorycode8"));
        output.put("itemType", inputMap.get("categorycode1"));
        output.put("brand", inputMap.get("categorycode2"));
        output.put("uom", inputMap.get("baseuom"));
        output.put("pieceToOtherUnitQuantity", inputMap.get("pc_conversion"));
        output.put("caseToOtherUnitQuantity", inputMap.get("uc_conversion"));
        output.put("mrp", inputMap.get("mrp"));
        output.put("itemId", inputMap.get("hierarchycode"));
        output.put("mCode", inputMap.get("hierarchycode"));
        output.put("batchCode", inputMap.get("itemcode"));
        output.put("skuCode", inputMap.get("itemcode"));

        // activeStatus via setActiveFunctionkbpl
        output.put("activeStatus", setActiveFunctionkbpl(inputMap.get("isactive")));

        // source constant "KBL"
        output.put("source", toConstant());

        // extendedAttributes map with multiple fields
        output.put("extendedAttributes", toMap(
                inputMap.get("categorycode7"),
                inputMap.get("eannumber"),
                inputMap.get("stockcoverdays"),
                inputMap.get("packsize"),
                inputMap.get("taxgroupcode"),
                inputMap.get("itemshelflife"),
                inputMap.get("tenantcode"),
                inputMap.get("shortdescription"),
                inputMap.get("weight"),
                inputMap.get("manufactureritemcode"),
                inputMap.get("hsncode"),
                inputMap.get("lastmodifieddatetime")
        ));

        if (!output.containsKey("priority") || output.get("priority") == null) {
            output.put("priority", 0);
        }

        return output;
    }

    private String setActiveFunctionkbpl(Object isActive) {
        return (isActive != null && isActive.toString().equals("1")) ? "active" : "inactive";
    }

    private String toConstant() {
        return "KBL";
    }

    private Map<String, Object> toMap(Object... values) {
        String[] keys = {
                "categorycode7", "eannumber", "stockcoverdays", "packsize", "taxgroupcode",
                "itemshelflife", "tenantcode", "shortdescription", "weight",
                "manufactureritemcode", "hsncode", "lastmodifieddatetime"
        };

        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keys.length && i < values.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }
}
