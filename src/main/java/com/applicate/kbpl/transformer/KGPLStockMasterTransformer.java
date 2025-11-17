package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import java.util.HashMap;
import java.util.Map;

public class KGPLStockMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> output = new HashMap<>();

        // Append "-KGPL" to itemcode for skuCode and batchCode
        Object itemcode = inputMap.get("itemcode");
        output.put("skuCode", appendKGPL(itemcode));
        output.put("batchCode", appendKGPL(itemcode));

        // Append "-KGPL" to tenantcode for supplier
        Object tenantcode = inputMap.get("tenantcode");
        output.put("supplier", appendKGPL(tenantcode));

        // Direct mappings
        output.put("qty", inputMap.get("stockquantity"));
        output.put("initialQty", inputMap.get("stockquantity1"));

        // batchId created by concatenating itemcode, productiondate and expiredate with "-KGPL-"
        Object productiondate = inputMap.get("productiondate");
        Object expiredate = inputMap.get("expiredate");
        output.put("batchId", createBatchId(itemcode, productiondate, expiredate));

        // minQty constant -1
        output.put("minQty", -1);

        // extendedAttributes map from specified keys
        output.put("extendedAttributes", toMap(
                inputMap.get("productiondate"),
                inputMap.get("subhierarchycode"),
                inputMap.get("mrp"),
                inputMap.get("itemtypecode"),
                inputMap.get("expiredate"),
                inputMap.get("stockquantity1")
        ));

        return output;
    }

    private String appendKGPL(Object param) {
        if (param != null) {
            String str = param.toString().trim();
            if (!str.isEmpty()) {
                return str + "-KGPL";
            }
        }
        return "";
    }

    private String createBatchId(Object itemcode, Object productiondate, Object expiredate) {
        String item = itemcode != null ? itemcode.toString() : "";
        String prodDate = productiondate != null ? productiondate.toString() : "";
        String expDate = expiredate != null ? expiredate.toString() : "";
        return item + "-KGPL-" + prodDate + "-" + expDate;
    }

    private Map<String, Object> toMap(Object... values) {
        String[] keys = {
                "productiondate", "subhierarchycode", "mrp", "itemtypecode", "expiredate", "stockquantity1"
        };
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keys.length && i < values.length; i++) {
            map.put(keys[i], values[i]);
        }
        return map;
    }
}
