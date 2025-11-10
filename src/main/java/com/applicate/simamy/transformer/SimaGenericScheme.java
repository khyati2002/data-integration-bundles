package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

public class SimaGenericScheme extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {
    @Override
    public Object transform(Map<String, Object> stringObjectMap)
    {
        HashMap<String,Object> finalTransformedObj = new HashMap<>();
        JsonNode stringObjectMapJson = JSONUtils.toJsonNode(stringObjectMap);
        finalTransformedObj.put("key1",stringObjectMapJson.get("AdjustmentListNumber").asText());
        finalTransformedObj.put("id",stringObjectMapJson.get("AdjustmentListNumber").asText());
        finalTransformedObj.put("name","GenericSchemeData");
        String adjustmenttype = stringObjectMapJson.get("AdjustmentType").asText();
        if (!("17".equals(adjustmenttype) || "02".equals(adjustmenttype) || "03".equals(adjustmenttype) || "2".equals(adjustmenttype) || "3".equals(adjustmenttype)))
            throw new TransformationException("Invalid Adjustment type");
        finalTransformedObj.put("payload", stringObjectMapJson);


        return finalTransformedObj;
    }
}
