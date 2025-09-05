package com.applicate.unnati.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class OutletMetadataTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) return Collections.emptyMap();
        Map<String, Object> outletMetadataMap = new LinkedHashMap<>();
        outletMetadataMap.put("outletUniqueCode", inputMap.get("outletUniqueCode"));
        outletMetadataMap.put("outletId", inputMap.get("outletId"));
        outletMetadataMap.put("salesrep", inputMap.get("salesrep"));
        outletMetadataMap.put("activeStatusReason", inputMap.get("activeStatusReason"));
        outletMetadataMap.put("activeStatus", inputMap.get("activeStatus"));
        outletMetadataMap.put("status", inputMap.get("status"));
        outletMetadataMap.put("outletCode", inputMap.get("outletCode"));
        outletMetadataMap.put("EXCLUDED_PROPERTIES", inputMap.get("EXCLUDED_PROPERTIES"));
        outletMetadataMap.put("preProcessPipelineException", inputMap.get("preProcessPipelineException"));
        outletMetadataMap.put("groupKey", inputMap.get("groupKey"));
        outletMetadataMap.put("supplierCode", inputMap.get("supplierCode"));
        outletMetadataMap.put("customerCode", inputMap.get("customerCode"));
        outletMetadataMap.put("serialVersionUID", inputMap.get("serialVersionUID"));
        outletMetadataMap.put("division", inputMap.get("division"));
        outletMetadataMap.put("syncedTime", inputMap.get("syncedTime"));
        outletMetadataMap.put("loginId", inputMap.get("loginId"));
        outletMetadataMap.put("source", inputMap.get("source"));
        outletMetadataMap.put("extendedAttributes", inputMap.get("extendedAttributes"));
        outletMetadataMap.put("supplierUniqueCode", inputMap.get("supplierUniqueCode"));

        return outletMetadataMap;
    }
}
