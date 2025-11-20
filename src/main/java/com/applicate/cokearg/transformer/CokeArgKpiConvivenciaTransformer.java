package com.applicate.cokearg.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public class CokeArgKpiConvivenciaTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> responseMap = new HashMap<>();
        if (inputMap.containsKey("ruta") && inputMap.get("ruta") != null) {
            responseMap.put("value", inputMap.get("ruta").toString());
            responseMap.put("name","kpi_convivencia_ruta");
            responseMap.put("target","USER");
        }
        if (inputMap.containsKey("supervisor") && inputMap.get("supervisor") != null) {
            responseMap.put("loginId", inputMap.get("supervisor").toString());
            responseMap.put("name","kpi_convivencia_supervisor");
            responseMap.put("target","USER");
        }

        Map<String, Object> eaMap = new HashMap<>();
        eaMap.put("objetivo_alcanzado", inputMap.get("objetivoalcanzado"));
        eaMap.put("objetivo", inputMap.get("objetivo"));
        eaMap.put("faltante",inputMap.get("faltante"));
        eaMap.put("mtd", inputMap.get("mtd"));
        eaMap.put("convivencia", inputMap.get("convivencia"));

        ObjectNode extendedAttributes = objectMapper.convertValue(eaMap, ObjectNode.class);
        responseMap.put("extendedAttributes", extendedAttributes);
        return responseMap;

    }

}
