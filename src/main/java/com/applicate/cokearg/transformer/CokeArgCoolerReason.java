package com.applicate.cokearg.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class CokeArgCoolerReason extends AbstractTransformer<Map<String,Object>,Map<String,Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("key1", inputMap.get("prefix").toString());
        responseMap.put("name", "EquipmentReasonMaster");

        ObjectNode payload = objectMapper.createObjectNode();

        try {
            Object data = inputMap.get("lista_motivos");

            if (data != null) {
                String dataStr = data.toString();

                // Split by comma and process each reason
                String[] reasons = dataStr.split("\\s*,\\s*");


                ArrayNode reasonsArray = objectMapper.createArrayNode();

                for (String reason : reasons) {
                    try {

                        String cleanReason = reason.replace("+", " ").trim();

                        // Try URL decoding first (for cases with %F1, %F3, etc.)
                        String decodedReason = URLDecoder.decode(cleanReason, "ISO-8859-1");

                        // Handle common corrupted characters
                        decodedReason = decodedReason.replace("", "ó"); // Replace  with ó

                        reasonsArray.add(decodedReason);
                    } catch (Exception decodeException) {
                        // If URL decoding fails, fall back to just replacing + with space and fixing corrupted chars
                        String fallbackReason = reason.replace("+", " ").trim();
                        fallbackReason = fallbackReason.replace("", "ó"); // Replace  with ó
                        reasonsArray.add(fallbackReason);
                    }
                }

                payload.set("reasons", reasonsArray);
            } else {
                payload.set("reasons", objectMapper.createArrayNode());
            }

        } catch (Exception e) {
            payload.put("error", "Failed to parse lista_motivos: " + e.getMessage());
        }

        responseMap.put("payload", payload);
        return responseMap;
    }
}
