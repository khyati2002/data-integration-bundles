package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import java.util.HashMap;
import java.util.Map;

public class ENRICHProductMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        output.put("category", addENRICHIfPresent(inputMap.get("[translate:categorycode3]")));
        output.put("pieceSizeDesc", addENRICHIfPresent(inputMap.get("[translate:categorycode4]")));
        output.put("subCategory", addENRICHIfPresent(inputMap.get("[translate:categorycode5]")));
        output.put("flavour", addENRICHIfPresent(inputMap.get("[translate:categorycode6]")));
        output.put("pieceSize", addENRICHIfPresent(inputMap.get("[translate:categorycode8]")));
        output.put("itemType", addENRICHIfPresent(inputMap.get("[translate:categorycode1]")));
        output.put("brand", addENRICHIfPresent(inputMap.get("[translate:categorycode2]")));
        output.put("itemId", addENRICHIfPresent(inputMap.get("[translate:hierarchycode]")));
        output.put("batchCode", addENRICHIfPresent(inputMap.get("[translate:itemcode]")));
        output.put("skuCode", addENRICHIfPresent(inputMap.get("[translate:itemcode]")));

        output.put("skuDescription", inputMap.get("[translate:itemdescription]"));
        output.put("skuName", inputMap.get("[translate:itemdescription]"));

        output.put("caseToPieceQuantity", inputMap.get("[translate:conversion1]"));
        output.put("uom", inputMap.get("[translate:baseuom]"));
        output.put("pieceToOtherUnitQuantity", inputMap.get("[translate:pc_conversion]"));
        output.put("caseToOtherUnitQuantity", inputMap.get("[translate:uc_conversion]"));
        output.put("mrp", inputMap.get("[translate:mrp]"));

        output.put("activeStatus", setActiveFunctionkbpl(inputMap.get("[translate:isactive]")));

        output.put("source", toConstant());

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

    private String addENRICHIfPresent(Object param) {
        if (param != null) {
            String str = param.toString().trim();
            if (!str.isEmpty()) {
                return str.toUpperCase() + "-ENRICH";
            }
        }
        return "";
    }

    private String setActiveFunctionkbpl(Object isActive) {
        return (isActive != null && isActive.toString().equals("1")) ? "active" : "inactive";
    }

    private String toConstant() {
        return "ENRICH";
    }

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
