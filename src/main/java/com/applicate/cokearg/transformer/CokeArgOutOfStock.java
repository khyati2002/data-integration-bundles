package com.applicate.cokearg.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.*;

public class CokeArgOutOfStock extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TRANSFORM_NAME = "OutOfStockMaster";
    private static final String LOCATION_KEY = "location";
    private static final String SKU_LIST_KEY = "sku_list";

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null || inputMap.isEmpty()) {
            return Collections.emptyMap();
        }

        String location = getString(inputMap, LOCATION_KEY);
        List<String> skuList = parseSkuList(inputMap.get(SKU_LIST_KEY));

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", TRANSFORM_NAME + "_" + location);
        result.put("name", TRANSFORM_NAME);
        result.put("key1", location);
        result.put("payload", createPayload(location, skuList));

        return result;
    }

    private ObjectNode createPayload(String location, List<String> skuList) {
        ObjectNode payloadNode = objectMapper.createObjectNode();
        ArrayNode skuArrayNode = objectMapper.createArrayNode();

        if (skuList != null) {
            for (String sku : skuList) {
                if (sku != null && !sku.trim().isEmpty()) {
                    skuArrayNode.add(sku.trim());
                }
            }
        }

        payloadNode.set(location, skuArrayNode);
        return payloadNode;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }

    private List<String> parseSkuList(Object skuListObj) {
        if (skuListObj == null) return Collections.emptyList();

        // Already a list
        if (skuListObj instanceof List) {
            List<?> list = (List<?>) skuListObj;
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) result.add(item.toString().trim());
            }
            return result;
        }

        // JSON string or delimited string
        if (skuListObj instanceof String) {
            String skuListStr = ((String) skuListObj).replaceAll("[\\[\\]\"]", "");
            String[] parts = skuListStr.split(",\\s*");
            return Arrays.asList(parts);
        }

        return Collections.emptyList();
    }
}
