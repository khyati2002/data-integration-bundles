package com.applicate.cokesa.transformer;


import com.salescode.dim.etl.transformation.service.DataTransformationService.TransformationException;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDetailsTransformerCokeSa extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private static final String REGEX = "\\.0$";
    private static final String AM01_ARTGRP03 = "AM01_ARTGRP03";

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createResponse(inputMap));
        return responseList;
    }

    private Map<String, Object> createResponse(Map <String, Object> inputMap){
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("skuCode", getMandatory(inputMap, "AM01_ARTNUM").replaceAll(REGEX, ""));
            response.put("batchCode", getMandatory(inputMap, "AM01_ARTNUM").replaceAll(REGEX, ""));
            response.put("activeStatus", checkActive(inputMap.get("AM01_SPRCOD")));
            response.put("itemClass", getMandatory(inputMap, "AM01_ARTGRP01") + "-AM01_ARTGRP01");
            response.put("skuDescription", getMandatory(inputMap, "AM01_ARTNAM"));
            response.put("skuName", getMandatory(inputMap, "AM01_ARTNAM"));
            response.put("caseToPieceQuantity", getMandatory(inputMap, "AM01_NUMSUU"));
            response.put("itemType", getMandatory(inputMap, "AM01_ARTTYP"));
            response.put("brand", getMandatory(inputMap, "AM01_ARTGRP02") + "-AM01_ARTGRP02");
            response.put("flavour", getMandatory(inputMap, "AM01_ARTGRP06") + "-AM01_ARTGRP06");
            response.put("category", getMandatory(inputMap, AM01_ARTGRP03) + "-"+AM01_ARTGRP03);
            response.put("pieceSizeDesc", getMandatory(inputMap, AM01_ARTGRP03) + "-"+AM01_ARTGRP03);
            response.put("subCategory", getMandatory(inputMap, AM01_ARTGRP03) + "-"+AM01_ARTGRP03);
            response.put("size", getMandatory(inputMap, "AM01_ARTGRP05") + "-AM01_ARTGRP05");
            response.put("pieceSize", getMandatory(inputMap, "AM01_ARTGRP05") + "-AM01_ARTGRP05");
            response.put("skuCaseWeight",inputMap.get("AM01_ARTWGT"));
            response.put("extendedAttributes", createExtended(inputMap));
        }catch (NullPointerException e){
            throw new TransformationException("Some fields were found null while transforming the ProductDetails");
        }
        return response;
    }

    private JsonNode createExtended(Map<String, Object> inputMap){
        Map<String, Object> extended = new HashMap<>();
        extended.put("effectiveDate", inputMap.get("AM01_EFTDAT").toString().replaceAll(REGEX, ""));
        extended.put("effectiveFrom", inputMap.get("AM01_EFRDAT").toString().replaceAll(REGEX, ""));
        extended.put("suppressDate", inputMap.get("AM01_SPRDAT").toString().replaceAll(REGEX, ""));
        extended.put("broCodeCase",inputMap.get("AM01_NATARTUNI"));
        extended.put("broCodePiece",inputMap.get("AM01_NATARTSUU"));
        extended.put("taxCodeA",inputMap.get("AM01_ARTTAXCOD1"));
        extended.put("taxCodeB",inputMap.get("AM01_ARTTAXCOD2"));
        extended.put("taxCodeC",inputMap.get("AM01_ARTTAXCOD3"));
        extended.put("taxCodeD",inputMap.get("AM01_ARTTAXCOD4"));
        extended.put("taxCodeE",inputMap.get("AM01_ARTTAXCOD5"));
        Object fieldValue = inputMap.get("AM01_ARTNAM2");
        String dsValue = null;

        if (fieldValue != null && !fieldValue.toString().isEmpty()) {
            dsValue = String.valueOf(setArabicDescription(fieldValue.toString()));
        }

        extended.put("ds", dsValue);
        return JSONUtils.toJsonNode(extended);
    }

    private String checkActive(Object inputStatus){
        if(NullUtils.isNull(inputStatus)) return "active";
        return "S".equalsIgnoreCase(inputStatus.toString()) ? "inactive" : "active";
    }

    private JsonNode setArabicDescription(String arabicDesc){
        Map<String, String> setLang = new HashMap<>();
        setLang.put("ar", arabicDesc);
        return JSONUtils.toJsonNode(setLang);
    }

    private String getMandatory(Map<String, Object> inputMap, String key) {
        if (!inputMap.containsKey(key) || inputMap.get(key) == null || inputMap.get(key).toString().trim().isEmpty()) {
            throw new NullPointerException("Missing mandatory field: " + key);
        }
        return inputMap.get(key).toString();
    }

}
