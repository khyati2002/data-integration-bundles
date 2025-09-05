package com.applicate.unnati.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScoreDetailsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> scoreDetailsMap = new LinkedHashMap<>();

        scoreDetailsMap.put("outletCode", inputMap.get("outletCode"));
        scoreDetailsMap.put("startDate", inputMap.get("startDate"));
        scoreDetailsMap.put("endDate", inputMap.get("endDate"));
        scoreDetailsMap.put("totalPoints", inputMap.get("totalPoints"));
        scoreDetailsMap.put("currentVolumn", inputMap.get("currentVolumn"));
        scoreDetailsMap.put("activeStatusReason", inputMap.get("activeStatusReason"));
        scoreDetailsMap.put("closingPoints", inputMap.get("closingPoints"));
        scoreDetailsMap.put("activeStatus", inputMap.get("activeStatus"));
        scoreDetailsMap.put("EXCLUDED_PROPERTIES", inputMap.get("EXCLUDED_PROPERTIES"));
        scoreDetailsMap.put("preProcessPipelineException", inputMap.get("preProcessPipelineException"));
        scoreDetailsMap.put("programNumber", inputMap.get("programNumber"));
        scoreDetailsMap.put("serialVersionUID", inputMap.get("serialVersionUID"));
        scoreDetailsMap.put("division", inputMap.get("division"));
        scoreDetailsMap.put("feature", inputMap.get("feature"));
        scoreDetailsMap.put("source", inputMap.get("source"));
        scoreDetailsMap.put("openingPoints", inputMap.get("openingPoints"));
        scoreDetailsMap.put("extendedAttributes", inputMap.get("extendedAttributes"));
        scoreDetailsMap.put("pointsBreakup", inputMap.get("pointsBreakup"));
        scoreDetailsMap.put("locationHierarchy", inputMap.get("locationHierarchy"));
        scoreDetailsMap.put("loginId", inputMap.get("loginId"));

        return scoreDetailsMap;
    }
}

