package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaxMasterGenCokeSa extends AbstractTransformer<Map<String,Object>,List<Map<String, Object>>> {
    

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createResponse(inputMap));
        return responseList;
    }

    private Map<String, Object> createResponse(Map <String, Object> inputMap){
        Map<String, Object> response = new HashMap<>();
        response.put("id", inputMap.get("AM09_TAXCOD").toString()+ "-" +inputMap.get("AM09_PRIELEPNT").toString()+ "-TaxDefined");
        response.put("name", "TaxDefined");
        response.put("key1", inputMap.get("AM09_TAXCOD").toString());
        response.put("key2", inputMap.get("AM09_PRIELEPNT").toString());
        response.put("key3", inputMap.get("AM09_TAXBASIND").toString());
        response.put("payload", createPayload(inputMap));
        return response;
    }

    private JsonNode createPayload(Map<String, Object> inputMap){
        Map<String, Object> payload = new HashMap<>();
        payload.put("taxName", inputMap.get("AM09_TAXNAM").toString());
        payload.put("taxNameAr", inputMap.get("AM09_TAXNAM2").toString());
        payload.put("fromDate", inputMap.get("AM09_EFRDAT").toString());
        payload.put("toDate", inputMap.get("AM09_EFTDAT").toString());
        payload.put("taxCode", inputMap.get("AM09_TAXCOD").toString());
        payload.put("taxProgram", inputMap.get("AM09_TAXCLS").toString().equals("V") ? "VAT" : "EXCISE");
        payload.put("priceElementColumn", inputMap.get("AM09_PRIELEPNT").toString());
        payload.put("taxType", inputMap.get("AM09_TAXBASIND").toString().equals("1") ? "percentage" : "value");
        payload.put("taxValueIfPercentage", inputMap.get("AM09_TAXRATPCT").toString());
        return JSONUtils.toJsonNode(payload);
    }
}
