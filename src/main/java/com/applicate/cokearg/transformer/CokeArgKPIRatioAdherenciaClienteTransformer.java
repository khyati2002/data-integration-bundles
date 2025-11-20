package com.applicate.cokearg.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public class CokeArgKPIRatioAdherenciaClienteTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> responseMap = new HashMap<>();
        if (inputMap.containsKey("ruta") && inputMap.get("ruta") != null) {
            responseMap.put("value", inputMap.get("ruta").toString());
            responseMap.put("name","kpi_ratio_adherencia_ruta");
            responseMap.put("target","USER");
        }
        if (inputMap.containsKey("as_cliente") && inputMap.get("as_cliente") != null) {
            responseMap.put("outletCode", inputMap.get("as_cliente").toString());
            responseMap.put("name","kpi_ratio_adherencia_cliente");
            responseMap.put("target","OUTLET");
            responseMap.put("value",inputMap.get("razonsocial"));
        }
        if (inputMap.containsKey("supervisor") && inputMap.get("supervisor") != null) {
            responseMap.put("loginId", inputMap.get("supervisor").toString());
            responseMap.put("name","kpi_ratio_adherencia_supervisor");
            responseMap.put("target","USER");
        }


        Map<String, Object> eaMap = new HashMap<>();
        eaMap.put("ratio", inputMap.get("ratio"));
        eaMap.put("adh_act", inputMap.get("adhact"));
        eaMap.put("adh_py", inputMap.get("adhpy"));
        eaMap.put("delta_adh", inputMap.get("deltaadh"));
        eaMap.put("obj_ratio", inputMap.get("objratio"));

        ObjectNode extendedAttributes = objectMapper.convertValue(eaMap, ObjectNode.class);
        responseMap.put("extendedAttributes", extendedAttributes);
        return responseMap;

    }

}
