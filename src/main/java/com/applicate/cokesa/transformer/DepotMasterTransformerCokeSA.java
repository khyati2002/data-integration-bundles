package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepotMasterTransformerCokeSA extends AbstractTransformer<Map<String,Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {

        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createDepotOutlet(inputMap));
        return responseList;

    }

    private Map<String, Object> createDepotOutlet(Map<String, Object> inputMap) {

        Map<String, Object> response = new HashMap<>();
        if(NullUtils.isNull(inputMap.get("XI30_LOC"))) throw new DataTransformationService.TransformationException("Depot Code cannot be null");

        response.put("outletcode",inputMap.get("XI30_LOC").toString());
        response.put("loginid", inputMap.get("XI30_LOC").toString());
        response.put("activeStatus", ActiveStatus.ACTIVE);
        response.put("contactno", "00000");
        response.put("outletName", inputMap.get("XI30_LOCNAM").toString());
        response.put("locationHierarchy", getUserLocation());
        response.put("channel", "ALL");
        response.put("distributionChannel", "ALL");
        response.put("outletClass", "ALL");
        response.put("latitude", BigDecimal.ZERO);
        response.put("longitude", BigDecimal.ZERO);
        response.put("email", "default");
        response.put("priceListId", null);
        response.put("address", "default");
        response.put("prodauthcode", null);

        Map<String, Object> extendedAttributes = new HashMap<>();
        extendedAttributes.put("CRNumber", inputMap.get("XI30_NATCPYNUM"));
        extendedAttributes.put("designation", "Depot");

        response.put("extendedAttributes", extendedAttributes);
        return response;

    }

    private String getUserLocation(){

        StringBuilder location = new StringBuilder();
        return location.length() > 0 ? location.toString() : "KSA";

    }

}
