package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;

import java.util.HashMap;
import java.util.Map;

public class SimaGenericScheme extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {
    @Override
    public Map<String,Object> transform(Map<String, Object> stringObjectMap)
    {
        HashMap<String,Object> finalTransformedObj = new HashMap<>();
        JsonNode stringObjectMapJson = JSONUtils.getObjectMapper().convertValue(stringObjectMap, JsonNode.class);
        finalTransformedObj.put("key1",stringObjectMapJson.get("AdjustmentListNumber").asText());
        finalTransformedObj.put("id",stringObjectMapJson.get("AdjustmentListNumber").asText());
        finalTransformedObj.put("name","GenericSchemeData");
        String adjustmenttype = stringObjectMapJson.get("AdjustmentType").asText();
        if (!("17".equals(adjustmenttype) || "02".equals(adjustmenttype) || "03".equals(adjustmenttype) || "2".equals(adjustmenttype) || "3".equals(adjustmenttype)))
            throw new DataTransformationService.TransformationException("Invalid Adjustment type");
        finalTransformedObj.put("payload", stringObjectMapJson);


        return finalTransformedObj;
    }
}
