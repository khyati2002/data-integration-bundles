package com.applicate.alsafi.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;


import java.util.*;

public class UserTransformer extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {
    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("loginId", inputMap.get("loginId"));
        userMap.put("userAccountId", inputMap.get("loginId"));
        userMap.put("mobile","0000000000");
        userMap.put("name", inputMap.get("name"));
        Map<String, Object> hierarchyMetadata = new HashMap<>();
        hierarchyMetadata.put("immediateParent", inputMap.get("parent"));
        userMap.put("immediateParent", List.of(hierarchyMetadata));
        Set<String> designation = new HashSet<>();
        designation.add(inputMap.get("designation").toString());
        userMap.put("designation", designation);
        userMap.put("locationHierarchy", getLocationObject(inputMap));
        return userMap;
    }

    private Map<String, Object> getLocationObject(Map<String, Object> inputMap){
        Map<String, Object> locationObject = new HashMap<>();
        locationObject.put("country", "Saudi Arabia");
        checkAndUpdate("region", inputMap, locationObject);
        checkAndUpdate("branch", inputMap, locationObject);
        checkAndUpdate("area", inputMap, locationObject);
        checkAndUpdate("district", inputMap, locationObject);
        return locationObject;
    }

    private void checkAndUpdate(String key, Map<String, Object> inputMap, Map<String, Object> locationObject){
        if(inputMap.containsKey(key) && inputMap.get(key) != null){
            locationObject.put(key, inputMap.get(key));
        }
    }


}
