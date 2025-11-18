package com.applicate.alsafi.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.Map;

public class UserTransformer extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {
    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("loginId", inputMap.get("loginId"));
        userMap.put("mobile","0000000000");
        userMap.put("name", inputMap.get("name"));
        userMap.put("immediateParent", inputMap.get("parent"));
        userMap.put("designation", inputMap.get("designation"));
        Map<String, Object> location = new HashMap<>();
        location.put("country", "Saudi Arabia");
        if(inputMap.containsKey("region")) location.put("test", "test");
        userMap.put("locationHierarchy", location);
        return userMap;
    }
    



}
