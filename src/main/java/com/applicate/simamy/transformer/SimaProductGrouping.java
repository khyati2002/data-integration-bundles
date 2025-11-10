package com.applicate.simamy.transformer;


import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SimaProductGrouping extends AbstractTransformer<Map<String,Object>,Map<String,Object>> {

    private static final String PRODUCT_GROUP = "ProductGroup";
    private static final String ARTICLE_GROUPING_LIST_NAME = "ArticleGroupingListName";
    private static final String ARTICLE_SELECTION = "ArticleSelection";
    private static final String SELECTION_CRITERIA = "SelectionCriteria";

    private final Map<String, String> mapOfKeysAndFields = Map.of(
            "BRAND", "brand",
            "MATL_TYPE", "extended_attributes->>'$.MATL_TYPE'",
            "PACK_TYPE", "piece_size_desc",
            "PACK_SIZE", "piece_size",
            "BEVERAGE_TYPE", "item_type",
            "MATL_GRP_2", "extended_attributes->>'$.MATL_GRP_2'",
            "SALES_UNIT", "extended_attributes->>'$.SALES_UNIT'",
            "PACKAGE", "extended_attributes->>'$.PACKAGE'",
            "TRADE_MARK", "extended_attributes->>'$.TRADE_MARK'");


    private final Map<String, String> mapOfCodeAndValues = Map.of(
            "brand","1",
            "piece_size_desc", "11",
            "piece_size", "3",
            "item_type", "4",
            "extended_attributes->>'$.SALES_UNIT'","2",
            "extended_attributes->>'$.PACKAGE", "7",
            "extended_attributes->>'$.TRADE_MARK'","8",
            "extended_attributes->>'$.MATL_GRP_2'","12");

    private final Map<String, String> mapOfOperators = Map.of(
            "EQ", "in",
            "NE", "not in");

    private final Map<String, Double> unitConversionMap = Map.of(
            "ML", 1.0,
            "ml", 1.0,
            "l", 1000.0,
            "L", 1000.0,
            "LTR", 1000.0,
            "KG", 1000.0,
            "kg", 1000.0,
            "GM", 1.0,
            "gm", 1.0);

    GenericEntityRepository genericEntityRepository = SpringContext.getBean(GenericEntityRepository.class);

    @Override
    public Object transform(Map<String, Object> stringObjectMap) {
        HashMap<String, Object> finalTransformedObj = new HashMap<>();
        HashMap<String, Map<String, List<String>>> objectForQuery = new HashMap<>();
        Map<String, String> skuCodeObject = new HashMap<>();
        List<String> listOfAndOperators = new ArrayList<>();
        List<String> listOfOrOperators = new ArrayList<>();
        Object productGroup = stringObjectMap.get(PRODUCT_GROUP);
        JsonNode productGroupObject = JSONUtils.toJsonNode(productGroup).get(0);
        String groupId = productGroupObject.get(ARTICLE_GROUPING_LIST_NAME).asText();

        if(isValidCondition(productGroupObject)){
            createSkuCodeObjectWithSelectionCriteria(productGroupObject,skuCodeObject,listOfAndOperators,listOfOrOperators,objectForQuery);
            createSkuCodeObjectWithArticleSelection(productGroupObject,skuCodeObject);
        }
        else if(productGroupObject.has(SELECTION_CRITERIA) && !productGroupObject.get(SELECTION_CRITERIA).isNull()){
            createSkuCodeObjectWithSelectionCriteria(productGroupObject,skuCodeObject,listOfAndOperators,listOfOrOperators,objectForQuery);
        }else if(productGroupObject.has(ARTICLE_SELECTION) && !productGroupObject.get(ARTICLE_SELECTION).isNull()){
            createSkuCodeObjectWithArticleSelection(productGroupObject,skuCodeObject);
        }else{
            throw new TransformationException("ArticleSelection and SelectionCriteria keys not found!");
        }

        Map<String, Map<String, String>> finalObjectForPayload = new HashMap<>();
        finalObjectForPayload.put("values", skuCodeObject);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode skuCodeJsonNode = mapper.convertValue(finalObjectForPayload, JsonNode.class);
        finalTransformedObj.put("name", PRODUCT_GROUP);
        finalTransformedObj.put("key1", groupId);
        finalTransformedObj.put("id", groupId);
        finalTransformedObj.put("payload", skuCodeJsonNode);
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
                List<GenericEntity> codeAndValueMapping = genericEntityRepository.findByNameAndKey1AndKey2("productMapping", mapOfCodeAndValues.get(key2), codeValue);
                if(codeAndValueMapping.isEmpty()){
                    throw new TransformationException("{} code not found in product mapping for key {}", codeValue, mapOfCodeAndValues.get(key2));
                }
                createComparisionValues(mapOfCodeAndValues,key2,codeAndValueMapping,comparisonValues);


            }else{
                comparisonValues.add(codeValue);
            }
            index++;
        } 
        return Map.of(comparisonOperator, comparisonValues);
    }

    private void createComparisionValues( Map<String, String> mapOfCodeAndValues,String key2,List<GenericEntity> codeAndValueMapping,List<String> comparisonValues){
        String valueToSearchInField = "";
        if ("3".equalsIgnoreCase(mapOfCodeAndValues.get(key2))){
            Pattern pattern = Pattern.compile("-?(\\d+(\\.\\d+))|(\\.\\d)|(\\d+)?");
            String piecesize = codeAndValueMapping.get(0).getKey3();
            String[] parts = piecesize.split(" ");
            if(parts.length ==2 && pattern.matcher(parts[0]).matches()) {
                double numericValue = Double.parseDouble(parts[0]);
                String unit = parts[1];
                String value = String.valueOf(unitConversionMap.get(unit)* numericValue);
                valueToSearchInField =value.split("\\.")[0];
                comparisonValues.add(valueToSearchInField);
            }
        }
        else{
            valueToSearchInField = codeAndValueMapping.get(0).getKey3();
            comparisonValues.add(valueToSearchInField);
        }
    }
    private void createSkuCodeObjectWithArticleSelection(JsonNode productGroupObject,Map<String, String> skuCodeObject){
        ArrayNode articleSelection = (ArrayNode) productGroupObject.get(ARTICLE_SELECTION);
        for(int i = 0; i < articleSelection.size(); i++){
            JsonNode articleObject = articleSelection.get(i);
            String skuCode = articleObject.get("ArticleNumber").asText();
            String included = articleObject.get("Included").asText();
            if("true".equalsIgnoreCase(included)) {
                skuCodeObject.put(skuCode, skuCode);
            }
            if("false".equalsIgnoreCase(included) && skuCodeObject.containsKey(skuCode)){
                skuCodeObject.remove(skuCode);
            }
        }
    }

    private void createSkuCodeObjectWithSelectionCriteria(JsonNode productGroupObject,Map<String, String> skuCodeObject,List<String> listOfAndOperators,List<String> listOfOrOperators,HashMap<String, Map<String, List<String>>> objectForQuery){
        ArrayNode selectionCriteria = (ArrayNode) productGroupObject.get(SELECTION_CRITERIA);
        for(int i = 0; i < selectionCriteria.size(); i++){
            JsonNode selectionObject = selectionCriteria.get(i);
            String fieldNumber = selectionObject.get("FieldNumber").asText();
            List<GenericEntity> productGroupMapping = genericEntityRepository.findByNameAndKey1("productGroupMapping", fieldNumber);
            String key2 = productGroupMapping.get(0).getKey2();
            Map<String, List<String>> objectOfFilters = createObjectOfFilters(selectionObject,mapOfKeysAndFields.get(key2));
            String logicalOperator = selectionObject.get("LogicalOperator").asText();
            if("FIR".equals(logicalOperator) || "AND".equals(logicalOperator)){
                listOfAndOperators.add(mapOfKeysAndFields.get(key2));
            }
            if("OR".equals(logicalOperator)){
                listOfOrOperators.add(mapOfKeysAndFields.get(key2));
            }
            updateObjectForQuery(objectForQuery,objectOfFilters, mapOfKeysAndFields.get(key2) );
        }


        String queryAndOperator = listOfAndOperators.stream().map(currentAndValue -> createWhereCondition(currentAndValue, objectForQuery)).collect(Collectors.joining(" and "));

        String queryOrOperator = listOfOrOperators.stream().map(currentAndValue -> createWhereCondition(currentAndValue, objectForQuery)).collect(Collectors.joining(" or "));

        String selectStatement = "select sku_code from ck_productdetails where ";
        String finalQuery = selectStatement + queryAndOperator + (queryOrOperator.isEmpty()? " " : " or ") + queryOrOperator + ";";
        List<Map<String, String>> skuCodes = (List<Map<String, String>>) EntityUtils.get().findDataByQuery(Map.class, finalQuery, true);
        for(int i = 0; i< skuCodes.size(); i++){
            String skuCode = skuCodes.get(i).get("sku_code");
            skuCodeObject.put(skuCode, skuCode);
        }
    }

    private boolean isValidCondition(JsonNode productGroupObject){
        return (productGroupObject.has(ARTICLE_SELECTION) && productGroupObject.has(SELECTION_CRITERIA) && !productGroupObject.get(ARTICLE_SELECTION).isNull() && !productGroupObject.get(SELECTION_CRITERIA).isNull() && productGroupObject.get(SELECTION_CRITERIA).size() > 0 && productGroupObject.get(ARTICLE_SELECTION).size() > 0);
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
