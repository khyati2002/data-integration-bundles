package com.applicate.unnati.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import java.util.LinkedHashMap;
import java.util.Map;

public class TargetsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> targetsMap = new LinkedHashMap<>();

        targetsMap.put("endDate", inputMap.get("endDate"));
        targetsMap.put("startDate", inputMap.get("startDate"));
        targetsMap.put("userValueStr", inputMap.get("userValueStr"));
        targetsMap.put("userValue", inputMap.get("userValue"));
        targetsMap.put("targetName", inputMap.get("targetName"));
        targetsMap.put("outletValue", inputMap.get("outletValue"));
        targetsMap.put("outletValueStr", inputMap.get("outletValueStr"));
        targetsMap.put("targetId", inputMap.get("targetId"));
        targetsMap.put("targetTable", inputMap.get("targetTable"));
        targetsMap.put("targetConditionUnit", inputMap.get("targetConditionUnit"));
        targetsMap.put("targetCondition", inputMap.get("targetCondition"));
        targetsMap.put("target", inputMap.get("target"));
        targetsMap.put("targetType", inputMap.get("targetType"));
        targetsMap.put("unit", inputMap.get("unit"));
        targetsMap.put("productType", inputMap.get("productType"));
        targetsMap.put("productValue", inputMap.get("productValue"));
        targetsMap.put("outletType", inputMap.get("outletType"));
        targetsMap.put("activeStatusReason", inputMap.get("activeStatusReason"));
        targetsMap.put("activeStatus", inputMap.get("activeStatus"));
        targetsMap.put("EXCLUDED_PROPERTIES", inputMap.get("EXCLUDED_PROPERTIES"));
        targetsMap.put("preProcessPipelineException", inputMap.get("preProcessPipelineException"));
        targetsMap.put("division", inputMap.get("division"));
        targetsMap.put("source", inputMap.get("source"));
        targetsMap.put("extendedAttributes", inputMap.get("extendedAttributes"));

        return targetsMap;
    }
}
