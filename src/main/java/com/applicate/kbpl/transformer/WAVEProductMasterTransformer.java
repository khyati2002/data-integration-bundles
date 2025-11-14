package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import java.util.HashMap;
import java.util.Map;

public class WAVEProductMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        // Mappings with addWAVEIfPresent function
        output.put("category", addWAVEIfPresent(inputMap.get("categorycode3")));
        output.put("pieceSizeDesc", addWAVEIfPresent(inputMap.get("categorycode4")));
        output.put("subCategory", addWAVEIfPresent(inputMap.get("categorycode5")));
        output.put("flavour", addWAVEIfPresent(inputMap.get("categorycode6")));
        output.put("pieceSize", addWAVEIfPresent(inputMap.get("categorycode8")));
        output.put("itemType", addWAVEIfPresent(inputMap.get("categorycode1")));
        output.put("brand", addWAVEIfPresent(inputMap.get("categorycode2")));
        output.put("itemId", addWAVEIfPresent(inputMap.get("hierarchycode")));
        output.put("batchCode", addWAVEIfPresent(inputMap.get("itemcode")));
        output.put("skuCode", addWAVEIfPresent(inputMap.get("itemcode")));

        // Direct field mappings
        output.put("skuDescription", inputMap.get("itemdescription"));
        output.put("skuName", inputMap.get("itemdescription"));

        output.put("caseToPieceQuantity", inputMap.get("conversion1"));
        output.put("uom", inputMap.get("baseuom"));
        output.put("pieceToOtherUnitQuantity", inputMap.get("pc_conversion"));
        output.put("caseToOtherUnitQuantity", inputMap.get("uc_conversion"));
        output.put("mrp", inputMap.get("mrp"));

        // activeStatus field transformation: "1" -> "active", else "inactive"
        output.put("activeStatus", setActiveFunctionkbpl(inputMap.get("isactive")));

        // Constant field "source" = "WAVE"
        output.put("source", toConstant());

        // Extended attributes map from multiple keys
        output.put("extendedAttributes", toMap(
                inputMap.get("eannumber"),
                inputMap.get("categorycode7"),
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

    // Converts input param to uppercase + "-WAVE" if present, else empty string
    private String addWAVEIfPresent(Object param) {
        if (param != null) {
            String str = param.toString().trim();
            if (!str.isEmpty()) {
                return str.toUpperCase() + "-WAVE";
            }
        }
        return "";
    }

    // Converts "1" to "active", else "inactive"
    private String setActiveFunctionkbpl(Object isActive) {
        return (isActive != null && isActive.toString().equals("1")) ? "active" : "inactive";
    }

    // Returns constant string "WAVE"
    private String toConstant() {
        return "WAVE";
    }

    // Builds extended attributes map from all given parameters
    private Map<String, Object> toMap(Object... values) {
        String[] keys = {
                "eannumber", "categorycode7", "stockcoverdays", "packsize", "taxgroupcode",
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
