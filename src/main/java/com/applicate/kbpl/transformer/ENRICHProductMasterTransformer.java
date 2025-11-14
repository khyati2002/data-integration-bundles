package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import java.util.HashMap;
import java.util.Map;

public class ENRICHProductMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        output.put("category", addENRICHIfPresent(inputMap.get("categorycode3")));
        output.put("pieceSizeDesc", addENRICHIfPresent(inputMap.get("categorycode4")));
        output.put("subCategory", addENRICHIfPresent(inputMap.get("categorycode5")));
        output.put("flavour", addENRICHIfPresent(inputMap.get("categorycode6")));
        output.put("pieceSize", addENRICHIfPresent(inputMap.get("categorycode8")));
        output.put("itemType", addENRICHIfPresent(inputMap.get("categorycode1")));
        output.put("brand", addENRICHIfPresent(inputMap.get("categorycode2")));
        output.put("itemId", addENRICHIfPresent(inputMap.get("hierarchycode")));
        output.put("batchCode", addENRICHIfPresent(inputMap.get("itemcode")));
        output.put("skuCode", addENRICHIfPresent(inputMap.get("itemcode")));

        output.put("skuDescription", inputMap.get("itemdescription"));
        output.put("skuName", inputMap.get("itemdescription"));

        output.put("caseToPieceQuantity", inputMap.get("conversion1"));
        output.put("uom", inputMap.get("baseuom"));
        output.put("pieceToOtherUnitQuantity", inputMap.get("pc_conversion"));
        output.put("caseToOtherUnitQuantity", inputMap.get("uc_conversion"));
        output.put("mrp", inputMap.get("mrp"));

        output.put("activeStatus", setActiveFunctionkbpl(inputMap.get("isactive")));

        output.put("source", toConstant());

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
