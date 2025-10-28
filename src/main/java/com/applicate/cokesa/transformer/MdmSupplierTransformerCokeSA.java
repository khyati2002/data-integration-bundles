package com.applicate.cokesa.transformer;


import com.salescode.dim.etl.transformation.service.DataTransformationService.TransformationException;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MdmSupplierTransformerCokeSA extends AbstractTransformer<Map<String,Object>,List<Map<String, Object>>> {
    private static final String DESIGNATION = "designation";
    private static final String MOBILE = "mobile";
    private static final String SUPPLIER = "supplier";
    private static final String ASM = "asm";
    private static final String SUPERVISOR = "supervisor";

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.addAll(createSupervisor(inputMap));
        return responseList;
    }

    private List<Map<String, Object>> createSupervisor(Map<String, Object> inputMap){
        List<Map<String, Object>> listUsers = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();
        String loginId = inputMap.get("loginid").toString();
        response.put("loginId", loginId);
        response.put("activeStatus",inputMap.getOrDefault("activestatus", "active").toString());
        response.put(MOBILE,inputMap.getOrDefault(MOBILE, "0000000000").toString());
        response.put("userAccountId", loginId);
        response.put("name", inputMap.get("name").toString());
        String finalDesignation = getUserDesignation(formatString(inputMap.get(DESIGNATION).toString()));
        response.put(DESIGNATION, finalDesignation);
        response.put("locationHierarchy",getUserLocation());
        response.put("immediateParent", StringUtils.isEqual(finalDesignation, ASM, true) ? "admin@applicate.in" : inputMap.get("parentid-1"));
        listUsers.add(response);
        if(StringUtils.isNotEmpty(finalDesignation) && StringUtils.isEqual(finalDesignation, SUPERVISOR, true) && NullUtils.isNotNull(inputMap.get("salesroute").toString())) listUsers.add(createSupplierDepot(inputMap.get("salesroute").toString(), loginId));
        return listUsers;
    }

    private Map<String, Object> createSupplierDepot(String salesRoute, String parentSupervisorId){
        Map<String, Object> response = new HashMap<>();
        String loginIdSupplier = salesRoute.substring(0,2);
        response.put("loginId", loginIdSupplier);
        response.put("activeStatus","active");
        response.put(MOBILE,"0000000000");
        response.put("userAccountId", loginIdSupplier);
        response.put("name", "DEPOTNAME_" + loginIdSupplier);
        response.put(DESIGNATION, SUPPLIER);
        response.put("locationHierarchy", getUserLocation());
        response.put("immediateParent", parentSupervisorId);
        return response;

    }
    public static String formatString(String designation) {
        if (NullUtils.isNull(designation)) {
            return null;
        }
        return designation.toLowerCase().replaceAll("\\s+", "");
    }

    private String getUserDesignation(String userTypeVal) {
        if (NullUtils.isNull(userTypeVal)) {
            throw new TransformationException("Designation value must not be null");
        }
        switch (userTypeVal) {
            case "salessupervisor":
            case "keyaccountsupervisor":
            case "fountainsupervisor":
                return SUPERVISOR;

            case "clustersalesmanager":
            case "areasalesmanager":
            case "fieldsalesmanager":
            case "keyaccountmanager":
            case "zonalsalesmanager":
                return ASM;

            case SUPPLIER:
                return SUPPLIER;

            default:
                throw new TransformationException("Unrecognized designation type: " + userTypeVal);
        }
    }
    private Map<String, Object> getUserLocation(){
        Map<String, Object> location = new HashMap<>();
        location.put("country", "KSA");
        return location;
    }

}
