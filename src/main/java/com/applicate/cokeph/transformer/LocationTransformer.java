package com.applicate.cokeph.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.Map;

public class LocationTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    private static final String REGION = "Region";
    private static final String PROVINCE = "Province";
    private static final String CITY = "City";
    private static final String BARANGAY = "Barangay";
    private static final String MUNICIPALITY = "Municipality";

    @Override
    public Map<String, Object> transform(Map<String,Object> input) {
        Map<String, Object> transformed = new HashMap<>();

        putIfValid(transformed, "region", input.get(REGION));
        putIfValid(transformed, "state", input.get(PROVINCE));
        putIfValid(transformed, "city", input.get(CITY));
        putIfValid(transformed, "area", input.get(MUNICIPALITY));
        putIfValid(transformed, "town", input.get(BARANGAY));
        transformed.put("source", "custom");
        transformed.put("country", "Philippines");

        return transformed;
    }

    private void putIfValid(Map<String, Object> map, String key, Object value) {
        if (value == null) return;

        String val = value.toString().trim();
        if (!val.isEmpty() && !val.equals("-")) {
            map.put(key, val);
        }
    }
}
