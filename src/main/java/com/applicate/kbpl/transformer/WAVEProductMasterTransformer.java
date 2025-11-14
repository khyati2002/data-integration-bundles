package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.Map;

public class WAVEProductMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        // Mappings with addWAVEIfPresent function
        output.put("category", addWAVEIfPresent(inputMap.get("[translate:categorycode3]")));
        output.put("pieceSizeDesc", addWAVEIfPresent(inputMap.get("[translate:categorycode4]")));
        output.put("subCategory", addWAVEIfPresent(inputMap.get("[translate:categorycode5]")));
        output.put("flavour", addWAVEIfPresent(inputMap.get("[translate:categorycode6]")));
        output.put("pieceSize", addWAVEIfPresent(inputMap.get("[translate:categorycode8]")));
        output.put("itemType", addWAVEIfPresent(inputMap.get("[translate:categorycode1]")));
        output.put("brand", addWAVEIfPresent(inputMap.get("[translate:categorycode2]")));
        output.put("itemId", addWAVEIfPresent(inputMap.get("[translate:hierarchycode]")));
        output.put("batchCode", addWAVEIfPresent(inputMap.get("[translate:itemcode]")));
        output.put("skuCode", addWAVEIfPresent(inputMap.get("[translate:itemcode]")));

        // Direct field mappings
        output.put("skuDescription", inputMap.get("[translate:itemdescription]"));
        output.put("skuName", inputMap.get("[translate:itemdescription]"));

        output.put("caseToPieceQuantity", inputMap.get("[translate:conversion1]"));
        output.put("uom", inputMap.get("[translate:baseuom]"));
        output.put("pieceToOtherUnitQuantity", inputMap.get("[translate:pc_conversion]"));
        output.put("caseToOtherUnitQuantity", inputMap.get("[translate:uc_conversion]"));
        output.put("mrp", inputMap.get("[translate:mrp]"));

        // activeStatus field transformation: "1" -> "active", else "inactive"
        output.put("activeStatus", setActiveFunctionkbpl(inputMap.get("[translate:isactive]")));

        // Constant field "source" = "WAVE"
        output.put("source", toConstant());

        // Extended attributes map from multiple keys
        output.put("extendedAttributes", toMap(
                inputMap.get("[translate:eannumber]"),
                inputMap.get("[translate:categorycode7]"),
                inputMap.get("[translate:stockcoverdays]"),
                inputMap.get("[translate:packsize]"),
                inputMap.get("[translate:taxgroupcode]"),
                inputMap.get("[translate:itemshelflife]"),
                inputMap.get("[translate:tenantcode]"),
                inputMap.get("[translate:shortdescription]"),
                inputMap.get("[translate:weight]"),
                inputMap.get("[translate:manufactureritemcode]"),
                inputMap.get("[translate:hsncode]"),
                inputMap.get("[translate:lastmodifieddatetime]")
        ));

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

