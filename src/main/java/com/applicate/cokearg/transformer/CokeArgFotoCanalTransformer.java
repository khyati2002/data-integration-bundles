package com.applicate.cokearg.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public class CokeArgFotoCanalTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> responseMap = new HashMap<>();

        if (inputMap.containsKey("cluster") && inputMap.get("cluster") != null) {
            String cluster = inputMap.get("cluster").toString();
            responseMap.put("key1", cluster);

            try {
                ObjectNode extendedNode = objectMapper.createObjectNode();
                extendedNode.put("cluster", cluster);
                extendedNode.put("mision", getString(inputMap, "mision"));
                extendedNode.put("vision", getString(inputMap, "vision"));
                extendedNode.put("objetivos", getString(inputMap, "objetivos"));
                extendedNode.put("puertas", getString(inputMap, "puertas"));
                extendedNode.put("permanent", getString(inputMap, "permanent"));
                extendedNode.put("estrategia", getString(inputMap, "estrategia"));
                extendedNode.put("generico", getString(inputMap, "generico"));

                responseMap.put("payload", extendedNode);
                responseMap.put("name", "FotoCanal");

            } catch (Exception e) {
                System.err.println("⚠️ Error processing extended attributes: " + e.getMessage());
            }
        }

        return responseMap;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
