package com.applicate.simamy.transformer;


import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.OutletDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimaCustomerGrouping extends AbstractTransformer<Map<String,Object>,Map<String,Object>> {

    private static final String CUSTOMER_GROUP = "CustomerGroup";
    private static final String CUSTOMER_GROUP_NAME = "CustomerGroupName";
    private static final String OUTLET_SELECTION = "OutletSelection";
    private static final String SELECTION_CRITERIA = "SelectionCriteria";

    private final Map<String, String> mapOfKeysAndFields = Map.of(
            "ATTRIB_7", "sub_channel",
            "TRADENAME", "extended_attributes->>'$.tradename'",
            "CLASSIFIC", "outlet_class",
            "MEP_CUST_NO", "extended_attributes->>'$.distributorCode'",
            "SALES_GROUP", "extended_attributes->>'$.sales_group'",
            "SALES_ROUTE", "channel");


    private final Map<String, String> mapOfCodeAndValues = Map.of(
            "sub_channel","14",
            "extended_attributes->>'$.tradename'", "3",
            "outlet_class", "11");

    private final Map<String, String> mapOfOperators = Map.of(
            "EQ", "in",
            "NE", "not in");
    GenericEntityService genericEntityRepository = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);
    OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);

    @Override
    public Map<String,Object> transform(Map<String, Object> stringObjectMap) {
        HashMap<String, Object> finalTransformedObj = new HashMap<>();
        HashMap<String, Map<String, List<String>>> objectForQuery = new HashMap<>();
        Map<String, String> outletCodeObject = new HashMap<>();
        List<String> listOfAndOperators = new ArrayList<>();
        List<String> listOfOrOperators = new ArrayList<>();
        Object customerGroup = stringObjectMap.get(CUSTOMER_GROUP);
        JsonNode customerGroupObject = JSONUtils.getObjectMapper().convertValue(customerGroup, JsonNode.class).get(0);
        String groupId = customerGroupObject.get(CUSTOMER_GROUP_NAME).asText();

        if(isValidCondition(customerGroupObject)){
            createOutletCodeObjectWithSelectionCriteria(customerGroupObject,outletCodeObject,listOfAndOperators,listOfOrOperators,objectForQuery);
            createOutletCodeObjectWithOutletSelection(customerGroupObject,outletCodeObject);
        }
        else if(customerGroupObject.has(SELECTION_CRITERIA) && !customerGroupObject.get(SELECTION_CRITERIA).isNull()){
            createOutletCodeObjectWithSelectionCriteria(customerGroupObject,outletCodeObject,listOfAndOperators,listOfOrOperators,objectForQuery);
        }else if(customerGroupObject.has(OUTLET_SELECTION) && !customerGroupObject.get(OUTLET_SELECTION).isNull()){
            createOutletCodeObjectWithOutletSelection(customerGroupObject,outletCodeObject);
        }else{
            throw new DataTransformationService.TransformationException("OutletSelection and SelectionCriteria keys not found!");
        }

        Map<String, Map<String, String>> finalObjectForPayload = new HashMap<>();
        finalObjectForPayload.put("values", outletCodeObject);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode outletCodeJsonNode = mapper.convertValue(finalObjectForPayload, JsonNode.class);
        finalTransformedObj.put("name", CUSTOMER_GROUP);
        finalTransformedObj.put("key1", groupId);
        finalTransformedObj.put("id", groupId);
        finalTransformedObj.put("payload", outletCodeJsonNode);
        return finalTransformedObj;
    }

    private String createWhereCondition(String currentValue, HashMap<String, Map<String, List<String>>> objectForQuery){
        Map<String, List<String>> operatorAndValuesInField = objectForQuery.get(currentValue);
        List<String> collect = operatorAndValuesInField.keySet().stream().map(operator -> {
            List<String> values = operatorAndValuesInField.get(operator);
            StringBuilder commaSeparatedValues = new StringBuilder("(");
            String valueString = values.stream().map(this::encloseQuotes).collect(Collectors.joining(","));
            commaSeparatedValues.append(valueString);
            commaSeparatedValues.append(")");
            return currentValue + " " + mapOfOperators.get(operator) + " " + commaSeparatedValues;
        }).collect(Collectors.toList());
        return collect.get(0);
    }

    private void updateObjectForQuery(HashMap<String, Map<String, List<String>>> objectForQuery,Map<String, List<String>> objectOfFilters, String key ){
        if(objectForQuery.containsKey(key)){
            Map<String, List<String>> stringListMap = objectForQuery.get(key);
            stringListMap.keySet().forEach( operator -> {
                List<String> comparisonValuesPresentInMap = stringListMap.get(operator);
                List<String> newValues = objectOfFilters.get(operator);
                comparisonValuesPresentInMap.addAll(newValues);
            });
        }
        else{
            objectForQuery.put(key, objectOfFilters);
        }
    }

    private Map<String, List<String>> createObjectOfFilters(JsonNode selectionObject, String key2){
        String comparisonOperator = selectionObject.get("ComparisonOperator").asText();
        List<String> comparisonValues = new ArrayList<>();
        int index = 1;
        while (true) {
            String comparisonValueKey = "ComparisonValue" + index;
            if (!selectionObject.has(comparisonValueKey)) {
                break;
            }
            String codeValue = selectionObject.get(comparisonValueKey).asText();
            if(mapOfCodeAndValues.containsKey(key2)) {
                List<GenericEntity> codeAndValueMapping = genericEntityRepository.findByNameAndKeys("Outletcategory", mapOfCodeAndValues.get(key2), codeValue);
                if(codeAndValueMapping.isEmpty()){
                    throw new DataTransformationService.TransformationException(String.format("%s code not found in outlet mapping for key %s", codeValue, mapOfCodeAndValues.get(key2)));
                }
                String valueToSearchInField = codeAndValueMapping.get(0).getKey3();
                comparisonValues.add(valueToSearchInField);
            }else{
                comparisonValues.add(codeValue);
            }
            index++;
        }
        return Map.of(comparisonOperator, comparisonValues);
    }

    private void createOutletCodeObjectWithOutletSelection(JsonNode customerGroupObject,Map<String, String> outletCodeObject){
        ArrayNode outletSelection = (ArrayNode) customerGroupObject.get(OUTLET_SELECTION);
        for(int i = 0; i < outletSelection.size(); i++){
            JsonNode outletObject = outletSelection.get(i);
            String outletCode = outletObject.get("OutletNumber").asText();
            String included = outletObject.get("Included").asText();
            if("true".equalsIgnoreCase(included)) {
                outletCodeObject.put(outletCode, outletCode);
            }
            if("false".equalsIgnoreCase(included) && outletCodeObject.containsKey(outletCode)){
                outletCodeObject.remove(outletCode);
            }
        }
    }

    private void createOutletCodeObjectWithSelectionCriteria(JsonNode customerGroupObject, Map<String, String> outletCodeObject, List<String> listOfAndOperators, List<String> listOfOrOperators, HashMap<String, Map<String, List<String>>> objectForQuery){
        ArrayNode selectionCriteria = (ArrayNode) customerGroupObject.get(SELECTION_CRITERIA);
        for(int i = 0; i < selectionCriteria.size(); i++){
            JsonNode selectionObject = selectionCriteria.get(i);
            String fieldNumber = selectionObject.get("FieldNumber").asText();
            List<GenericEntity> customerGroupMapping = genericEntityRepository.findByNameAndKeys("customerGroupMapping", fieldNumber);
            String key2 = customerGroupMapping.get(0).getKey2();
            Map<String, List<String>> objectOfFilters = createObjectOfFilters(selectionObject, mapOfKeysAndFields.get(key2));
            String logicalOperator = selectionObject.get("LogicalOperator").asText();
            if("FIR".equals(logicalOperator) || "AND".equals(logicalOperator)){
                listOfAndOperators.add(mapOfKeysAndFields.get(key2));
            }
            if("OR".equals(logicalOperator)){
                listOfOrOperators.add(mapOfKeysAndFields.get(key2));
            }
            updateObjectForQuery(objectForQuery, objectOfFilters, mapOfKeysAndFields.get(key2));
        }

        String queryAndOperator = listOfAndOperators.stream()
                .map(currentAndValue -> createWhereCondition(currentAndValue, objectForQuery))
                .collect(Collectors.joining(" and "));

        String queryOrOperator = listOfOrOperators.stream()
                .map(currentAndValue -> createWhereCondition(currentAndValue, objectForQuery))
                .collect(Collectors.joining(" or "));

        String whereClause = queryAndOperator + (queryOrOperator.isEmpty() ? "" : " or " + queryOrOperator);

        // Use the new service method
        List<String> outletCodes = outletDetailsService.findOutletCodesByCondition(whereClause);

        for(String outletCode : outletCodes){
            outletCodeObject.put(outletCode, outletCode);
        }
    }
    private boolean isValidCondition(JsonNode customerGroupObject){
        return (customerGroupObject.has(OUTLET_SELECTION) && customerGroupObject.has(SELECTION_CRITERIA) && !customerGroupObject.get(OUTLET_SELECTION).isNull() && !customerGroupObject.get(SELECTION_CRITERIA).isNull() && customerGroupObject.get(SELECTION_CRITERIA).size() > 0 && customerGroupObject.get(OUTLET_SELECTION).size() > 0);
    }

    /**
     * Enclose the passed value inside quotations.
     * If the passed value contains single quotation mark, enclose it inside double quotes
     * otherwise, enclose it in single quotation marks
     * @param value string value to enclose inside quotation
     * @return string value enclosed inside quotation
     */
    private String encloseQuotes(String value) {
        return value.contains("'") ? "\"" + value + "\""  :  "'" + value + "'";
    }

}
