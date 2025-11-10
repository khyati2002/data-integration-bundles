package com.applicate.simamy.transformer;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.util.ObjectUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ILCCouponSIMATransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final String CRITERIA = "criteria";
    private static final String SCHEME_ID=  "schemeId";
    private static final String DISCOUNT_ID=  "discountid";
    private static final String MINIMUM_AMOUNT = "minimumAmount";
    private static final String PROGRAM_LEVEL = "programlevel";
    private static final String ITEM_EACH = "item_each";
    private static final String VALUE_EACH = "value_each";
    private static final String SCHEME_TYPE = "schemeType";
    private static final String SCHEME_DESCRIPTION = "schemeDescription";
    private static final String BRAND = "brand";
    private static final String CATEGORY = "category";
    private static final String ITEM_CLASS = "itemClass";
    private static final String OUTLET_CODE = "outletCode";
    private static final String CHANNEL = "channel";



    @Override
    public Object transform(Map<String, Object> inputMap) {
        Map<String, Object> schemeData = schemeDefinition(inputMap);
        schemeData.put("schemeCalculation",calculationTransformer(inputMap));
        schemeData.put("schemeProductBifurcationsList", schemeProductTransformer(inputMap));
        schemeData.put("schemeOutletBifurcationsList", schemeOutletTransformer(inputMap));
        return schemeData;
    }

    private Map<String, Object> schemeOutletTransformer(Map<String, Object> inputMap) {
        Map<String, Object> schemeOutletMap = new HashMap<>();
        schemeOutletMap.put(SCHEME_ID, inputMap.get(DISCOUNT_ID));

        schemeOutletMap.put(OUTLET_CODE, ObjectUtils.isEmpty(inputMap.get(OUTLET_CODE))|| NullUtils.isNull(inputMap.get(OUTLET_CODE)) ? "all" : inputMap.get(OUTLET_CODE));
        schemeOutletMap.put("outletClass", ObjectUtils.isEmpty(inputMap.get("outletclass"))|| NullUtils.isNull(inputMap.get("outletclass")) ? "all" : inputMap.get("outletclass"));
        schemeOutletMap.put("loginId", ObjectUtils.isEmpty(inputMap.get("supplierId"))|| NullUtils.isNull(inputMap.get("supplierId")) ? "all" : inputMap.get("supplierId"));
        schemeOutletMap.put("outletCategory",ObjectUtils.isEmpty(inputMap.get("outletcategory"))|| NullUtils.isNull(inputMap.get("outletcategory")) ? "all" : inputMap.get("outletcategory"));
        schemeOutletMap.put("outletType", ObjectUtils.isEmpty(inputMap.get("outlettype"))|| NullUtils.isNull(inputMap.get("outlettype")) ? "all" : inputMap.get("outlettype"));
        schemeOutletMap.put(CHANNEL, ObjectUtils.isEmpty(inputMap.get(CHANNEL))|| NullUtils.isNull(inputMap.get(CHANNEL)) ? "all" :inputMap.get(CHANNEL));
        return schemeOutletMap;
    }

    private Map<String, Object> schemeProductTransformer(Map<String, Object> inputMap) {
        Map<String, Object> schemeProductMap = new HashMap<>();
        schemeProductMap.put(SCHEME_ID, inputMap.get(DISCOUNT_ID));
        schemeProductMap.put(BRAND, ObjectUtils.isEmpty(inputMap.get(BRAND)) ? "all": inputMap.get(BRAND));
        schemeProductMap.put("batchCode",ObjectUtils.isEmpty(inputMap.get("batchcode")) ? "all" : inputMap.get("batchcode"));
        schemeProductMap.put(CATEGORY, ObjectUtils.isEmpty(inputMap.get(CATEGORY)) ? "all" : inputMap.get(CATEGORY));
        schemeProductMap.put("subCategory", ObjectUtils.isEmpty(inputMap.get("subcategory")) ? "all" : inputMap.get("subcategory"));
        schemeProductMap.put(ITEM_CLASS, ObjectUtils.isEmpty(inputMap.get(ITEM_CLASS)) ? "all" : inputMap.get(ITEM_CLASS));
        return schemeProductMap;
    }

    private Map<String, Object> calculationTransformer(Map<String, Object> inputMap) {
        Map<String , Object> schemeCalculationMap = new HashMap<>();
        schemeCalculationMap.put(SCHEME_ID, inputMap.get(DISCOUNT_ID));
        schemeCalculationMap.put(CRITERIA, inputMap.get(CRITERIA));
        schemeCalculationMap.put(SCHEME_TYPE, inputMap.get("type").toString().toUpperCase());
        schemeCalculationMap.put("itemEach", getValue(schemeCalculationMap.get(SCHEME_TYPE)));
        ArrayNode slabArray = getSlabIfAlreadyExist(inputMap);
        schemeCalculationMap.put("slabInfo", slabArray);
        if(ObjectUtils.isEmpty(inputMap.get("min_max_cond"))) schemeCalculationMap.put("rangeLevelUnit", "amount");
        else schemeCalculationMap.put("rangeLevelUnit", inputMap.get("min_max_cond"));
        schemeCalculationMap.put("usageLimit", inputMap.get("usageLimit"));
        schemeCalculationMap.put("maxDiscount", inputMap.get("maxDiscount"));
        schemeCalculationMap.put("maxTerm", inputMap.get("maxTerm"));
        schemeCalculationMap.put(MINIMUM_AMOUNT, inputMap.get(MINIMUM_AMOUNT));
        return schemeCalculationMap;
    }

    int getValue(Object schemeType){
        if(ObjectUtils.isEmpty(schemeType)){
            return 0;
        }
        String type = schemeType.toString();
        if(type.equalsIgnoreCase(ITEM_EACH) || type.equalsIgnoreCase(VALUE_EACH)){
            return 1;
        }
        return 0;
    }

    private Map<String, Object> schemeDefinition(Map<String, Object> inputMap) {
        Map<String, Object> schemeDefinitionMap = new HashMap<>();
        schemeDefinitionMap.put(SCHEME_ID, inputMap.get(DISCOUNT_ID));
        schemeDefinitionMap.put("schemeName", inputMap.get("discountName").toString().toUpperCase());
        schemeDefinitionMap.put("programLevel", inputMap.get(PROGRAM_LEVEL).toString().toUpperCase());
        schemeDefinitionMap.put(SCHEME_DESCRIPTION, inputMap.get(SCHEME_DESCRIPTION));
        schemeDefinitionMap.put(SCHEME_TYPE, inputMap.get("type").toString().toUpperCase());
        String startDateInput = inputMap.get("startdate").toString();
        String endDateInput = inputMap.get("enddate").toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date startDate = sdf.parse(startDateInput);
            Date endDate = sdf.parse(endDateInput);
            schemeDefinitionMap.put("endDate", endDate);
            schemeDefinitionMap.put("startDate", startDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        schemeDefinitionMap.put("priority", inputMap.get("priority"));
        ObjectNode extendedAttributes = new ObjectMapper().createObjectNode();
        schemeDefinitionMap.put("extendedAttributes", extendedAttributes);

        return schemeDefinitionMap;
    }
    ArrayNode getSlabIfAlreadyExist(Map<String, Object> inputMap){
        String slabFrom =  inputMap.get(PROGRAM_LEVEL).toString().equalsIgnoreCase("Coupon") ? inputMap.get("startamount").toString() : inputMap.get("min").toString() ;
        String slabTo = inputMap.get(PROGRAM_LEVEL).toString().equalsIgnoreCase("Coupon") ? inputMap.get("endamount").toString() : inputMap.get("max").toString();
        String slabDiscount = inputMap.get("discountamount").toString();
        String minAmount = inputMap.get(MINIMUM_AMOUNT).toString();

        ObjectNode slabNode = new ObjectMapper().createObjectNode();
        slabNode.put("endRange", slabTo);
        slabNode.put("startRange", slabFrom);
        slabNode.put("schemeBenefit", slabDiscount);
        slabNode.put(SCHEME_DESCRIPTION, inputMap.get(SCHEME_DESCRIPTION).toString());
        slabNode.put(MINIMUM_AMOUNT, minAmount);

        ArrayNode slabArray = new ObjectMapper().createArrayNode();
        slabArray.add(slabNode);
        return slabArray;
    }
}
