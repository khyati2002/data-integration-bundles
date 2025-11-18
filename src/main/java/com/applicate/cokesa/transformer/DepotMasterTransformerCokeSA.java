package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepotMasterTransformerCokeSA extends AbstractTransformer<Map<String,Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createDepotUser(inputMap));
        responseList.add(createDepotOutlet(inputMap));
        return responseList;

    }

    private Map<String, Object> createDepotOutlet(Map<String, Object> inputMap) {

        Map<String, Object> response = new HashMap<>();
        if(NullUtils.isNull(inputMap.get("XI30_LOC"))) throw new DataTransformationService.TransformationException("Depot Code cannot be null");

        response.put("outletCode",inputMap.get("XI30_LOC").toString());
        response.put("activeStatus", "active");
        response.put("contactno", "0000000000");
        response.put("outletName", inputMap.get("XI30_LOCNAM").toString());
        response.put("locationHierarchy", getUserLocation());

        Map<String, Object> extendedAttributes = new HashMap<>();
        extendedAttributes.put("CRNumber", inputMap.get("XI30_NATCPYNUM"));

        response.put("extendedAttributes", JSONUtils.toJsonNode(extendedAttributes));
        return response;

    }

    private Map<String, Object> createDepotUser(Map<String, Object> inputMap) {

        Map<String, Object> response = new HashMap<>();
        if(NullUtils.isNull(inputMap.get("XI30_LOC"))) throw new DataTransformationService.TransformationException("Depot Code cannot be null");

        response.put("loginId",inputMap.get("XI30_LOC").toString());
        response.put("activeStatus", "active");
        response.put("mobile", "0000000000");
        response.put("userAccountId", inputMap.get("XI30_LOC").toString());
        response.put("name", inputMap.get("XI30_LOCNAM").toString());
        response.put("designation", "depot");
        response.put("locationHierarchy", getUserLocation());
        response.put("channel", "all");
        response.put("outletClass", "all");
        response.put("distributionChannel", "all");
        response.put("latitude", "0");
        response.put("longitude", "0");
        response.put("email", "default");
        response.put("priceListId", null);
        response.put("immediateParent", "admin@applicate.in");
        response.put("address", "default");
        response.put("prodauthcode", null);

        Map<String, Object> extendedAttributes = new HashMap<>();
        extendedAttributes.put("CRNumber", inputMap.get("XI30_NATCPYNUM"));

        response.put("extendedAttributes", JSONUtils.toJsonNode(extendedAttributes));
        return response;

    }

    private Map<String, Object> getUserLocation(){

        Map<String, Object> location = new HashMap<>();
        location.put("country", "KSA");
        return location;

    }

}
