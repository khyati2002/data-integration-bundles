package com.applicate.cokearg.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * CokeArgUserTransformer
 * Compatible with Flink-shaded Jackson libraries.
 */
public class CokeArgUserTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> responseMap = new HashMap<>();
        Map<String, String> designationMap = new HashMap<>();

        // Define mapping between input role names and standardized internal designations
        designationMap.put("Preventista", "preseller");
        designationMap.put("Jefe de zona", "jefedezona");
        designationMap.put("Supervisor", "supervisor");
        designationMap.put("Gerente comercial", "gerentecomercial");
        designationMap.put("Telemarketing", "telemarketing");
        designationMap.put("Relevo", "relevo");
        designationMap.put("supplier", "supplier");

        // Map direct fields
        responseMap.put("loginId", getString(inputMap, "codigo"));
        responseMap.put("userAccountId", getString(inputMap, "codigo"));
        responseMap.put("name", getString(inputMap, "nombre"));
        responseMap.put("email", getString(inputMap, "email"));
        responseMap.put("mobile", getString(inputMap, "telefono"));

        // Immediate parent handling — default to distributor if missing
        String codigoSuperior = getString(inputMap, "codigo_superior");
        responseMap.put("immediateParent",
                (codigoSuperior == null || codigoSuperior.isEmpty()) ? "arg_dist" : codigoSuperior);

        // Map designation based on role name
        String roleName = getString(inputMap, "rolename");
        responseMap.put("designation", designationMap.getOrDefault(roleName, "unknown"));

        // Add fixed location hierarchy
        Map<String, Object> location = new HashMap<>();
        location.put("country", "Argentina");
        responseMap.put("locationHierarchy", location);

        // Create Flink-compatible ObjectNode for extended attributes
        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        extendedAttributes.put("loc", getString(inputMap, "locacion"));
        extendedAttributes.put("ruta", getString(inputMap, "ruta"));
        responseMap.put("extendedAttributes", extendedAttributes);

        return responseMap;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }
}
