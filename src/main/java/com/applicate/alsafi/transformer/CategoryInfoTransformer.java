package com.applicate.alsafi.transformer;
import com.applicate.services.channelkart.transformers.impl.JoltTransformer;
import java.util.HashMap;
import java.util.Map;
public class CategoryTransformer extends JoltTransformer {
    @Override
    public Object transform(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();
        result.put("categoryCode", input.get("codeFromQuery"));
        result.put("categoryValue", input.get("valueFromQuery"));
        result.put("feature", inputMap.get("featureFromQuery"));

        return result;
    }
}