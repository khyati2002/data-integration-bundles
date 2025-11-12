package com.applicate.cokearg.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

/**
 * CokeArgUserTransformer:
 * Transforms raw Coke Argentina user data into standardized user data format.
 * Maps local field names like "codigo", "rolename", etc. into system-level fields.
 */
public class CokeArgUserTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

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
        responseMap.put("immediateParent", (codigoSuperior == null || codigoSuperior.isEmpty())
                ? "arg_dist"
                : codigoSuperior);

        // Map designation based on role name
        String roleName = getString(inputMap, "rolename");
        if (roleName != null && designationMap.containsKey(roleName)) {
            responseMap.put("designation", designationMap.get(roleName));
        } else {
            responseMap.put("designation", "unknown");
        }

        // Add fixed location hierarchy
        Map<String, Object> location = new HashMap<>();
        location.put("country", "Argentina");
        responseMap.put("locationHierarchy", location);

        // Add extended attributes (loc and ruta)
        ObjectNode extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
        extendedAttributes.put("loc", getString(inputMap, "locacion"));
        extendedAttributes.put("ruta", getString(inputMap, "ruta"));
        responseMap.put("extendedAttributes", extendedAttributes);

        return responseMap;
    }

    /** Utility: safely extract string from map */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }
}
