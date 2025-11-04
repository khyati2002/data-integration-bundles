package com.applicate.cokesa.transformer;



import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserTransformerCokeSA extends AbstractTransformer<Map<String,Object>,List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createUser(inputMap));
        return responseList;
    }

    private Map<String, Object> createUser(Map <String, Object> inputMap){
        Map<String, Object> response = new HashMap<>();
        String loginId = inputMap.get("RS02_LOCATION02").toString() + inputMap.get("RS02_PERSONNEL2").toString();
        response.put("loginId", loginId);
        response.put("activeStatus", inputMap.get("RS02_PERAVIAL").toString().equals("1") ? "active" : "inactive");
        response.put("mobile","0000000000");
        response.put("userAccountId", loginId);
        response.put("name", inputMap.get("RS02_PERSNAME").toString());
        response.put("designation", getClientDesignation(inputMap.get("RS02_DRVHLPCD").toString()));
        response.put("locationHierarchy", getClientLocation(inputMap));
        response.put("immediateParent", inputMap.get("RS02_LOCATION02").toString());
        response.put("extendedAttributes",createExtended(inputMap));
        return response;
    }



    private JsonNode createExtended(Map<String, Object> inputMap){
        Map<String, Object> extended = new HashMap<>();
        if(inputMap.containsKey("RS02_FILLERL063") && NullUtils.isNotNull(inputMap.get("RS02_FILLERL063"))) extended.put("pinCode",inputMap.get("RS02_FILLERL063").toString());
        if(inputMap.containsKey("RS02_PAYROLNO") && NullUtils.isNotNull(inputMap.get("RS02_PAYROLNO")))   extended.put("payrollNo",inputMap.get("RS02_PAYROLNO").toString());
        if(inputMap.containsKey("RS02_LOCATION02") && NullUtils.isNotNull(inputMap.get("RS02_LOCATION02"))) extended.put("depoLocation",inputMap.get("RS02_LOCATION02").toString());
        return JSONUtils.toJsonNode(extended);
    }

    private Map<String, Object> getClientLocation(Map<String, Object> inputMap){
        Map<String, Object> location = new HashMap<>();
        location.put("country", "KSA");
        return location;
    }

    private String getClientDesignation(String userTypeVal) {
        if (NullUtils.isNull(userTypeVal)) {
            throw new DataTransformationService.TransformationException("Designation value must not be null");
        }
        switch (userTypeVal) {
            case "01":
                return "salesrep";
            case "06":
                return "vanseller";
            case "05":
                return "merchandiser";
            default:
                throw new DataTransformationService.TransformationException("Unrecognized designation type: " + userTypeVal);
        }
    }

    
}