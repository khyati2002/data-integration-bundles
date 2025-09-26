package com.applicate.hccb.transformer;

import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class HCCBTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final String CRITERIA = "criteria";
    private static final String PRIORITY = "priority";
    private static final String MONITORING_SCOPE = "monitoring_scope";
    private static final String SCHEME_ID = "schemeId";
    private static final String SCHEME_NO = "scheme_no";
    private static final String MARKET_SCOPE = "market_scope";
    private static final String OUTLET_CODE = "outletCode";
    private static final String CHANNEL = "channel";
    private static final String LOGIN_ID = "loginId";
    private static final String BATCH_CODE = "batchCode";
    private static final String ITEM_CLASS = "itemClass";
    private static final String ITEM_EACH = "item_each";
    private static final String VALUE_EACH = "value_each";
    private static final String SCHEME_TYPE = "schemeType";
    private static final String REGION = "region";
    private static final String DISTRICT = "district";
    private static final String TOWN = "town";
    private static final String STATE = "state";
    private static final String CITY = "city";
    private static final String EXTERNAL_ID = "external_id";
    private static final String DIST_SAP_CUSTOMER_ID = "dist_sap_customer_id";


    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> schemeData = schemeDefinition(inputMap);
        schemeData.put("schemeCalculation", calculationTransformer(inputMap));
        schemeData.put("schemeProductBifurcationsList", schemeProductTransformer(inputMap));
        schemeData.put("schemeOutletBifurcationsList", schemeOutletTransformer(inputMap));
        schemeData.put("schemeLocationBifurcationsList", schemeLocationTransformer(inputMap));
        return schemeData;
    }

    private Map<String, Object> schemeLocationTransformer(Map<String, Object> inputMap) {
        Map<String, Object> schemeLocationMap = new HashMap<>();
        schemeLocationMap.put(SCHEME_ID, inputMap.get(SCHEME_NO));
        schemeLocationMap.put(REGION, ObjectUtils.isEmpty(inputMap.get(REGION)) ? "all" : inputMap.get(REGION));
        schemeLocationMap.put(CITY, ObjectUtils.isEmpty(inputMap.get(CITY)) ? "all" : inputMap.get(CITY));
        schemeLocationMap.put(STATE, ObjectUtils.isEmpty(inputMap.get(STATE)) ? "all" : inputMap.get(STATE));
        schemeLocationMap.put(TOWN, ObjectUtils.isEmpty(inputMap.get(TOWN)) ? "all" : inputMap.get(TOWN));
        schemeLocationMap.put(DISTRICT, ObjectUtils.isEmpty(inputMap.get(DISTRICT)) ? "all" : inputMap.get(DISTRICT));


        return schemeLocationMap;
    }

    private Map<String, Object> schemeOutletTransformer(Map<String, Object> inputMap) {
        Map<String, Object> schemeOutletMap = new HashMap<>();
        schemeOutletMap.put(SCHEME_ID, inputMap.get(SCHEME_NO));
        int marketScope = Integer.parseInt(inputMap.get(MARKET_SCOPE).toString().trim());
        String marketScopeDesc = inputMap.get("market_scope_desc").toString().trim();
        schemeOutletMap.put(OUTLET_CODE, "all");
        schemeOutletMap.put(LOGIN_ID, "all");
        schemeOutletMap.put(CHANNEL, "all");
        schemeOutletMap.put("outletCategory", "all");
        schemeOutletMap.put("outletType", "all");
        schemeOutletMap.put("subChannel", "all");
        schemeOutletMap.put("distributionChannel", "all");
        schemeOutletMap.put("account", "all");
        schemeOutletMap.put("outletClass", "all");
        schemeOutletMap.put("marketId", "all");
        schemeOutletMap.put("marketName", "all");
        schemeOutletMap.put("subTerritory", "all");
        schemeOutletMap.put("soldTo", "all");
        schemeOutletMap.put("outletDivision", "all");
        schemeOutletMap.put("priceListId", "all");
        schemeOutletMap.put("beat", "all");


        if (NullUtils.isNotNull(inputMap.get(EXTERNAL_ID)) && !ObjectUtils.isEmpty(inputMap.get(EXTERNAL_ID).toString())) {
            String[] parts = inputMap.get(EXTERNAL_ID).toString().split("_");
            schemeOutletMap.put("distributionChannel", parts[parts.length - 1]);
        }
        if (marketScope == 1) {
            schemeOutletMap.put(OUTLET_CODE, marketScopeDesc);
        } else if (marketScope == 3) {
            schemeOutletMap.put(LOGIN_ID, marketScopeDesc);
        } else if (marketScope == 6) {
            schemeOutletMap.put(CHANNEL, marketScopeDesc);

            if (NullUtils.isNotNull(inputMap.get(DIST_SAP_CUSTOMER_ID)) && !ObjectUtils.isEmpty(inputMap.get(DIST_SAP_CUSTOMER_ID).toString())) {
                schemeOutletMap.put(LOGIN_ID, inputMap.get(DIST_SAP_CUSTOMER_ID));
            }
        }


        return schemeOutletMap;
    }


    private Map<String, Object> schemeProductTransformer(Map<String, Object> inputMap) {
        Map<String, Object> schemeProductMap = new HashMap<>();
        schemeProductMap.put(SCHEME_ID, inputMap.get(SCHEME_NO));
        int monitoringScope = Integer.parseInt(inputMap.get(MONITORING_SCOPE).toString().trim());
        String monitoringValue = inputMap.get("monitoring_value").toString().trim();
        schemeProductMap.put("pieceSize", "all");
        schemeProductMap.put("pieceSizeDesc", "all");
        schemeProductMap.put("subCategoryCode", "all");
        schemeProductMap.put("ctg", "1");
        schemeProductMap.put("brand", "all");
        schemeProductMap.put(ITEM_CLASS, "all");
        schemeProductMap.put("itemId", "all");
        schemeProductMap.put(BATCH_CODE, "all");
        schemeProductMap.put("category", "all");
        schemeProductMap.put("subCategory", "all");
        schemeProductMap.put("customGroupCode", "all");
        schemeProductMap.put("flavour", "all");
        schemeProductMap.put("marketSku", "all");
        schemeProductMap.put("purchaseUnit", "all");
        schemeProductMap.put("qualifier_", "1");
        schemeProductMap.put("size", "all");
        schemeProductMap.put("product", "all");
        schemeProductMap.put("itemType", "all");
        schemeProductMap.put("articleCode", "all");
        schemeProductMap.put("skuCode", "all");
        schemeProductMap.put("mcode", "all");
        if (monitoringScope == 1) {
            schemeProductMap.put(ITEM_CLASS, monitoringValue);
        } else if (monitoringScope == 2) {
            schemeProductMap.put(BATCH_CODE, monitoringValue);
        } else if (monitoringScope == 3)
            schemeProductMap.put("customGroupCode", monitoringValue);
        return schemeProductMap;
    }

    // batchCode, brand, category, itemclass, itemId, pieceSize, scheme_id, subCategory, custom_group_code, ctg, flavour, marketsku, piece_size_desc, purchase_unit, size, subCategorycode, qualifier, itemType, product, articlecode, skucode, m_code

    private Map<String, Object> calculationTransformer(Map<String, Object> inputMap) {
        Map<String, Object> schemeCalculationMap = new HashMap<>();
        schemeCalculationMap.put(SCHEME_ID, inputMap.get(SCHEME_NO));
        schemeCalculationMap.put(CRITERIA, getSchemeCriteria(inputMap));
        schemeCalculationMap.put(SCHEME_TYPE, getSchemeType(inputMap));
        schemeCalculationMap.put("itemEach", getValue(schemeCalculationMap.get(SCHEME_TYPE), inputMap.get(MONITORING_SCOPE).toString().trim()));
        ArrayNode slabArray = getSlabIfAlreadyExist(inputMap);
        schemeCalculationMap.put("slabInfo", slabArray);
        schemeCalculationMap.put("rangeLevelUnit", "nq");
        schemeCalculationMap.put("maxDiscount", "0");
        schemeCalculationMap.put("maxTerm", "0");
        schemeCalculationMap.put("minimumAmount", "0");
        schemeCalculationMap.put("usageLimit", "0");
        schemeCalculationMap.put("schemeDiscountedProductPrice", inputMap.get("discountedprice"));
        schemeCalculationMap.put("schemeDiscountedProductcode", inputMap.get("discounted_item_id"));
        schemeCalculationMap.put("schemeDiscountedProductcodeuom", inputMap.get("discounted_item_uom"));
        if (inputMap.get(MONITORING_SCOPE).toString().trim().equalsIgnoreCase("3"))
            schemeCalculationMap.put("mustBuyGroupId", inputMap.get(SCHEME_NO));
        ObjectNode extendedAttributes = new ObjectMapper().createObjectNode();
        if (inputMap.get(MONITORING_SCOPE).toString().trim().equalsIgnoreCase("3")) {
            extendedAttributes.put("mustBuyRepeatSlabSync", "true");
        }
        schemeCalculationMap.put("extendedAttributes", extendedAttributes);
        return schemeCalculationMap;
    }

    int getValue(Object schemeType, String monitoringScope) {
        if (ObjectUtils.isEmpty(schemeType)) {
            return 0;
        }
        String type = schemeType.toString();
        if (monitoringScope.equalsIgnoreCase("3") && (type.equalsIgnoreCase(ITEM_EACH) || type.equalsIgnoreCase(VALUE_EACH)))
            return 2;
        if (type.equalsIgnoreCase(ITEM_EACH) || type.equalsIgnoreCase(VALUE_EACH) || type.equalsIgnoreCase("flat")) {
            return 1;
        }
        return 0;
    }

    ArrayNode getSlabIfAlreadyExist(Map<String, Object> inputMap) {
        String slabFrom = inputMap.get("monitoring_slab_from").toString().trim();
        String slabTo = inputMap.get("monitoring_slab_to").toString().trim();
        String slabDiscount = inputMap.get("discounted_value").toString().trim();

        ObjectNode slabNode = new ObjectMapper().createObjectNode();
        slabNode.put("endRange", slabTo);
        slabNode.put("startRange", slabFrom);
        slabNode.put("schemeBenefit", slabDiscount);
        slabNode.put("schemeDescription", inputMap.get("scheme_desc").toString());
        ArrayNode slabArray = new ObjectMapper().createArrayNode();
        slabArray.add(slabNode);
        return slabArray;
    }

    private Map<String, Object> schemeDefinition(Map<String, Object> inputMap) {
        Map<String, Object> schemeDefinitionMap = new HashMap<>();
        if(inputMap.get(MONITORING_SCOPE)!=null && inputMap.get(MONITORING_SCOPE).toString().trim().equalsIgnoreCase("4")){
            throw new DataTransformationService.TransformationException("invalid MONITORING_SCOPE, value= 4 ");
        }
        schemeDefinitionMap.put(SCHEME_ID, inputMap.get(SCHEME_NO));
        schemeDefinitionMap.put(CRITERIA, getSchemeCriteria(inputMap));
        String startDateInput = inputMap.get("mer_wef").toString().trim();
        String endDateInput = inputMap.get("mer_wet").toString().trim();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        ZonedDateTime startdateZoned = LocalDateTime.parse(startDateInput, formatter).atZone(ZoneOffset.UTC);
        ZonedDateTime enddateZoned = LocalDateTime.parse(endDateInput, formatter).atZone(ZoneOffset.UTC);
        LocalDateTime startdateutc = startdateZoned.toLocalDateTime();
        LocalDateTime enddateutc = enddateZoned.toLocalDateTime();

        schemeDefinitionMap.put("startDate", startdateutc);
        schemeDefinitionMap.put("endDate", enddateutc);

        schemeDefinitionMap.put("schemeDescription", inputMap.get("scheme_desc"));
        schemeDefinitionMap.put(PRIORITY, getPriority(inputMap.get(MONITORING_SCOPE).toString().trim(), inputMap.get(MARKET_SCOPE).toString().trim()));
        if (inputMap.get(MONITORING_SCOPE).toString().trim().equalsIgnoreCase("3"))
            schemeDefinitionMap.put("programLevel", "bundle");
        schemeDefinitionMap.put(SCHEME_TYPE, getSchemeType(inputMap));
        ObjectNode extendedAttributes = new ObjectMapper().createObjectNode();
        extendedAttributes.put("marketScope", inputMap.get(MARKET_SCOPE).toString().trim());
        extendedAttributes.put("monitoringScope", inputMap.get(MONITORING_SCOPE).toString().trim());
        extendedAttributes.put("Disbursement_Method", inputMap.get("disbursement_method").toString().trim());
        schemeDefinitionMap.put("extendedAttributes", extendedAttributes);

        return schemeDefinitionMap;
    }

    private String getSchemeType(Map<String, Object> inputMap) {
        int schemeType = Integer.parseInt(inputMap.get("calculation_method").toString().trim());
        if (schemeType == 1 || schemeType == 2)
            return VALUE_EACH;
        else if (schemeType == 3)
            return "percentage";
        else if (schemeType == 4)
            return ITEM_EACH;
        else if (schemeType == 5)
            return "percentageOnMrp";
        else if (schemeType == 6)
            return "flat";
        else
            throw new IllegalArgumentException("Scheme Type Not Supported : " + schemeType);
    }


    private String getSchemeCriteria(Map<String, Object> inputMap) {
        String monitoringScope = inputMap.get(MONITORING_SCOPE).toString().trim();
        String calculationMethod = inputMap.get("calculation_method").toString().trim();
        if (monitoringScope.equalsIgnoreCase("2") && (calculationMethod.equalsIgnoreCase("1") || calculationMethod.equalsIgnoreCase("3") ||
                calculationMethod.equalsIgnoreCase("4") || calculationMethod.equalsIgnoreCase("5")))
            return "itemwise";
        else if (monitoringScope.equalsIgnoreCase("2") && (calculationMethod.equalsIgnoreCase("2") || calculationMethod.equalsIgnoreCase("6")))
            return "itemwise_fixedprice";
        else if (monitoringScope.equalsIgnoreCase("1") && (calculationMethod.equalsIgnoreCase("5") || calculationMethod.equalsIgnoreCase("6")))
            return "itemwise";
        else if (monitoringScope.equalsIgnoreCase("1"))
            return "itemwise_group";

        return "itemwise_group";
    }


    private int getPriority(String monitoringScope, String marketScope) {
        if (monitoringScope.equalsIgnoreCase("3"))
            return 1;
        else if (marketScope.equalsIgnoreCase("1") && monitoringScope.equalsIgnoreCase("2"))
            return 1;
        else if (marketScope.equalsIgnoreCase("1") && monitoringScope.equalsIgnoreCase("1"))
            return 2;
        else if (marketScope.equalsIgnoreCase("6") && monitoringScope.equalsIgnoreCase("2"))
            return 3;
        else if (marketScope.equalsIgnoreCase("6") && monitoringScope.equalsIgnoreCase("1"))
            return 4;
        else if (marketScope.equalsIgnoreCase("3") && monitoringScope.equalsIgnoreCase("2"))
            return 5;
        else if (marketScope.equalsIgnoreCase("3") && monitoringScope.equalsIgnoreCase("1"))
            return 6;
        return 99;
    }
}
