package com.applicate.alsafi.transformer;
import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.Map;
public class CategoryInfoTransformer extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        result.put("categoryCode", input.get("codeFromQuery"));
        result.put("categoryValue", input.get("valueFromQuery"));
        result.put("feature", input.get("featureFromQuery"));
        return result;
    }
}