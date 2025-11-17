package com.applicate.kbpl.transformer;

import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class KGPLSchemeTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    public static final String PROMOTION_PRODUCT_LEVEL = "promotionproductlevel";
    public static final String PROMOTION_DESCRIPTION = "promotiondescription";
    private static final String PROMOTION_ASSIGNMENTS = "promotionassignments";
    private static final String PROMOTION_ASSIGNMENT = "promotionassignment";
    private static final String PROMO_GROUPS = "promogroups";
    private static final String PROMO_GROUP = "promogroup";
    private static final String OUTLET_PROMOTION_MAPPINGS = "outletpromotionmappings";
    private static final String OUTLET_PROMOTION_MAPPING = "outletpromotionmapping";
    private static final String PROMOTION_TYPE_CODE = "promotiontypecode";
    private static final String ITEM_CODE = "itemcode";
    public static final String APPROVALSTATUS = "approvalstatus";
    public static final String CLOSURETYPE = "closuretype";
    public static final String LOGIN_ID = "loginId";
    public static final String CATEGORYCODE_1 = "categorycode1";
    public static final String CATEGORYCODE_2 = "categorycode2";
    public static final String CATEGORYCODE_3 = "categorycode3";
    public static final String CATEGORYCODE_4 = "categorycode4";
    public static final String CATEGORYCODE_5 = "categorycode5";
    public static final String CATEGORYCODE_6 = "categorycode6";
    public static final String CATEGORYCODE_7 = "categorycode7";
    public static final String CATEGORYCODE_8 = "categorycode8";
    public static final String CATEGORYCODE_9 = "categorycode9";
    public static final String CATEGORYCODE_10 = "categorycode10";
    public static final String STARTDATE = "startdate";
    public static final String BATCH_CODE = "batchCode";
    public static final String START_QTY_OR_VAL = "startQtyOrVal";
    public static final String CRETERIA = "criteria";
    public static final String PROGRAM_TYPE = "programType";
    public static final String LINKED = "linked";
    public static final String FOREACH = "foreach";
    public static final String ITEMWISE_GROUP = "itemwise_group";
    public static final String VALUE = "value";
    public static final String END_QTY_OR_VAL = "endQtyOrVal";
    private static final String KGPL = "-KGPL";
    private static final String SCHEME_ID = "schemeId";
    private static final String DISCOUNT_ID = "discountid";
    public static final String PROMOTIONAMOUNT = "promotionamount";
    public static final String RANGELOW = "rangelow";
    public static final String OUTLET_CATEGORY = "outletCategory";
    public static final String OUTLET_TYPE = "outletType";
    public static final String ITEM_CLASS = "itemClass";
    public static final String ITEM_EACH = "itemEach";
    public static final String CATEGORY = "category";
    public static final String SCHEME_DESCRIPTION = "schemeDescription";
    public static final String EXTENDED_ATTRIBUTES = "extendedAttributes";
    public static final String CHANNEL = "channel";
    public static final String CHANNEL1 = "subChannel";
    public static final String CHANNEL2 = "distributionChannel";
    public static final String OUTLET_CODE = "outletCode";
    public static final String BRAND = "brand";
    public static final String ITEM_ID = "itemId";
    public static final String MIN_MAX_COND = "minMaxCond";
    public static final String RANGE_LEVEL_UNIT = "rangeLevelUnit";
    public static final String CUSTOM_FIELD = "customField";
    public static final String PERCENTAGE = "percentage";
    public static final String START_DATE = "startDate";
    public static final String STARTAMOUNT = "startamount";
    public static final String DISCOUNTED_AMOUNT = "discountedAmount";
    public static final String DISCOUNTED_QTY = "discountedQty";
    public static final String DISCOUNTED_PERCENTAGE = "discountedPercentage";
    public static final String RANGEHIGH = "rangehigh";
    public static final String END_DATE = "endDate";
    public static final String OUTLETCODE = "outletcode";
    public static final String PROMOTIONCODE_CANNOT_BE_EMPTY = "promotioncode cannot be empty";
    public static final String SUB_CATEGORY = "subCategory";
    public static final String ACTIVEINDICATOR = "activeindicator";
    private final Logger logger = LoggerFactory.getLogger(KGPLSchemeTransformer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inMap) {
        ObjectNode schemeMap = JSONUtils.toObjectNode(inMap);
        JsonNode promotionassignment;

        List<JsonNode> responseList = new ArrayList<>();
        try {
            if (NullUtils.isNotNull(schemeMap) && checkCondition(schemeMap)) {
                if (schemeMap.get(PROMOTION_ASSIGNMENTS).get(PROMOTION_ASSIGNMENT) instanceof ObjectNode) {
                    promotionassignment = schemeMap.get(PROMOTION_ASSIGNMENTS).get(PROMOTION_ASSIGNMENT);
                    promotionassignment = getJsonNode(schemeMap, promotionassignment);
                    promotionassignment = getJsonNode(schemeMap, promotionassignment);
                    responseList.addAll(appendPromoGroup(schemeMap, promotionassignment));
                } else {
                    JsonNode fieldsArray = schemeMap.get(PROMOTION_ASSIGNMENTS).get(PROMOTION_ASSIGNMENT);
                    fieldsArray = slabcorection(fieldsArray);
                    for (JsonNode field : fieldsArray) {
                        promotionassignment = getJsonNode(schemeMap, field);

                        responseList.addAll(appendPromoGroup(schemeMap, promotionassignment));
                    }
                }
            }
        } catch (Exception exception) {
            throw new DataTransformationService.TransformationException(
                    "Exception occurred while transforming schemes. Reason: " + exception.getLocalizedMessage(),
                    exception
            );

        }
        if (responseList.isEmpty()) {
            throw new DataTransformationService.TransformationException(
                    "No Data for schemes is persisted. Reason: Data is missing or invalid in keys: " +
                            PROMOTION_ASSIGNMENTS + ", " + PROMO_GROUPS + ", " + OUTLET_PROMOTION_MAPPINGS +
                            ", " + APPROVALSTATUS + ", " + PROMOTION_TYPE_CODE);

        } else {
            logger.info("Processed records for schemes with discount id : {} and total no. of records : {}", responseList.get(0).get(DISCOUNT_ID), responseList.size());
            List<Map<String, Object>> finalResponseList = responseList.stream()
                    .map(JSONUtils::toMap).collect(Collectors.toList());
            if (finalResponseList.isEmpty())
                throw new DataTransformationService.TransformationException("No Data for schemes is persisted. Reason : Found no records for supplier 298664 in the input.");
            logger.info(" Final records send to persist are {}  ", finalResponseList);
            return newtransformer(finalResponseList);
        }
    }


    public JsonNode slabcorection(JsonNode fieldsArray) {
        int i;
        for (i = 0; i < fieldsArray.size() - 1; i++) {
            checkSlab(fieldsArray.get(i));
            JsonNode currentElement = fieldsArray.get(i);
            JsonNode nextElement = fieldsArray.get(i + 1);

            int nextValue = (nextElement.get(RANGELOW).asInt() - 1);

            ((ObjectNode) currentElement).put(RANGEHIGH, nextValue);

        }
        checkSlab(fieldsArray.get(i));
        return fieldsArray;
    }

    /**
     * <p>
     * adding validations for the case where slab
     * rangelow or rangehigh is negative
     * rangelow is greater then rangehigh
     * </p>
     *
     * @param curr JsonNode which contains all the scheme info
     * @throws ParseException
     */
    private void checkSlab(JsonNode curr) {
        int rangelow = curr.get(RANGELOW).asInt();
        int rangehigh = curr.get(RANGEHIGH).asInt();
        if (
                (rangelow > rangehigh)
                        || (rangelow < 0)
                        || (rangehigh < 0)
        ) {
            throw new RuntimeException("Either Slab doesn't exist or the slab is wrong");
        }
    }

    public List<Map<String, Object>> newtransformer(List<Map<String, Object>> inputMap) {
        List<Map<String, Object>> finalData = new ArrayList<>();
        for (Map<String, Object> single : inputMap) {
            Map<String, Object> schemeData = schemeDefinition(single);
            schemeData.put("schemeCalculation", calculationTransformer(single));
            schemeData.put("schemeProductBifurcationsList", schemeProductTransformer(single));
            schemeData.put("schemeOutletBifurcationsList", schemeOutletTransformer(single));
            finalData.add(schemeData);
        }
        return finalData;
    }

    private Map<String, Object> schemeOutletTransformer(Map<String, Object> inputMap) {
        Map<String, Object> schemeOutletMap = new HashMap<>();
        schemeOutletMap.put("channel", "all");
        schemeOutletMap.put("subChannel", "all");
        schemeOutletMap.put("outletCategory", "all");
        schemeOutletMap.put("outletCode", "all");
        schemeOutletMap.put("outletType", "all");
        schemeOutletMap.put("loginId", "all");
        schemeOutletMap.put("distributionChannel", "all");
        schemeOutletMap.put("account", "all");
        schemeOutletMap.put("outletClass", "all");
        schemeOutletMap.put("marketName", "all");
        schemeOutletMap.put("subTerritory", "all");
        schemeOutletMap.put("soldTo", "all");
        schemeOutletMap.put("marketId", "all");
        schemeOutletMap.put("outletDivision", "all");
        schemeOutletMap.put("priceListId", "all");
        schemeOutletMap.put("beat", "all");
        ifEmpty(inputMap.get(DISCOUNT_ID)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException(PROMOTIONCODE_CANNOT_BE_EMPTY);
        });

        ifEmpty(inputMap.get(LOGIN_ID)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("distributorcode cannot be empty");
        });

        String outletCode = inputMap.get(OUTLET_CODE).toString();
        if(!outletCode.equals("all")) outletCode = outletCode + KGPL;


        schemeOutletMap.put("source", "KGPL");
        schemeOutletMap.put("priceListId", "all");
        schemeOutletMap.put(SCHEME_ID, inputMap.get(DISCOUNT_ID) + KGPL);
        schemeOutletMap.put(OUTLET_CODE, outletCode);
        schemeOutletMap.put(LOGIN_ID, inputMap.get(LOGIN_ID) + KGPL);
        schemeOutletMap.put(OUTLET_CATEGORY, inputMap.get(OUTLET_CATEGORY));
        schemeOutletMap.put(OUTLET_TYPE, inputMap.get(OUTLET_TYPE));
        schemeOutletMap.put(CHANNEL, inputMap.get(CHANNEL));
        schemeOutletMap.put(CHANNEL1, inputMap.get(CHANNEL1));
        schemeOutletMap.put(CHANNEL2, inputMap.get(CHANNEL2));


        return schemeOutletMap;
    }

    private Map<String, Object> schemeProductTransformer(Map<String, Object> inputMap) {
        Map<String, Object> schemeProductMap = new HashMap<>();
        schemeProductMap.put("source", "KGPL");
        schemeProductMap.put("brand", "all");
        schemeProductMap.put("itemClass", "all");
        schemeProductMap.put("category", "all");
        schemeProductMap.put("itemId", "all");
        schemeProductMap.put("brand", "all");
        schemeProductMap.put("batchCode", "all");
        schemeProductMap.put("customGroupCode", "all");
        schemeProductMap.put("marketSku", "all");
        schemeProductMap.put("purchaseUnit", "all");
        schemeProductMap.put("qualifier", "all");
        schemeProductMap.put("skuCode", "all");
        schemeProductMap.put("articleCode", "all");
        schemeProductMap.put("category", "all");
        schemeProductMap.put("subCategory", "all");
        schemeProductMap.put("itemClass", "all");
        schemeProductMap.put("itemId", "all");
        schemeProductMap.put("flavour", "all");
        schemeProductMap.put("pieceSizeDesc", "all");
        schemeProductMap.put("size", "all");
        schemeProductMap.put("subCategoryCode", "all");
        schemeProductMap.put("ctg", "all");
        schemeProductMap.put("pieceSize", "all");
        schemeProductMap.put("itemType", "all");
        schemeProductMap.put("product", "all");

        ifEmpty(inputMap.get(DISCOUNT_ID)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException(PROMOTIONCODE_CANNOT_BE_EMPTY);
        });

        ifEmpty(inputMap.get(ITEM_ID)).ifPresentOrElse(val -> {}, () -> {
            ifEmpty(inputMap.get(BATCH_CODE)).ifPresentOrElse(val -> {}, () -> {
                throw new DataTransformationService.TransformationException("producthierarchycode and itemCode both cannot be empty, please provide one of them");
            });
        });

        schemeProductMap.put(SCHEME_ID, inputMap.get(DISCOUNT_ID) + KGPL);
        schemeProductMap.put(BRAND, "all");
        schemeProductMap.put(ITEM_CLASS, "all");
        schemeProductMap.put(CATEGORY, "all");
        schemeProductMap.put(ITEM_ID, "all");
        schemeProductMap.put("customGroupCode", "all");
        schemeProductMap.put(BATCH_CODE, inputMap.get(BATCH_CODE));

        if (NullUtils.isNotNull(inputMap.get(SUB_CATEGORY)) && !ObjectUtils.isEmpty(inputMap.get(SUB_CATEGORY)))
            schemeProductMap.put(SUB_CATEGORY, inputMap.get(SUB_CATEGORY).toString());
        else schemeProductMap.put(SUB_CATEGORY, "all");

        if (NullUtils.isNotNull(inputMap.get(BRAND)) && !ObjectUtils.isEmpty(inputMap.get(BRAND)))
            schemeProductMap.put(BRAND, inputMap.get(BRAND));
        if (NullUtils.isNotNull(inputMap.get(ITEM_CLASS)) && !ObjectUtils.isEmpty(inputMap.get(ITEM_CLASS)))
            schemeProductMap.put(ITEM_CLASS, inputMap.get(ITEM_CLASS));
        if (NullUtils.isNotNull(inputMap.get(CATEGORY)) && !ObjectUtils.isEmpty(inputMap.get(CATEGORY)))
            schemeProductMap.put(CATEGORY, inputMap.get(CATEGORY));
        if (NullUtils.isNotNull(inputMap.get(ITEM_ID)) && !ObjectUtils.isEmpty(inputMap.get(ITEM_ID)) && !inputMap.get(ITEM_ID).toString().equals("all"))
            schemeProductMap.put(ITEM_ID, inputMap.get(ITEM_ID).toString().trim() + KGPL);
        return schemeProductMap;
    }

    private Map<String, Object> calculationTransformer(Map<String, Object> inputMap) {
        ifEmpty(inputMap.get(DISCOUNT_ID)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException(PROMOTIONCODE_CANNOT_BE_EMPTY);
        });

        ifEmpty(inputMap.get(CRETERIA)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("promotionproductlevel cannot be empty");
        });

        ifEmpty(inputMap.get("type")).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("promotiontypecode cannot be empty");
        });

        Map<String, Object> schemeCalculationMap = new HashMap<>();
        schemeCalculationMap.put(SCHEME_ID, inputMap.get(DISCOUNT_ID) + KGPL);
        schemeCalculationMap.put(CRETERIA, inputMap.get(CRETERIA));
        schemeCalculationMap.put("schemeType", inputMap.get("type").toString().toUpperCase());
        ArrayNode slabArray = getSlabIfAlreadyExist(inputMap);
        schemeCalculationMap.put("slabInfo", slabArray);
        if (ObjectUtils.isEmpty(inputMap.get(MIN_MAX_COND))) {
            schemeCalculationMap.put(RANGE_LEVEL_UNIT, "amount");
        } else {
            switch (inputMap.get(MIN_MAX_COND).toString()) {
                case "nq":
                    schemeCalculationMap.put(RANGE_LEVEL_UNIT, "nq");
                    break;
                case "pc":
                    schemeCalculationMap.put(RANGE_LEVEL_UNIT, "piece");
                    break;
                case "cs":
                    schemeCalculationMap.put(RANGE_LEVEL_UNIT, "case");
                    break;
                default:
                    throw new DataTransformationService.TransformationException("Error in range_level_unit formation");
            }
        }
        schemeCalculationMap.put("usageLimit", inputMap.get("usageLimit"));
        schemeCalculationMap.put("maxDiscount", "0");
        schemeCalculationMap.put("maxTerm", "0");
        schemeCalculationMap.put("minimumAmount", "0");
        schemeCalculationMap.put(CUSTOM_FIELD, inputMap.get(CUSTOM_FIELD));
//        if (inputMap.get("type") != PERCENTAGE) {
//            schemeCalculationMap.put(ITEM_EACH, 1);
//        } else {
//            schemeCalculationMap.put(ITEM_EACH, 0);
//        }


        if (!PERCENTAGE.equalsIgnoreCase(inputMap.get("type").toString())) {
            if ("item".equalsIgnoreCase(inputMap.get("type").toString())) {
                schemeCalculationMap.put(ITEM_EACH, 2);
            } else {
                schemeCalculationMap.put(ITEM_EACH, 1);
            }
        } else {
            schemeCalculationMap.put(ITEM_EACH, 0);
        }

        if (NullUtils.isNotNull(inputMap.get("type")) && inputMap.get("type").toString().equalsIgnoreCase("item")) {
            String str = inputMap.get("discountedBatchCode").toString();
            String[] res = str.split(",");
            schemeCalculationMap.put("schemeDiscountedProductcode", res[0] + KGPL);
            schemeCalculationMap.put("schemeDiscountedProductcodeUOM", "nq");
        }

        return schemeCalculationMap;
    }


    private Map<String, Object> schemeDefinition(Map<String, Object> inputMap) {
        ifEmpty(inputMap.get(DISCOUNT_ID)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException(PROMOTIONCODE_CANNOT_BE_EMPTY);
        });

        ifEmpty(inputMap.get(CRETERIA)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("promotionproductlevel cannot be empty");
        });

        ifEmpty(inputMap.get("type")).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("promotiontypecode cannot be empty");
        });

        ifEmpty(inputMap.get(START_DATE)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("startdate cannot be empty");
        });

        ifEmpty(inputMap.get(END_DATE)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("enddate cannot be empty");
        });

        Map<String, Object> schemeDefinitionMap = new HashMap<>();
        schemeDefinitionMap.put("source", "KGPL");
        schemeDefinitionMap.put(SCHEME_ID, inputMap.get(DISCOUNT_ID) + KGPL);
        schemeDefinitionMap.put(CRETERIA, inputMap.get(CRETERIA));
        schemeDefinitionMap.put("schemeName", inputMap.get("discountName"));
        schemeDefinitionMap.put("programLevel", inputMap.get("programlevel"));
        schemeDefinitionMap.put(SCHEME_DESCRIPTION, inputMap.get(SCHEME_DESCRIPTION));
        schemeDefinitionMap.put("schemeType", inputMap.get("type"));
        String startDateInput = inputMap.get(START_DATE).toString();
        String endDateInput = inputMap.get(END_DATE).toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date startDate = sdf.parse(startDateInput);
            Date endDate = sdf.parse(endDateInput);
            schemeDefinitionMap.put(END_DATE, endDate);
            schemeDefinitionMap.put(START_DATE, startDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        schemeDefinitionMap.put("priority", "0");
        schemeDefinitionMap.put(EXTENDED_ATTRIBUTES, inputMap.get(EXTENDED_ATTRIBUTES));
        return schemeDefinitionMap;
    }

    ArrayNode getSlabIfAlreadyExist(Map<String, Object> inputMap) {
        String slabFrom;
        String slabTo;
        if (NullUtils.isNotNull(inputMap.get(STARTAMOUNT)) && !ObjectUtils.isEmpty(inputMap.get(STARTAMOUNT))) {
            slabFrom = inputMap.get(STARTAMOUNT).toString();
            slabTo = inputMap.get("endamount").toString();
        } else {
            slabFrom = inputMap.get(START_QTY_OR_VAL).toString();
            slabTo = inputMap.get(END_QTY_OR_VAL).toString();
        }
        String slabDiscount;
        float taxval=0;
        if (NullUtils.isNotNull(inputMap.get(DISCOUNTED_AMOUNT)) && !ObjectUtils.isEmpty(inputMap.get(DISCOUNTED_AMOUNT))) {
            taxval=calculateTax(inputMap,inputMap.get(DISCOUNTED_AMOUNT), "KGPL");
            float discount = Float.parseFloat(inputMap.get(DISCOUNTED_AMOUNT).toString())+taxval;
            slabDiscount = String.valueOf(discount);
        } else if (NullUtils.isNotNull(inputMap.get(DISCOUNTED_QTY)) && !ObjectUtils.isEmpty(inputMap.get(DISCOUNTED_QTY))) {
            slabDiscount = inputMap.get(DISCOUNTED_QTY).toString();
        } else {
            float discount = Float.parseFloat(inputMap.get(DISCOUNTED_PERCENTAGE).toString());
            slabDiscount = String.valueOf(discount);
        }


        ObjectNode slabNode = new ObjectMapper().createObjectNode();

        ifEmpty(slabTo).ifPresentOrElse(val -> slabNode.put("endRange", slabTo) , () -> {
            throw new DataTransformationService.TransformationException("slabInfo-endRange value can not be empty");
        });

        ifEmpty(slabFrom).ifPresentOrElse(val -> slabNode.put("startRange", slabFrom) , () -> {
            throw new DataTransformationService.TransformationException("slabInfo-startRange value can not be empty");
        });

        ifEmpty(slabDiscount).ifPresentOrElse(val -> slabNode.put("schemeBenefit", slabDiscount) , () -> {
            throw new DataTransformationService.TransformationException("slabInfo-schemeBenefit value can not be empty");
        });

        ifEmpty(inputMap.get(SCHEME_DESCRIPTION)).ifPresentOrElse(val -> slabNode.put(SCHEME_DESCRIPTION, val) , () -> {
            throw new DataTransformationService.TransformationException("promotiondescription value can not be empty");
        });

        ArrayNode slabArray = new ObjectMapper().createArrayNode();
        slabArray.add(slabNode);
        return slabArray;
    }
    private float calculateTax(Map<String, Object> inputMap, Object discountedAmount, String sourceSuffix) {
        try{
            ProductDetailsService productDetailsService = (ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);
            String taxGroupCode = productDetailsService.fetchTaxGroupCode(inputMap, sourceSuffix);
            if (taxGroupCode == null || taxGroupCode.isEmpty()) {
                throw new DataTransformationService.TransformationException("taxGroupCode not found for this product");
            }
            taxGroupCode=taxGroupCode.replace("\"", "");
            taxGroupCode = taxGroupCode.replace("GST", "");
            float taxPercentage = Float.parseFloat(taxGroupCode);
            float currentDiscount=Float.parseFloat(discountedAmount.toString());
            return (currentDiscount*(taxPercentage/100));
        }
        catch (Exception e){
            throw new DataTransformationService.TransformationException("issue in tax calculation");
        }
    }

    private boolean checkCondition(JsonNode schemeMap) {
        return schemeMap.has(PROMOTION_ASSIGNMENTS) && schemeMap.get(PROMOTION_ASSIGNMENTS).has(PROMOTION_ASSIGNMENT)
                && schemeMap.has(PROMO_GROUPS) && schemeMap.get(PROMO_GROUPS).has(PROMO_GROUP)
                && schemeMap.has(OUTLET_PROMOTION_MAPPINGS) && schemeMap.get(OUTLET_PROMOTION_MAPPINGS).has(OUTLET_PROMOTION_MAPPING)
                && (schemeMap.get(PROMOTION_TYPE_CODE).asText().equalsIgnoreCase("1") || schemeMap.get(PROMOTION_TYPE_CODE).asText().equalsIgnoreCase("4") || schemeMap.get(PROMOTION_TYPE_CODE).asText().equalsIgnoreCase("2"))
                && (schemeMap.get(APPROVALSTATUS).asText().equalsIgnoreCase("1"));
    }


    private static JsonNode getJsonNode(JsonNode schemeMap, JsonNode promotionassignment) throws JsonProcessingException {
        ObjectNode node = (ObjectNode) promotionassignment;
        node.set(PROMOTION_DESCRIPTION, schemeMap.get(PROMOTION_DESCRIPTION));
        node.set(PROMOTION_TYPE_CODE, schemeMap.get(PROMOTION_TYPE_CODE));
        node.set("rangebasis", schemeMap.get("rangebasis"));
        node.set("amountbasis", schemeMap.get("amountbasis"));
        node.set("exclusionoption", schemeMap.get("exclusionoption"));
        node.set("prorata", schemeMap.get("prorata"));
        node.set(PROMOTION_PRODUCT_LEVEL, schemeMap.get(PROMOTION_PRODUCT_LEVEL));
        node.set(APPROVALSTATUS, schemeMap.get(APPROVALSTATUS));//Only ApprovalStatus value 2  promotions should apply
        node.set(CLOSURETYPE, schemeMap.get(CLOSURETYPE));//cloure value 0 is active and 1 is inactive
        promotionassignment = new ObjectMapper().readTree(node.toString());
        return promotionassignment;
    }

    // check rec
    private List<JsonNode> appendPromoGroup(JsonNode schemeMap, JsonNode field) throws IOException, ParseException {
        JsonNode promo;
        List<JsonNode> responseNode = new ArrayList<>();
        if (schemeMap.get(PROMO_GROUPS).get(PROMO_GROUP) instanceof ObjectNode) {
            ObjectNode obj = JSONUtils.getObjectMapper().convertValue(schemeMap.get(PROMO_GROUPS).get(PROMO_GROUP),ObjectNode.class);
            removeCategoryCodeFromObj(obj);
            promo = JSONUtils.mergeJsons(field, obj);
//            need an add check here
            responseNode.addAll(appendOutletPromotionMapping(schemeMap, promo, ""));
            return responseNode;
        } else {
            JsonNode fieldsArray = removeCategoryCode(JSONUtils.getObjectMapper().convertValue(schemeMap.get(PROMO_GROUPS).get(PROMO_GROUP),ArrayNode.class));
            String freeitem = getfreeitemlist(schemeMap);
            for (JsonNode promo1 : fieldsArray) {
                if (promo1.get("grouptype").asText().equalsIgnoreCase("Q")) {
                    promo = JSONUtils.mergeJsons(field, promo1);
                    responseNode.addAll(appendOutletPromotionMapping(schemeMap, promo, freeitem));
                }
            }
            return responseNode;
        }
    }

    private void removeCategoryCodeFromObj(ObjectNode obj){
        if(obj.has(CATEGORYCODE_1)){
            obj.remove(CATEGORYCODE_1);
        }
        if(obj.has(CATEGORYCODE_2)){
            obj.remove(CATEGORYCODE_2);
        }
        if(obj.has(CATEGORYCODE_3)){
            obj.remove(CATEGORYCODE_3);
        }
        if(obj.has(CATEGORYCODE_4)){
            obj.remove(CATEGORYCODE_4);
        }
        if(obj.has(CATEGORYCODE_5)){
            obj.remove(CATEGORYCODE_5);
        }
        if(obj.has(CATEGORYCODE_6)){
            obj.remove(CATEGORYCODE_6);
        }
        if(obj.has(CATEGORYCODE_7)){
            obj.remove(CATEGORYCODE_7);
        }
        if(obj.has(CATEGORYCODE_8)){
            obj.remove(CATEGORYCODE_8);
        }
        if(obj.has(CATEGORYCODE_9)){
            obj.remove(CATEGORYCODE_9);
        }
        if(obj.has(CATEGORYCODE_10)){
            obj.remove(CATEGORYCODE_10);
        }
    }

    /**
     * remove unwanted keys
     * @param node
     * @return
     */
    private JsonNode removeCategoryCode(ArrayNode node) {
        node.forEach(  arrayNode -> {
            ObjectNode obj = (ObjectNode)arrayNode;
            removeCategoryCodeFromObj(obj);
        });
        return JSONUtils.getObjectMapper().convertValue(node,JsonNode.class);
    }

    private static String getfreeitemlist(JsonNode schemeMap) {
        JsonNode fieldsArray = schemeMap.get(PROMO_GROUPS).get(PROMO_GROUP);
        List<String> freeitems = new ArrayList<>();
        for (JsonNode field : fieldsArray) {
            if (field.get("grouptype").asText().equalsIgnoreCase("A"))
                freeitems.add(field.get(ITEM_CODE).asText());
        }
        return String.join(",", freeitems);
    }

    private List<JsonNode> appendOutletPromotionMapping(JsonNode schemeMap, JsonNode field, String freeitem) throws IOException, ParseException {
        JsonNode outletpromo;
        List<JsonNode> responseNode = new ArrayList<>();
        if (schemeMap.get(OUTLET_PROMOTION_MAPPINGS).get(OUTLET_PROMOTION_MAPPING) instanceof ObjectNode) {
            outletpromo = JSONUtils.mergeJsons(field, schemeMap.get(OUTLET_PROMOTION_MAPPINGS).get(OUTLET_PROMOTION_MAPPING));
            responseNode.add(prepareFinalObject(outletpromo, freeitem));
            return responseNode;
        } else {
            JsonNode fieldsArray = schemeMap.get(OUTLET_PROMOTION_MAPPINGS).get(OUTLET_PROMOTION_MAPPING);

            for (JsonNode promo1 : fieldsArray) {
                outletpromo = JSONUtils.mergeJsons(field, promo1);
                responseNode.add(prepareFinalObject(outletpromo, freeitem));
            }
            return responseNode;
        }
    }

    private JsonNode prepareFinalObject(JsonNode field, String freeitem) throws ParseException {

        ObjectMapper mapper = new ObjectMapper();
        ObjectMapper mapper1 = new ObjectMapper();
        JsonNode associatedProgram = mapper.createObjectNode();
        JsonNode finalscheme = mapper1.createObjectNode();
        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ObjectNode node = (ObjectNode) associatedProgram;
        ObjectNode node1 = (ObjectNode) finalscheme;

        // associatedProgram body set started

        node.put(MIN_MAX_COND, "nq");
        // Type decission
        if (field.get(PROMOTION_TYPE_CODE).asText().equalsIgnoreCase("1")) {
            node.put("type", VALUE);
            node1.put("type", VALUE);
            // Values to be given As discount
            node.set(DISCOUNTED_AMOUNT, field.get(PROMOTIONAMOUNT));
        } else if (field.get(PROMOTION_TYPE_CODE).asText().equalsIgnoreCase("4")) {
            node.put("type", "item");
            node1.put("type", "item");
            node.put("discountedBatchCode", freeitem);
            // Qty to be given As discount
            node.set(DISCOUNTED_QTY, field.get(PROMOTIONAMOUNT));
        } else if (field.get(PROMOTION_TYPE_CODE).asText().equalsIgnoreCase("11")) {
            node.put("type", PERCENTAGE);
            node1.put("type", PERCENTAGE);
            node.put(CUSTOM_FIELD, "percentage_mrp");
            // percentage to be given As discount
            node.set(DISCOUNTED_PERCENTAGE, field.get(PROMOTIONAMOUNT));

        } else if (field.get(PROMOTION_TYPE_CODE).asText().equalsIgnoreCase("2")) {
            node.put("type", PERCENTAGE);
            node1.put("type", PERCENTAGE);
            //percentage to be given as discount
            node.set(DISCOUNTED_PERCENTAGE, field.get(PROMOTIONAMOUNT));

        }


        String closureType = field.get(CLOSURETYPE).asText();
        String percentageOffOnMrpKey;
        if (field.get(PROMOTION_TYPE_CODE).asText().equalsIgnoreCase("11")) {
            percentageOffOnMrpKey = "perOffOnMrp";
        }
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date yesterday = calendar.getTime();
        inputDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        String yesterdayDate = inputDateFormat.format(yesterday);

        String endDate = closureType.equalsIgnoreCase("1") ? yesterdayDate : (field.get("enddate").asText());
        ifEmpty(field.has(ACTIVEINDICATOR)).ifPresentOrElse(val -> {}, () -> {
            throw new DataTransformationService.TransformationException("activeindicator can not be empty");
        });
        String activeindicator = field.has(ACTIVEINDICATOR) ? field.get(ACTIVEINDICATOR).asText() : "not received";

        if (closureType.equalsIgnoreCase("0") && activeindicator.equalsIgnoreCase("0")) {
            endDate = yesterdayDate;
        }


        node.put(START_DATE, outputDateFormat.format(inputDateFormat.parse((field.get(STARTDATE).asText()))));
        node.put(END_DATE, outputDateFormat.format(inputDateFormat.parse(endDate)));
        node.set(DISCOUNT_ID, field.get("promotioncode"));
        node.put(OUTLET_CODE, fetchFieldValue(field, OUTLETCODE));
        node.set(LOGIN_ID, field.get("distributorcode"));

        // Product attribute on which scheme will be applied
        node.set(ITEM_ID, field.get("producthierarchycode"));
        String itemCode = fetchFieldValue(field, ITEM_CODE);
        if(itemCode.equals("all")) node.put(BATCH_CODE, itemCode);
        else node.put(BATCH_CODE, itemCode + KGPL);

        //Condition to be satisfied for Application
        float rangeLow = Float.parseFloat(field.get(RANGELOW).asText());
        float rangeHigh = Float.parseFloat(field.get(RANGEHIGH).asText());
        int res = Float.compare(rangeLow, rangeHigh);
        float endQtyOrVal = (((rangeLow < 1 && rangeHigh == 1) || (res == 0)) ? 9999 : rangeHigh);
        node.put(START_QTY_OR_VAL, rangeLow > 0 ? rangeLow : 1);
        node.put(END_QTY_OR_VAL, endQtyOrVal);


        //Criteria decision
        if (field.get(PROMOTION_PRODUCT_LEVEL).asText().equalsIgnoreCase("0")) {
            if (node.get(START_QTY_OR_VAL).floatValue() == 1 && node.get(END_QTY_OR_VAL).floatValue() == 9999) {
                node.put(CRETERIA, "itemwise");
            } else {
                node.put(CRETERIA, ITEMWISE_GROUP);
                node.put(PROGRAM_TYPE, LINKED);
                node1.put(PROGRAM_TYPE, LINKED);
            }
        } else {
            node.put(CRETERIA, ITEMWISE_GROUP);
            node.put(PROGRAM_TYPE, LINKED);
            node1.put(PROGRAM_TYPE, LINKED);
        }
        // extendedAttributes
        ObjectNode extended = objectMapper.createObjectNode();
        String foreach = field.has(FOREACH) && field.get(FOREACH).asText().equalsIgnoreCase("") ? "0" : field.get(FOREACH).asText();
        Integer repeatingRange = field.get("repeatingrange").intValue();
        extended.put(FOREACH, Float.valueOf(foreach));
        extended.put("rangeHighActual", rangeHigh);
        extended.put("repeatingRange", repeatingRange);
        extended.put("mq", field.has("quantity") ? field.get("quantity").floatValue() : 0.0f);
        extended.put("activeIndicator", activeindicator);
        extended.put("closureType", closureType);
        if (field.has(PROMOTION_TYPE_CODE) && field.get(PROMOTION_TYPE_CODE) != null && field.get(PROMOTION_TYPE_CODE).asText().equalsIgnoreCase("11")) {
            extended.put("percentageOffMrp", "true");
        }
        JsonNode extendedAttributes = JSONUtils.toJsonNode((Map<?, ?>) extended);
        node.set(EXTENDED_ATTRIBUTES, extendedAttributes);
        node1.set(EXTENDED_ATTRIBUTES, extendedAttributes);

        // finalscheme body set started

        node1.put(START_DATE, outputDateFormat.format(inputDateFormat.parse((field.get(STARTDATE).asText()))));
        node1.put(END_DATE, outputDateFormat.format(inputDateFormat.parse((field.get("enddate").asText()))));
        node1.set(DISCOUNT_ID, field.get("promotioncode"));
        node1.set(SCHEME_DESCRIPTION, field.get(PROMOTION_DESCRIPTION));
        node.set(SCHEME_DESCRIPTION, field.get(PROMOTION_DESCRIPTION));

        node1.set(LOGIN_ID, field.get("distributorcode"));
        node1.put(OUTLET_CODE, fetchFieldValue(field, OUTLETCODE));
        // Product attribute on which scheme will be applied
        node1.set(ITEM_ID, field.get("producthierarchycode"));
        if(itemCode.equals("all")) node1.put(BATCH_CODE, itemCode);
        else node1.put(BATCH_CODE, itemCode + KGPL);

        //Condtion to be satisfy for Application
        node1.put(START_QTY_OR_VAL, rangeLow > 0 ? rangeLow : 1);
        node1.put(END_QTY_OR_VAL, endQtyOrVal);

        // capping of discounted value
        if (repeatingRange.equals(0) && node.get(CRETERIA).asText().equalsIgnoreCase(ITEMWISE_GROUP) && node.get("type").asText().equalsIgnoreCase("item")) {
            float maxValue = node.get(DISCOUNTED_QTY).floatValue();
            node.put("maxValue", maxValue);
        }


        ObjectNode categoryCodes = getCategoryCodes(field);
        try {
            node = (ObjectNode) JSONUtils.mergeJsons(categoryCodes, node);
            node1 = (ObjectNode) JSONUtils.mergeJsons(categoryCodes, node1);
        } catch (Exception e) {
            throw new RuntimeException("Error while merging category node in offers and range program: " + e.getMessage(),e);
        }

        String schemeEntityId = node.get(DISCOUNT_ID).asText()
                .concat(node.get(LOGIN_ID).asText())
                .concat(node.get(BATCH_CODE).asText())
                .concat(outputDateFormat.format(inputDateFormat.parse((field.get(STARTDATE).asText()))))
                .concat(node.get(START_QTY_OR_VAL).asText())
                .concat(node.get(OUTLET_TYPE).asText()).concat(node.get("marketId").asText())
                .concat(node.get("marketName").asText()).concat(node.get(CHANNEL).asText())
                .concat(node.get(CHANNEL1).asText()).concat(node.get(OUTLET_CATEGORY).asText())
                .concat(node.get("outletClass").asText()).concat(node.get(CHANNEL2).asText())
                .concat(node.get("outletDivision").asText()).concat(node.get("account").asText());

        String id = getMd5(schemeEntityId);
        node.put("groupType", id);
        node1.put("groupType", id);


        //put associated data for filterrange
        node1.set("associatedProgram", node);

        return node;

    }


    private ObjectNode getCategoryCodes(JsonNode field) {

        ObjectNode categoryNode = new ObjectMapper().createObjectNode();
        categoryNode.put(OUTLET_TYPE, fetchFieldValue(field, CATEGORYCODE_9));
        categoryNode.put("marketId", fetchFieldValue(field, CATEGORYCODE_2));
        categoryNode.put("marketName", fetchFieldValue(field, CATEGORYCODE_3));
        categoryNode.put(CHANNEL, fetchFieldValue(field, CATEGORYCODE_4));
        categoryNode.put(CHANNEL1, fetchFieldValue(field, CATEGORYCODE_5));
        categoryNode.put(OUTLET_CATEGORY, fetchFieldValue(field, CATEGORYCODE_6));
        categoryNode.put("outletClass", fetchFieldValue(field, CATEGORYCODE_7));
        String distributionChannel = fetchFieldValue(field, CATEGORYCODE_8);
        if(distributionChannel.equals("all")) {
            categoryNode.put(CHANNEL2, distributionChannel);
        } else {
            categoryNode.put(CHANNEL2, distributionChannel + KGPL);
        }
        categoryNode.put("outletDivision", fetchFieldValue(field, CATEGORYCODE_1));
        categoryNode.put("account", fetchFieldValue(field, CATEGORYCODE_10));

        return categoryNode;
    }

    private String fetchFieldValue(JsonNode node, String field) {
        return (node.has(field) && StringUtils.isEmpty(node.get(field).asText().trim())) ? "all" : node.get(field).asText().trim();
    }


    public String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            StringBuilder hashtext = new StringBuilder(no.toString(16));
            while (hashtext.length() < 32) {
                hashtext.append("0").append(hashtext);
            }
            return hashtext.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<String> ifEmpty(Object object) {
        if(ObjectUtils.isEmpty(object))  {
            return Optional.empty();
        } else {
            return Optional.of(object.toString());
        }
    }
}