//package com.applicate.simamy.transformer;
//
//import com.applicate.services.channelkart.abstractdatasource.DatabaseProfileRegistry;
//import com.applicate.services.channelkart.exceptions.TransformationException;
//import com.applicate.services.channelkart.models.GenericEntity;
//import com.applicate.services.channelkart.models.MetaData;
//import com.applicate.services.channelkart.models.OutletDetails;
//import com.applicate.services.channelkart.repository.GenericEntityRepository;
//import com.applicate.services.channelkart.repository.MetaDataRepository;
//import com.applicate.services.channelkart.security.SecurityContextUtils;
//import com.applicate.services.channelkart.services.OutletDetailsService;
//import com.applicate.services.channelkart.services.SpringContext;
//import com.applicate.services.channelkart.transformers.AbstractTransformer;
//import com.applicate.services.channelkart.utils.JSONUtils;
//import com.applicate.services.channelkart.utils.JdbcUtils;
//import com.applicate.services.channelkart.utils.NullUtils;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.JsonNodeFactory;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.apache.commons.lang3.ObjectUtils;
//import org.apache.commons.lang3.SerializationUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import javax.sql.DataSource;
//import java.io.IOException;
//import java.net.URL;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.*;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.concurrent.atomic.AtomicReference;
//import java.util.function.Function;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//import java.util.stream.StreamSupport;
//
//public class SchemeTransformerCokeMY extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
//    private static final String SCHEME_ID = "schemeId";
//    private static final String ACTIVE_STATUS = "activeStatus";
//    private static final String SCHEME_TYPE = "schemeType";
//    private static final String SCHEME_DESCRIPTION = "schemeDescription";
//    private static final String SCHEME_FACTOR = "schemeFactor";
//    private static final String RANGE_LEVEL_UNIT = "rangeLevelUnit";
//    private static final String START_DATE = "startDate";
//    private static final String END_DATE = "endDate";
//    private static final String CRITERIA = "criteria";
//    private static final String SLAB_INFO = "slabInfo";
//    private static final String BATCH_CODE = "batchCode";
//    private static final String ITEM_EACH = "itemEach";
//    private static final String PIECE_SIZE = "pieceSize";
//    private static final String PIECE_SIZE_DESC = "pieceSizeDesc";
//    private static final String CATEGORY = "category";
//    private static final String SUB_CATEGORY = "subCategory";
//    private static final String ITEM_CLASS = "itemClass";
//    private static final String ITEM_ID = "itemId";
//    private static final String ITEM_TYPE = "itemType";
//    private static final String LOGIN_ID = "loginId";
//    private static final String DISTRIBUTION_CHANNEL = "distributionChannel";
//    private static final String SUB_CHANNEL = "subChannel";
//    private static final String OUTLET_CODE = "outletCode";
//    private static final String OUTLET_CLASS = "outletClass";
//    private static final String PRIORITY = "priority";
//    private static final String CHANNEL = "channel";
//    private static final String TRADENAME = "tradename";
//    private static final String FLAVOUR = "flavour";
//    private static final String SCHEME_BENEFIT = "schemeBenefit";
//    private static final String EXTENDED_ATTRIBUTES = "extendedAttributes";
//
//    private static final String PROMO_CODE = "PROMO_CD";
//    private static final String PROMO_DESC = "PROMO_DESC";
//    private static final String PROMO_INDEX = "PROMO_INDEX";
//    private static final String PROMO_IND = "PROMO_IND";
//    private static final String MUST_IND = "MUST_IND";
//    private static final String PROMO_STATUS = "PROMO_STATUS";
//    private static final String START_DT = "START_DT";
//    private static final String END_DT = "END_DT";
//    private static final String MECHANIC_TYPE = "MECHANIC_TYPE";
//    private static final String MST_PROMODTL = "MST_PROMODTL";
//    private static final String MST_PROMOPWP = "MST_PROMOPWP";
//    private static final String MST_PROMOPRD = "MST_PROMOPRD";
//    private static final String MST_PROMOPRD_GRP = "MST_PROMOPRD_GRP";
//    private static final String MUST_BUY_GROUP_CONDITION= "mustBuyGroupCondition";
//    private static final String GRP_CD = "GRP_CD";
//    private static final String MST_PROMOFOC = "MST_PROMOFOC";
//    private static final String MST_PROMOASSIGN = "MST_PROMOASSIGN";
//    private static final String PARENT_CD = "PARENT_CD";
//    private static final String FOC_QTY = "FOC_QTY";
//    private static final String TTLBUY_QTY = "TTLBUY_QTY";
//    private static final String PRDCAT_LEVEL = "PRDCAT_LEVEL";
//    private static final String ASS_TYPE = "ASS_TYPE";
//    private static final String ASS_CD = "ASS_CD";
//    private static final String APPLY_ON = "APPLY_ON";
//    private static final String MIN_QTY = "MIN_QTY";
//    private static final String BVGCATEGORY = "CATEGORY";
//    private static final String BRAND = "BRAND";
//    private static final String PACK_TYPE = "PACK_TYPE";
//    private static final String PACK_SIZE = "PACK_SIZE";
//    private static final String SALES_UNIT = "SALES_UNIT";
//    private static final String SCHEME_PRORATA = "SCHEME_PRORATA";
//    private static final String PROMO_PROGRESS = "PROMO_PROGRESS";
//    private static final String FOR_EVERY = "FOR_EVERY";
//    private static final String FOR_EVERY_FLAG = "FOR_EVERY_FLAG";
//
//    private static final String CUSTOM_FIELD = "customField";
//    private static final String BATCHCODE = "batch_code";
//    private static final String FLAVOR = "FLAVOR";
//    private static final String BEVERAGE_TYPE = "BEVERAGE_TYPE";
//    private static final String BRAND_NAME = "brand";
//    static final String PRODUCT_MAPPING = "productMapping";
//    private static final String ERROR_MSG = "Request body doesn't have ";
//
//    private final Logger logger = LoggerFactory.getLogger(SchemeTransformerCokeMY.class);
//
//    GenericEntityRepository repository = SpringContext.getBean(GenericEntityRepository.class);
//
//    static Map<String, Double> unitConversionMap = new HashMap<>();
//
//    MetaDataRepository metaDataRepository = SpringContext.getBean(MetaDataRepository.class);
//    OutletDetailsService outletDetailsService = SpringContext.getBean(OutletDetailsService.class);
//
//    public SchemeTransformerCokeMY() {
//        unitConversionMap.put("ML", 1.0);
//        unitConversionMap.put("ml", 1.0);
//        unitConversionMap.put("l", 1000.0);
//        unitConversionMap.put("L", 1000.0);
//        unitConversionMap.put("LTR", 1000.0);
//        unitConversionMap.put("KG", 1000.0);
//        unitConversionMap.put("kg", 1000.0);
//        unitConversionMap.put("GM", 1.0);
//        unitConversionMap.put("gm", 1.0);
//    }
//    private JsonNode fetchDataFromUrl(String url) throws IOException {
//        JsonNode rootNode = JSONUtils.getObjectMapper().readTree(new URL(url));
//        if (rootNode.isArray()) {
//            return rootNode.get(0);
//        } else {
//            return rootNode;
//        }
//    }
//
//    @Override
//    public List<Map<String, Object>> transform(Map<String, Object> inMap) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        if(inMap.get("ckIntS3FileUrl")!=null) {
//            String s3FileUrl = inMap.get("ckIntS3FileUrl").toString();
//            try {
//                inMap = JSONUtils.getObjectMapper().convertValue(fetchDataFromUrl(s3FileUrl),Map.class);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        JsonNode schemeMap = objectMapper.valueToTree(inMap.get("MST_PROMO"));
//        if(schemeMap.has(PROMO_IND) && Objects.equals(schemeMap.get(PROMO_IND).asText().trim(), "")){
//            throw new TransformationException("PROMO_IND received as empty string");
//        }
//        if (schemeMap.has(PROMO_IND) && schemeMap.get(PROMO_IND).asText().trim().equalsIgnoreCase("P")) {
//            throw new TransformationException("PROMO_IND received as: " + schemeMap.get(PROMO_IND).asText());
//        }
//
//        List<JsonNode> responseList = new ArrayList<>();
//        try {
//            if (NullUtils.isNotNull(schemeMap)) {
//                checkCondition(schemeMap);
//                responseList.add(appendPromo(schemeMap));
//            }
//        } catch (Exception exception) {
//            throw new TransformationException("Exception Occurred while transforming schemes. Reason {}", exception.getLocalizedMessage());
//        }
//        if (!responseList.isEmpty()) {
//            logger.info("Processed records for schemes with discount id : {} and total no. of records : {}", responseList.get(0).get(""), responseList.size());
//            List<Map<String, Object>> finalResponseList = responseList.stream()
//                                                                      .map(JSONUtils::toMap).collect(Collectors.toList());
//            logger.info(" Final records send to persist are {}  ", finalResponseList);
//            return finalResponseList;
//        }
//
//        return new LinkedList<>();
//    }
//
//    private JsonNode appendPromo(JsonNode schemeMap) {
//        String promoType = schemeMap.get(MST_PROMODTL).isArray() ?
//                schemeMap.get(MST_PROMODTL).get(0).get(MECHANIC_TYPE).asText() : schemeMap.get(MST_PROMODTL).get(MECHANIC_TYPE).asText();
//
//        List<ObjectNode> productBifurcationsList = collectProductBifurcations(schemeMap, promoType);
//        List<ObjectNode> outletBifurcationsList = collectOutletBifurcations(schemeMap);
//
//        return prepareFinalObject(schemeMap, promoType, productBifurcationsList, outletBifurcationsList);
//    }
//
//    private List<ObjectNode> collectProductBifurcations(JsonNode schemeMap, String promoType) {
//        return StreamSupport.stream(schemeProductTransformer(schemeMap, promoType).spliterator(), false)
//                            .map(ObjectNode.class::cast)
//                            .collect(Collectors.toList());
//    }
//
//    private List<ObjectNode> collectOutletBifurcations(JsonNode schemeMap) {
//        return StreamSupport.stream(schemeOutletTransformer(schemeMap).spliterator(), false)
//                            .map(ObjectNode.class::cast)
//                            .collect(Collectors.toList());
//    }
//
//    private JsonNode prepareFinalObject(JsonNode schemeMap, String promoType, List<ObjectNode> productBifurcationsList, List<ObjectNode> outletBifurcationsList) {
//        ObjectNode finalObject = getSchemeDefinition(schemeMap, promoType);
//        finalObject.set("schemeCalculation", calculationTransformer(schemeMap, finalObject, promoType));
//        finalObject.set("schemeProductBifurcationsList", JSONUtils.getObjectMapper().valueToTree(productBifurcationsList));
//        finalObject.set("schemeOutletBifurcationsList", JSONUtils.getObjectMapper().valueToTree(outletBifurcationsList));
//
//        return finalObject;
//    }
//
//    private ObjectNode getSchemeDefinition(JsonNode schemeMap, String promoType) {
//        ObjectNode schemeDefinition = JsonNodeFactory.instance.objectNode();
//
//        schemeDefinition.set(SCHEME_ID, schemeMap.get(PROMO_CODE));
//        schemeDefinition.put(CRITERIA, "itemwise_group");
//        schemeDefinition.set(SCHEME_DESCRIPTION, schemeMap.get(PROMO_DESC));
//        schemeDefinition.put(SCHEME_TYPE, getSchemeType(promoType));
//        schemeDefinition.set(PRIORITY, schemeMap.get("PROMO_CATEGORY_SEQ"));
//
//        if (schemeMap.get(PROMO_STATUS).asText().equalsIgnoreCase("A")) {
//            schemeDefinition.put(ACTIVE_STATUS, "active");
//        } else {
//            schemeDefinition.put(ACTIVE_STATUS, "inactive");
//        }
//        SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd");
//        SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//
//        try {
//            String start = outputDateFormat.format(inputDateFormat.parse(schemeMap.get(START_DT).asText()));
//            Date startDate = outputDateFormat.parse(start);
//            String end = outputDateFormat.format(inputDateFormat.parse(schemeMap.get(END_DT).asText()));
//            Date endDate = outputDateFormat.parse(end);
//            schemeDefinition.put(START_DATE, startDate.getTime());
//            schemeDefinition.put(END_DATE, endDate.getTime());
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//
//        schemeDefinition.set(EXTENDED_ATTRIBUTES, getDefinitionExtendedAttributes(schemeMap, promoType));
//
//        return schemeDefinition;
//    }
//
//    private ObjectNode calculationTransformer(JsonNode schemeMap, ObjectNode finalObject, String promoType) {
//        ObjectNode schemeCalculationMap = JsonNodeFactory.instance.objectNode();
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        schemeCalculationMap.set(SCHEME_ID, finalObject.get(SCHEME_ID));
//        schemeCalculationMap.set(CRITERIA, finalObject.get(CRITERIA));
//        schemeCalculationMap.set(SCHEME_TYPE, finalObject.get(SCHEME_TYPE));
//
//        ArrayNode promoDetails;
//
//        if (schemeMap.get(MST_PROMODTL).isObject()) {
//            promoDetails = objectMapper.createArrayNode();
//            promoDetails.add(schemeMap.get(MST_PROMODTL));
//        } else {
//            promoDetails = (ArrayNode) schemeMap.get(MST_PROMODTL);
//        }
//
//        ArrayNode slabArray = getSlabIfAlreadyExist(schemeMap, promoDetails, finalObject, promoType);
//
//        schemeCalculationMap.set(SLAB_INFO, slabArray);
//
//        if (schemeMap.get("DTL_TYPE").asText().equalsIgnoreCase("A")) {
//            schemeCalculationMap.put(RANGE_LEVEL_UNIT, "amount");
//        } else if (schemeMap.get("DTL_TYPE").asText().equalsIgnoreCase("Q")) {
//            schemeCalculationMap.put(RANGE_LEVEL_UNIT, "cs");
//        }
//
//        schemeCalculationMap.put(ITEM_EACH, getItemEach(schemeMap, promoDetails));
//
//        if (NullUtils.isNotNull(finalObject.get(SCHEME_TYPE)) && finalObject.get(SCHEME_TYPE).asText().equalsIgnoreCase("item")) {
//            ArrayNode schemeFreeProductInfo = freeProductTransformer(schemeMap, schemeMap.get(PROMO_CODE));
//            addFreeProductInfoList(schemeFreeProductInfo, schemeCalculationMap, finalObject, schemeMap);
//        }
//
//        ArrayNode schemeMustBuyGroupList = schemeMustBuyTransformer(schemeMap);
//        if (!schemeMustBuyGroupList.isEmpty()) {
//            schemeCalculationMap.set("schemeMustBuyGroupList", schemeMustBuyGroupList);
//            schemeCalculationMap.set("mustBuyGroupId", schemeMap.get(PROMO_CODE));
//        }
//
//        schemeCalculationMap.set(EXTENDED_ATTRIBUTES, getCalculationExtendedAttributes(schemeMap, promoDetails));
//
//        return schemeCalculationMap;
//    }
//
//    private void addFreeProductInfoList(ArrayNode schemeFreeProductInfo, ObjectNode schemeCalculationMap, ObjectNode finalObject, JsonNode schemeMap) {
//        if (!schemeFreeProductInfo.isEmpty()) {
//            Set<JsonNode> uniqueSet = new HashSet<>();
//            ArrayNode uniqueList = JsonNodeFactory.instance.arrayNode();
//
//            for (JsonNode node : schemeFreeProductInfo) {
//                if (uniqueSet.add(node)) {
//                    uniqueList.add(node);
//                }
//            }
//
//            schemeCalculationMap.set("schemeFreeProductInfoList", uniqueList);
//            schemeCalculationMap.set("freeProductInfoId", schemeMap.get(PROMO_CODE));
//        }
//    }
//
//    private ArrayNode schemeOutletTransformer(JsonNode schemeMap) {
//        ArrayNode outletListNode = JsonNodeFactory.instance.arrayNode();
//
//        JsonNode promoAssign = schemeMap.get(MST_PROMOASSIGN);
//
//        //To check if the supplier provided is a migrated supplier
//        List<MetaData> migratedSuppliers = metaDataRepository.findByDomainName("MigratedSuppliers");
//
//        List<JsonNode> channelNodes = new ArrayList<>();
//        List<JsonNode> distributorNodes = new ArrayList<>();
//        List<JsonNode> outletClassNodes = new ArrayList<>();
//        List<JsonNode> outletCodeNodes = new ArrayList<>();
//        Set<String> outletCodesSet = new HashSet<>();
//        Set<String> distributorSet = new HashSet<>();
//
//        for (JsonNode node : promoAssign) {
//            String type = node.get(ASS_TYPE).asText();
//            switch (type) {
//                case "A": channelNodes.add(node); break;
//                case "D":
//                    distributorNodes.add(node);
//                    distributorSet.add(node.get(ASS_CD).asText());
//                    break;
//                case "R": outletClassNodes.add(node); break;
//                case "C":
//                    outletCodeNodes.add(node);
//                    outletCodesSet.add(node.get(ASS_CD).asText());
//                    break;
//                default: break;
//            }
//        }
//
//        Map<String, OutletDetails> outletDetailsMap = new HashMap<>();
//
//        if(!outletClassNodes.isEmpty() || !channelNodes.isEmpty()) {
//            List<OutletDetails> outletDetailsList = outletDetailsService.findByOutletCodeIn(new ArrayList<>(outletCodesSet));
//            outletDetailsMap = outletDetailsList.stream()
//                                                .collect(Collectors.toMap(OutletDetails::getOutletCode, Function.identity()));
//        }
//
//        //if no channels or outlet classes provided, add outlet codes directly under distributors
//        if(outletClassNodes.isEmpty() && channelNodes.isEmpty()) {
//            addOnlyOutletAssignmentsUnderDistributor(distributorSet, outletCodeNodes, outletListNode);
//        }
//
//        distributorNodes.forEach(assignDetails -> {
//            String assignmentType = assignDetails.get(ASS_TYPE).asText();
//            if (assignmentType.equalsIgnoreCase("D")) {
//                String distributor = assignDetails.get(ASS_CD).asText();
//
//                if (NullUtils.isNull(distributor) || distributor.isEmpty() || distributor.equalsIgnoreCase("*")) {
//                    throw new TransformationException("Supplier received as null/empty/*");
//                }
//
//                if (!checkForMigratedSupplier(migratedSuppliers, distributor)) {
//                    throw new TransformationException("supplier " + distributor + " is not migrated");
//                }
//
//                addAssignmentsUnderDistributor(distributor, channelNodes, outletClassNodes, outletListNode);
//            }
//        });
//
//        String promoId = schemeMap.get(PROMO_CODE).asText();
//        //add the outlets under respective distributors that are not already assigned
//        addOutletAssignmentUnderDistributor(outletDetailsMap, distributorSet, outletListNode, outletCodeNodes, promoId);
//
//        return outletListNode;
//    }
//
//    private void addOutletAssignmentUnderDistributor(Map<String, OutletDetails> outletDetailsMap, Set<String> distributors, ArrayNode outletListNode, List<JsonNode> outletCodeNodes, String promoId) {
//        Set<String> outletClasses = new HashSet<>();
//        Set<String> channels = new HashSet<>();
//
//        for(JsonNode outletNode : outletListNode) {
//            if(outletNode==null)
//                continue;
//            distributors.add(outletNode.get(LOGIN_ID).asText());
//            outletClasses.add(outletNode.get(OUTLET_CLASS).asText());
//            channels.add(outletNode.get(CHANNEL).asText());
//        }
//
//        //if outlet class set and channel set only contains "all", no need to create new row
//        if(outletClasses.size() > 1 && outletClasses.contains("all") && channels.size() > 1 && channels.contains("all")) {
//            return;
//        }
//
//        for(JsonNode outletCodeNode : outletCodeNodes) {
//            String outletCode = outletCodeNode.get(ASS_CD).asText();
//            OutletDetails outletDetails = outletDetailsMap.get(outletCode);
//            if(outletDetails == null || outletDetails.getExtendedAttributes() == null || outletDetails.getOutletClass() == null || outletDetails.getChannel() == null) {
//                logger.warn("outlet details are null for outlet code: {}", outletCode);
//                continue;
//            }
//            String outletDistributor = outletCodeNode.get(PARENT_CD).asText();
//            String outletClass = outletDetails.getOutletClass();
//            String outletChannel = outletDetails.getChannel();
//
//            if (distributors.contains(outletDistributor)) {
//                boolean outletClassMismatch = !outletClasses.contains(outletClass) && !outletClasses.contains("all");
//                boolean channelMismatch = !channels.contains(outletChannel) && !channels.contains("all");
//
//                if (outletClassMismatch || channelMismatch) {
//                    outletListNode.add(createSchemeDistOutletObject(outletDistributor, outletCode, promoId));
//                }
//            }
//        }
//    }
//
//    private ObjectNode createSchemeDistOutletObject(String distributor, String outletCode, String promoId) {
//        ObjectNode schemeOutletObject = JsonNodeFactory.instance.objectNode();
//
//        schemeOutletObject.put(SCHEME_ID, promoId);
//
//        schemeOutletObject.put(LOGIN_ID, distributor);
//        schemeOutletObject.put(OUTLET_CLASS, "all");
//        schemeOutletObject.put(DISTRIBUTION_CHANNEL, "all");
//        schemeOutletObject.put(SUB_CHANNEL, "all");
//        schemeOutletObject.put(OUTLET_CODE, outletCode);
//        schemeOutletObject.put(CHANNEL, "all");
//
//        setDefaultOutletValues(schemeOutletObject);
//
//        return schemeOutletObject;
//    }
//
//    private boolean checkForMigratedSupplier(List<MetaData> migratedSuppliers, String distributor) {
//        ArrayNode migratedSuppliersList = migratedSuppliers.get(0).getDomainValues();
//        for (JsonNode node : migratedSuppliersList) {
//            if (node.has(distributor)) {
//                return true; // Distributor found
//            }
//        }
//        return false; // Distributor not found
//    }
//
//
//    private void addAssignmentsUnderDistributor(String distributor, List<JsonNode> channelNodes, List<JsonNode> outletClassNodes, ArrayNode outletListNode) {
//        if(!outletClassNodes.isEmpty()){
//            outletClassNodes.forEach(assignment -> {
//                String assignmentType = assignment.get(ASS_TYPE).asText();
//                if (assignmentType.equalsIgnoreCase("R")) {
//                    String segment = getOutletClass(assignment);
//                    if (!channelNodes.isEmpty()) {
//                        addAssignmentsUnderSegment(distributor, segment, channelNodes, outletListNode);
//                    } else {
//                        // "A" is missing, save as Distributor-Segment
//                        ObjectNode schemeOutletObject = createSchemeOutletObject(distributor, segment, assignment, assignmentType);
//                        outletListNode.add(schemeOutletObject);
//                    }
//                }
//            });
//        } else if (!channelNodes.isEmpty()){
//            channelNodes.forEach(assignment -> {
//                String assignmentType = assignment.get(ASS_TYPE).asText();
//                if (assignmentType.equalsIgnoreCase("A")) {
//                    ObjectNode schemeOutletObject = createSchemeOutletObject(distributor, "all", assignment, assignmentType);
//                    if(schemeOutletObject!=null){
//                        outletListNode.add(schemeOutletObject);
//                    }}
//            });
//        }
//    }
//
//    private void addAssignmentsUnderSegment(String distributor, String segment, List<JsonNode> channelCodes, ArrayNode outletListNode) {
//        channelCodes.forEach(assignment -> {
//            String assignmentType = assignment.get(ASS_TYPE).asText();
//            if (assignmentType.equalsIgnoreCase("A")) {
//                ObjectNode schemeOutletObject = createSchemeOutletObject(distributor, segment, assignment, assignmentType);
//                if(schemeOutletObject!=null){
//                    outletListNode.add(schemeOutletObject);
//                }}
//        });
//    }
//
//    private ObjectNode createSchemeOutletObject(String distributor, String segment, JsonNode assignDetails, String assignmentType) {
//        ObjectNode schemeOutletObject = JsonNodeFactory.instance.objectNode();
//
//        schemeOutletObject.put(SCHEME_ID, assignDetails.get(PROMO_CODE).asText());
//
//        String assignmentCode = assignDetails.get(ASS_CD).asText();
//        String fieldName = "";
//        if (assignmentType.equalsIgnoreCase("A")) {
//            fieldName = determineOutletFieldName(assignDetails);
//        }
//
//
//        if (fieldName.equalsIgnoreCase(TRADENAME)) {
//            String tradeName = getTradeName(assignmentCode);
//            if (tradeName == null) {
//                return null;
//            }
//            schemeOutletObject.put("marketName", tradeName);
//        }
//        else
//            schemeOutletObject.put("marketName", "all");
//
//        schemeOutletObject.put(LOGIN_ID, distributor);
//        schemeOutletObject.put(OUTLET_CLASS, segment);
//        schemeOutletObject.put(DISTRIBUTION_CHANNEL, getFieldValue(fieldName, DISTRIBUTION_CHANNEL, assignmentCode));
//        schemeOutletObject.put(SUB_CHANNEL, getFieldValue(fieldName, SUB_CHANNEL, assignmentCode));
//        schemeOutletObject.put(OUTLET_CODE, getFieldValue(fieldName, OUTLET_CODE, assignmentCode));
//        schemeOutletObject.put(CHANNEL, getFieldValue(fieldName, CHANNEL, assignmentCode));
//
//        setDefaultOutletValues(schemeOutletObject);
//
//        return schemeOutletObject;
//    }
//
//    private void addOnlyOutletAssignmentsUnderDistributor(Set<String> distributorSet, List<JsonNode> outletCodeNodes, ArrayNode outletListNode) {
//        outletCodeNodes.forEach(assignment -> {
//            String assignmentType = assignment.get(ASS_TYPE).asText();
//            if (assignmentType.equalsIgnoreCase("C")){
//                String outletCode = assignment.get(ASS_CD).asText();
//
//                String outletDistributor = assignment.get(PARENT_CD).asText();
//
//                if(distributorSet.contains(outletDistributor)) {
//                    ObjectNode schemeDistOutletObj = createSchemeDistOutletObject(outletDistributor, outletCode, assignment.get(PROMO_CODE).asText());
//                    outletListNode.add(schemeDistOutletObj);
//                }
//            }
//        });
//    }
//
//    private String getTradeName(String assignmentCode) {
//        String cc3;
//        cc3 = assignmentCode;
//        List<GenericEntity> cc3map = repository.findByNameAndKey1AndKey2("Outletcategory", "3", cc3);
//        if (!cc3map.isEmpty()) {
//            return cc3map.get(0).getKey3();
//        }
//
//        return null;
//    }
//
//    private String determineOutletFieldName(JsonNode assignDetails) {
//        String custhierLevel = assignDetails.get("CUSTHIER_LEVEL").asText();
//
//        if (custhierLevel.equalsIgnoreCase("A1")) {
//            return CHANNEL;
//        } else if (custhierLevel.equalsIgnoreCase("A2")) {
//            return TRADENAME;
//        } else if (custhierLevel.equalsIgnoreCase("A3")) {
//            return SUB_CHANNEL;
//        }
//
//        return "";
//    }
//
//    private void setDefaultOutletValues(ObjectNode schemeOutletObject) {
//        schemeOutletObject.put("outletDivision", "all");
//        schemeOutletObject.put("outletCategory", "all");
//
//        //market Id = Y -> New DMS
//        schemeOutletObject.put("marketId", "y");
//
//        schemeOutletObject.put("account", "all");
//        schemeOutletObject.put("outletType", "all");
//        schemeOutletObject.put("priceListId", "all");
//        schemeOutletObject.put("soldTo", "all");
//        schemeOutletObject.put("subTerritory", "all");
//    }
//
//    private ArrayNode schemeProductTransformer(JsonNode schemeMap, String promoType) {
//        ArrayNode productListNode = JsonNodeFactory.instance.arrayNode();
//        if (promoType.equalsIgnoreCase("5")) {
//            assignPwpProducts(productListNode, schemeMap);
//        }
//
//        addProductBifurcations(productListNode, schemeMap);
//
//        Set<JsonNode> uniqueSet = new HashSet<>();
//        ArrayNode uniqueList = JsonNodeFactory.instance.arrayNode();
//
//        for (JsonNode node : productListNode) {
//            if (uniqueSet.add(node)) {
//                uniqueList.add(node);
//            }
//        }
//
//        return uniqueList;
//    }
//
//    private void assignPwpProducts(ArrayNode productListNode, JsonNode schemeMap) {
//        if (schemeMap.get(MST_PROMOPWP).isObject()) {
//            JsonNode productDetail = schemeMap.get(MST_PROMOPWP);
//            addPwpProducts(productListNode, schemeMap.get(PROMO_CODE), productDetail);
//        } else {
//            JsonNode pwpPromoProducts = schemeMap.get(MST_PROMOPWP);
//            pwpPromoProducts.forEach(productDetail ->
//                    addPwpProducts(productListNode, schemeMap.get(PROMO_CODE), productDetail));
//        }
//    }
//
//    private void addProductBifurcations(ArrayNode productListNode, JsonNode schemeMap) {
//        if (schemeMap.get(MST_PROMOPRD).isObject()) {
//            JsonNode productDetail = schemeMap.get(MST_PROMOPRD);
//            addProducts(productListNode, schemeMap.get(PROMO_CODE), productDetail);
//        } else {
//            JsonNode productDetails = schemeMap.get(MST_PROMOPRD);
//            productDetails.forEach(productDetail ->
//                    addProducts(productListNode, schemeMap.get(PROMO_CODE), productDetail));
//        }
//    }
//
//    private void addPwpProducts(ArrayNode productListNode, JsonNode promoCode, JsonNode productDetails) {
//        ObjectNode productNode = createPwpProductNode(promoCode, productDetails);
//        productListNode.add(productNode);
//    }
//
//    private void addProducts(ArrayNode productListNode, JsonNode promoCode, JsonNode productDetails) {
//        ObjectNode productNode = createProductNode(promoCode, productDetails);
//        productListNode.add(productNode);
//    }
//
//    private ObjectNode createPwpProductNode(JsonNode promoCode, JsonNode productDetails) {
//        ObjectNode productNode = JsonNodeFactory.instance.objectNode();
//
//        productNode.put(SCHEME_ID, promoCode.asText());
//        productNode.put(BATCH_CODE, productDetails.get(PRDCAT_LEVEL).asText().equalsIgnoreCase("8") ? productDetails.get("PWP_PRDCAT_CD").asText() : "all");
//        productNode.put(PIECE_SIZE, getPackSizeValue(productDetails)); //PACK_SIZE
//        productNode.put(BRAND_NAME, getBrandValue(productDetails)); //BRAND
//        productNode.put(CATEGORY, getCategoryValue(productDetails)); //CATEGORY
//        productNode.put(FLAVOUR, getFlavorValue(productDetails)); //FLAVOR
//        productNode.put(ITEM_TYPE, getBeverageTypeValue(productDetails)); //BEVERAGE_TYPE
//        productNode.put(PIECE_SIZE_DESC, getPackTypeValue(productDetails)); //PACK_TYPE
//        productNode.put("ctg", getSalesUnitValue(productDetails)); //SALES_UNIT
//        productNode.put("purchaseUnit", "all");
//        productNode.put(ITEM_CLASS, "all");
//        productNode.put(ITEM_ID, "all");
//        productNode.put(SUB_CATEGORY, "all");
//        productNode.put("marketSku", "all");
//        productNode.put("size", "all");
//        productNode.put("subCategoryCode", "all");
//        productNode.put("customGroupCode", "all");
//
//        return productNode;
//    }
//
//    private ObjectNode createProductNode(JsonNode promoCode, JsonNode productDetails) {
//        ObjectNode productNode = JsonNodeFactory.instance.objectNode();
//
//        productNode.put(SCHEME_ID, promoCode.asText());
//
//        productNode.put(BATCH_CODE, productDetails.get(PRDCAT_LEVEL).asText().equalsIgnoreCase("8") ? productDetails.get("PRDCAT_CD").asText() : "all");
//
//        productNode.put(PIECE_SIZE, getPackSizeValue(productDetails)); //PACK_SIZE
//        productNode.put(BRAND_NAME, getBrandValue(productDetails)); //BRAND
//        productNode.put(CATEGORY, getCategoryValue(productDetails)); //CATEGORY
//        productNode.put(FLAVOUR, getFlavorValue(productDetails)); //FLAVOR
//        productNode.put(ITEM_TYPE, getBeverageTypeValue(productDetails)); //BEVERAGE_TYPE
//        productNode.put(PIECE_SIZE_DESC, getPackTypeValue(productDetails)); //PACK_TYPE
//        productNode.put("ctg", getSalesUnitValue(productDetails)); //SALES_UNIT
//        productNode.put("purchaseUnit", "all");
//        productNode.put(ITEM_CLASS, "all");
//        productNode.put(ITEM_ID, "all");
//        productNode.put(SUB_CATEGORY, "all");
//        productNode.put("marketSku", "all");
//        productNode.put("size", "all");
//        productNode.put("subCategoryCode", "all");
//        productNode.put("customGroupCode", "all");
//
//        return productNode;
//    }
//
//
//    private ArrayNode freeProductTransformer(JsonNode schemeMap, JsonNode promoCode) {
//        ArrayNode productListNode = JsonNodeFactory.instance.arrayNode();
//
//        if (schemeMap.get(MST_PROMOFOC).isObject()) {
//            JsonNode productDetails = schemeMap.get(MST_PROMOFOC);
//
//            String productHierarchyLevel = productDetails.get("FOC_PRDCAT_LEVEL").asText();
//
//            if (productHierarchyLevel.equalsIgnoreCase("8")) {
//                addDirectFocProduct(productListNode, promoCode, productDetails);
//            } else {
//                fetchAndAddFocProducts(productListNode, promoCode, productDetails);
//            }
//        } else {
//            JsonNode focPromoProducts = schemeMap.get(MST_PROMOFOC);
//
//            focPromoProducts.forEach(productDetails -> {
//                String productHierarchyLevel = productDetails.get("FOC_PRDCAT_LEVEL").asText();
//
//                if (productHierarchyLevel.equalsIgnoreCase("8")) {
//                    addDirectFocProduct(productListNode, promoCode, productDetails);
//                } else {
//                    fetchAndAddFocProducts(productListNode, promoCode, productDetails);
//                }
//            });
//        }
//
//        if (productListNode.isEmpty())
//            throw new TransformationException("No products available for these filters in productdetails for FOC");
//
//        return productListNode;
//    }
//
//    private void addDirectFocProduct(ArrayNode productListNode, JsonNode promoCode, JsonNode productDetails) {
//        ObjectNode productNode = JsonNodeFactory.instance.objectNode();
//
//        productNode.set(SCHEME_ID, promoCode);
//        productNode.set("freeProductUOM", productDetails.get("FOC_UOM_CD"));
//
//        String batchCode = productDetails.get("FOC_PRDCAT_CD").asText();
//        String trimmedBatchCode = batchCode.replaceFirst("^0+(?!$)", "");
//        productNode.put(BATCH_CODE, batchCode.isEmpty() ? "all" : trimmedBatchCode);
//        productNode.set(EXTENDED_ATTRIBUTES, getFreeProductExtendedAttributes(productDetails));
//        productNode.set("qty", productDetails.get(PROMO_INDEX));
//
//        productListNode.add(productNode);
//    }
//
//    private void fetchAndAddFocProducts(ArrayNode productListNode, JsonNode promoCode, JsonNode productDetails) {
//        List<Map<String, Object>> queryResult = executeQueryForProductDetails(productDetails);
//
//        if (queryResult.isEmpty()) return;
//
//        queryResult.forEach(row -> {
//            ObjectNode productNode = JsonNodeFactory.instance.objectNode();
//
//            productNode.set(SCHEME_ID, promoCode);
//            productNode.set("freeProductUOM", productDetails.get("FOC_UOM_CD"));
//
//            String batchCode = row.get(BATCHCODE).toString();
//            productNode.put(BATCH_CODE, batchCode.isEmpty() ? "all" : batchCode);
//            productNode.set(EXTENDED_ATTRIBUTES, getFreeProductExtendedAttributes(productDetails));
//            productNode.set("qty", productDetails.get(PROMO_INDEX));
//
//            productListNode.add(productNode);
//        });
//    }
//
//    private ArrayNode schemeMustBuyTransformer(JsonNode schemeMap) {
//
//        Map<String, String> groupMap = makeGroupMap(schemeMap);
//        ArrayNode productListNode = JsonNodeFactory.instance.arrayNode();
//
//        if(schemeMap.get(MST_PROMOPRD).isObject()){
//            JsonNode prodAssign = schemeMap.get(MST_PROMOPRD);
//            String minQtyVal = isMustBuy(prodAssign, groupMap);
//            addMustBuyEntries(minQtyVal, schemeMap, productListNode, prodAssign);
//
//        }else{
//            JsonNode prodAssignArray = schemeMap.get(MST_PROMOPRD);
//            prodAssignArray.forEach(prodAssign -> {
//                String minQtyVal = isMustBuy(prodAssign, groupMap);
//                addMustBuyEntries(minQtyVal, schemeMap, productListNode, prodAssign);
//            });
//        }
//
//        return productListNode;
//    }
//
//
//    private Map<String, String> makeGroupMap(JsonNode schemeMap){
//        Map<String, String> groupMap = new HashMap<>();
//        if(!schemeMap.has(MST_PROMOPRD_GRP)) {
//            return groupMap;
//        }
//        JsonNode groupNode = schemeMap.get(MST_PROMOPRD_GRP);
//        if (groupNode.isObject()) {
//            processGroupNode(groupNode, groupMap);
//        } else if (groupNode.isArray()) {
//            groupNode.forEach(node -> processGroupNode(node, groupMap));
//        }
//
//        return groupMap;
//    }
//
//    private void processGroupNode(JsonNode node, Map<String, String> groupMap) {
//        if (node.has(GRP_CD) && node.has(MUST_IND) && node.has(MIN_QTY)) {
//            String groupCd = node.get(GRP_CD).asText();
//            String mustInd = node.get(MUST_IND).asText();
//            String minQty = node.get(MIN_QTY).asText();
//
//            if (mustInd.equalsIgnoreCase("1")) {
//                groupMap.put(groupCd, minQty);
//            }
//        }
//    }
//
//    private String isMustBuy(JsonNode prodAssign, Map<String, String> groupMap) {
//        if(prodAssign.get(GRP_CD).asText().equalsIgnoreCase("%")){
//            if(prodAssign.get(MUST_IND).asText().equalsIgnoreCase("1")) return prodAssign.get(MIN_QTY).asText();
//            else return "-1";
//        }else {
//            String groupCd = prodAssign.get(GRP_CD).asText();
//            return groupMap.getOrDefault(groupCd, "-1");
//        }
//    }
//    private void addMustBuyEntries(String minQtyVal, JsonNode schemeMap, ArrayNode productListNode, JsonNode prodAssign) {
//        if (!minQtyVal.equalsIgnoreCase("-1")) {
//            if(schemeMap.has(MST_PROMOPRD_GRP) && !prodAssign.get(PRDCAT_LEVEL).asText().equalsIgnoreCase("8")) {
//                addMustBuyProductsFromQuery(productListNode, schemeMap, prodAssign, minQtyVal);
//            }else
//            {
//                addMustBuyProducts(productListNode, schemeMap, prodAssign, minQtyVal);
//            }
//        }
//    }
//
//    private void addMustBuyProductsFromQuery(ArrayNode productListNode, JsonNode schemeMap, JsonNode prodAssign, String minQtyVal) {
//        List<Map<String, Object>> queryResult = executeQueryForProductDetails(prodAssign);
//        if (queryResult.isEmpty()) return;
//        queryResult.forEach(row -> {
//            ObjectNode productNode = JsonNodeFactory.instance.objectNode();
//            productNode.set("mustByGroupId", prodAssign.get(PROMO_CODE));
//            productNode.put(SCHEME_ID, schemeMap.get(PROMO_CODE).asText());
//            productNode.put("qty", minQtyVal);
//            productNode.put("unit", "CS");
//            String batchCode = row.get(BATCHCODE).toString();
//            productNode.put(BATCH_CODE, batchCode.isEmpty() ? "all" : batchCode);
//            productNode.set(MUST_BUY_GROUP_CONDITION, prodAssign.get(GRP_CD));
//
//            productListNode.add(productNode);
//        });
//
//    }
//    private void addMustBuyProducts(ArrayNode productListNode, JsonNode schemeMap, JsonNode prodAssign, String minQtyVal) {
//        ObjectNode productNode = JsonNodeFactory.instance.objectNode();
//        productNode.set("mustByGroupId", prodAssign.get(PROMO_CODE));
//        productNode.put(SCHEME_ID, schemeMap.get(PROMO_CODE).asText());
//        productNode.put(BATCH_CODE, prodAssign.get(PRDCAT_LEVEL).asText().equalsIgnoreCase("8") ? prodAssign.get("PRDCAT_CD").asText() : "all");
//        productNode.put("qty", minQtyVal);
//        productNode.put("unit", "CS");
//        productNode.put(PIECE_SIZE, getPackSizeValue(prodAssign)); //PACK_SIZE
//        productNode.put(BRAND_NAME, getBrandValue(prodAssign)); //BRAND
//        productNode.put(CATEGORY, getCategoryValue(prodAssign)); //CATEGORY
//        productNode.put(FLAVOUR, getFlavorValue(prodAssign)); //FLAVOR
//        productNode.put(ITEM_TYPE, getBeverageTypeValue(prodAssign)); //BEVERAGE_TYPE
//        productNode.put(PIECE_SIZE_DESC, getPackTypeValue(prodAssign)); //PACK_TYPE
//        productNode.put("ctg", getSalesUnitValue(prodAssign)); //SALES_UNIT
//        productNode.put(ITEM_CLASS, "all");
//        productNode.put(ITEM_ID, "all");
//        productNode.put(SUB_CATEGORY, "all");
//        if(prodAssign.has(GRP_CD) && !prodAssign.get(GRP_CD).isEmpty() && !prodAssign.get(GRP_CD).asText().equalsIgnoreCase("%")) {
//            productNode.set(MUST_BUY_GROUP_CONDITION, prodAssign.get(GRP_CD));
//        }
//        productListNode.add(productNode);
//    }
//
//    private List<Map<String, Object>> executeQueryForProductDetails(JsonNode productDetails) {
//        StringBuilder query = new StringBuilder("select batch_code from ck_productdetails where 1=1");
//
//        if (productDetails.has(PACK_SIZE) && !productDetails.get(PACK_SIZE).asText().isEmpty()) {
//            query.append(" and piece_size = '").append(getPackSizeValue(productDetails)).append("'");
//        }
//
//        if (productDetails.has(PACK_TYPE) && !productDetails.get(PACK_TYPE).asText().isEmpty()) {
//            query.append(" and piece_size_desc = '").append(getPackTypeValue(productDetails)).append("'");
//        }
//
//        if (productDetails.has(BEVERAGE_TYPE) && !productDetails.get(BEVERAGE_TYPE).asText().isEmpty()) {
//            query.append(" and item_type = '").append(getBeverageTypeValue(productDetails)).append("'");
//        }
//
//        if (productDetails.has(BVGCATEGORY) && !productDetails.get(BVGCATEGORY).asText().isEmpty()) {
//            query.append(" and category = '").append(getCategoryValue(productDetails)).append("'");
//        }
//
//        if (productDetails.has(BRAND) && !productDetails.get(BRAND).asText().isEmpty()) {
//            query.append(" and brand = '").append(getBrandValue(productDetails)).append("'");
//        }
//
//        if (productDetails.has(FLAVOR) && !productDetails.get(FLAVOR).asText().isEmpty()) {
//            query.append(" and flavour = '").append(getFlavorValue(productDetails)).append("'");
//        }
//
//        if (productDetails.has(SALES_UNIT) && !productDetails.get(SALES_UNIT).asText().isEmpty()) {
//            query.append(" and ctg = '").append(getSalesUnitValue(productDetails)).append("'");
//        }
//
//        query.append(";");
//
//        JdbcTemplate jdbcTemplate = JdbcUtils.createJdbcTemplate((DataSource) DatabaseProfileRegistry.getDataSourceHashMap().get(SecurityContextUtils.getLob()));
//        return jdbcTemplate.queryForList(query.toString());
//    }
//
//    private String getSchemeType(String promoType) {
//        if(promoType.equalsIgnoreCase("2") || promoType.equalsIgnoreCase("5")) {
//            return "value";
//        } else{
//            return "item";
//        }
//    }
//
//    private JsonNode getDefinitionExtendedAttributes(JsonNode schemeMap, String promoType) {
//        JsonNode includedBatchCodes;
//
//        ObjectNode extended = JSONUtils.getObjectMapper().createObjectNode();
//
//        if(schemeMap.has(MST_PROMOPRD_GRP)) {
//            extended.put("mustBuyWithAndCondition", 1);
//        }
//
//        if(promoType.equalsIgnoreCase("5")) {
//            includedBatchCodes = getPWPBatchCodes(schemeMap);
//            extended.set("batchCodesToInclude", includedBatchCodes);
//        }
//
//        return JSONUtils.toJsonNode(extended);
//    }
//
//    private JsonNode getCalculationExtendedAttributes(JsonNode schemeMap, ArrayNode promoDetails) {
//        ObjectNode extended = JSONUtils.getObjectMapper().createObjectNode();
//        if(promoDetails.get(0).get(APPLY_ON).asText().equalsIgnoreCase("2")){
//            extended.put("perProduct", "2");
//        }
//        if(schemeMap.has(PROMO_PROGRESS) && schemeMap.get(PROMO_PROGRESS).asText().equalsIgnoreCase("1")){
//            extended.put("exclusionOption", "0");
//        }
//
//        if(schemeMap.get("MAX_COUNT_FLAG").asText().equalsIgnoreCase("1")) {
//            extended.put("isbalance", "true");
//        }
//
//        return JSONUtils.toJsonNode(extended);
//    }
//
//    private JsonNode getFreeProductExtendedAttributes(JsonNode promoDetails) {
//        ObjectNode extended = JSONUtils.getObjectMapper().createObjectNode();
//        extended.set("promoIndex", promoDetails.get(PROMO_INDEX));
//
//        return JSONUtils.toJsonNode(extended);
//    }
//
//
//
//    private JsonNode getPWPBatchCodes(JsonNode schemeMap) {
//        StringBuilder pwpBatchCodes = new StringBuilder();
//
//        JsonNode promoProducts = schemeMap.get(MST_PROMOPWP);
//
//        if (promoProducts.isObject()) {
//            processProductDetails(promoProducts, pwpBatchCodes);
//        } else {
//            promoProducts.forEach(productDetails -> processProductDetails(productDetails, pwpBatchCodes));
//        }
//
//        return JSONUtils.toJsonNode(pwpBatchCodes.toString());
//    }
//
//    private void processProductDetails(JsonNode productDetails, StringBuilder pwpBatchCodes) {
//        String productHierarchyLevel = productDetails.get(PRDCAT_LEVEL).asText();
//
//        if (isLowestLevelHierarchy(productHierarchyLevel)) {
//            appendBatchCode(pwpBatchCodes, productDetails.get("PWP_PRDCAT_CD").asText());
//        } else {
//            List<Map<String, Object>> queryResult = executeQueryForProductDetails(productDetails);
//            appendQueryResultBatchCodes(pwpBatchCodes, queryResult);
//        }
//    }
//
//    private boolean isLowestLevelHierarchy(String productHierarchyLevel) {
//        return productHierarchyLevel.equalsIgnoreCase("8");
//    }
//
//    private void appendBatchCode(StringBuilder pwpBatchCodes, String batchCode) {
//        if (pwpBatchCodes.length() > 0) {
//            pwpBatchCodes.append(",");
//        }
//        pwpBatchCodes.append(batchCode);
//    }
//
//    private void appendQueryResultBatchCodes(StringBuilder pwpBatchCodes, List<Map<String, Object>> queryResult) {
//        pwpBatchCodes.append(queryResult.stream()
//                                        .map(row -> row.get(BATCHCODE).toString())
//                                        .collect(Collectors.joining(",")));
//    }
//
//
//    //MST_PROMODTL is a single object or Array? can be both (assuming)
//    private ArrayNode getSlabIfAlreadyExist(JsonNode schemeMap, ArrayNode promoDetails, ObjectNode finalObject, String promoType) {
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        ArrayNode slabArray = (ArrayNode) finalObject.get(SLAB_INFO);
//
//        // Initializing slabArray if it doesn't exist
//        if (NullUtils.isNull(slabArray)) {
//            slabArray = objectMapper.createArrayNode();
//        }
//
//        int slabCount = promoDetails.size();
//
//        for (int idx = 0; idx < slabCount; idx++) {
//            JsonNode currentDetail = promoDetails.get(idx);
//            ObjectNode slabNode = JsonNodeFactory.instance.objectNode();
//
//            slabNode.put("promoIndex", currentDetail.get(PROMO_INDEX).asText());
//            slabNode.put("startRange", currentDetail.get(TTLBUY_QTY).asText());
//
//            if(promoType.equalsIgnoreCase("1")){
//                slabNode.set(SCHEME_BENEFIT, currentDetail.get(FOC_QTY));
//            }else if(promoType.equalsIgnoreCase("5")){
//                if(schemeMap.get(MST_PROMOPWP).isObject()){
//                    JsonNode pwpDetail = schemeMap.get(MST_PROMOPWP);
//                    slabNode.set(SCHEME_BENEFIT, pwpDetail.get("PWP_DISC"));
//                }else{
//                    JsonNode pwpDetail = schemeMap.get(MST_PROMOPWP).get(idx);
//                    slabNode.set(SCHEME_BENEFIT, pwpDetail.get("PWP_DISC"));
//                }
//            }else{
//                slabNode.put(SCHEME_BENEFIT, currentDetail.get("FACTOR_VALUE").asText());
//            }
//
//            slabNode.put(SCHEME_DESCRIPTION, schemeMap.get(PROMO_DESC).asText());
//            slabNode.put(SCHEME_FACTOR, getSchemeFactor(schemeMap,currentDetail));
//
//            if (idx < slabCount - 1) {
//                int nextStartRange = promoDetails.get(idx + 1).get(TTLBUY_QTY).asInt();
//                slabNode.put("endRange", String.valueOf(nextStartRange - 1));
//            } else {
//                slabNode.put("endRange", "99999999.0");
//            }
//            ObjectNode deepCopy = SerializationUtils.clone(slabNode);
//            slabArray.add(deepCopy);
//
//        }
//        return slabArray;
//    }
//
//    private String getSchemeFactor(JsonNode schemeMap,JsonNode currentDetail){
//
//        if(schemeMap.has(FOR_EVERY_FLAG) && schemeMap.get(FOR_EVERY_FLAG).asText().equalsIgnoreCase("1")){
//            int forEveryValue = safeParseInt(currentDetail.get(FOR_EVERY).asText());
//            return String.valueOf(forEveryValue);
//        }
//
//        Map<String, String> groupMap = makeGroupMap(schemeMap);
//        AtomicReference<Integer> totalMustBuyQty = new AtomicReference<>(0);
//
//        if(schemeMap.get(MST_PROMOPRD).isObject()){
//            JsonNode prodAssign = schemeMap.get(MST_PROMOPRD);
//            String minQtyVal = isMustBuy(prodAssign, groupMap);
//            totalMustBuyQty.updateAndGet(v -> v + safeParseInt(minQtyVal));
//        }else{
//            if(!groupMap.isEmpty()){
//                for (Map.Entry<String, String> entry : groupMap.entrySet()) {
//                    String value = entry.getValue();
//                    totalMustBuyQty.updateAndGet(v -> v + safeParseInt(value));
//                }
//            }else{
//                JsonNode prodAssignArray = schemeMap.get(MST_PROMOPRD);
//                prodAssignArray.forEach(prodAssign -> {
//                    String minQtyVal = isMustBuy(prodAssign, groupMap);
//                    if(!minQtyVal.equalsIgnoreCase("-1")){
//                        totalMustBuyQty.updateAndGet(v -> v + safeParseInt(minQtyVal));
//                    }
//                });
//            }
//        }
//        if(totalMustBuyQty.get() == 0) return "1";
//        return Integer.toString(totalMustBuyQty.get());
//    }
//
//    private int safeParseInt(String value) {
//        try {
//            return Integer.parseInt(value);
//        } catch (NumberFormatException e) {
//            try {
//                return (int) Math.floor(Double.parseDouble(value));
//            } catch (NumberFormatException ex) {
//                return 1;
//            }
//        }
//    }
//
//    private String getItemEach(JsonNode schemeMap, ArrayNode promoDetails){
//        if(schemeMap.get("TTLBUY_TYPE").asText().equalsIgnoreCase("M")){
//            if(promoDetails.get(0).get(APPLY_ON).asText().equals("1")){
//                if(schemeMap.has(PROMO_PROGRESS) && schemeMap.get(PROMO_PROGRESS).asText().equalsIgnoreCase("1")){
//                    return "15";
//                }else if(schemeMap.has(SCHEME_PRORATA) && schemeMap.get(SCHEME_PRORATA).asText().equalsIgnoreCase("1")){
//                    return "1";
//                }
//                if(schemeMap.has(FOR_EVERY_FLAG) && NullUtils.isNotNull(schemeMap.get(FOR_EVERY_FLAG)) && schemeMap.get(FOR_EVERY_FLAG).asText().equalsIgnoreCase("1")){
//                    return "19";
//                }
//                return "1";
//            }
//            else if(promoDetails.get(0).get(APPLY_ON).asText().equals("3")) {
//                if(schemeMap.has(SCHEME_PRORATA) && schemeMap.get(SCHEME_PRORATA).asText().equalsIgnoreCase("1")){
//                    return "18";
//                }
//                else if(schemeMap.has(FOR_EVERY_FLAG) && NullUtils.isNotNull(schemeMap.get(FOR_EVERY_FLAG)) && schemeMap.get(FOR_EVERY_FLAG).asText().equalsIgnoreCase("1")){
//                    return "7";
//                }
//                return "2";
//            }
//        }
//        return "4";
//    }
//
//    private String getFieldValue(String fieldName, String targetField, String value) {
//        return fieldName.equalsIgnoreCase(targetField) && !value.isEmpty() ? value : "all";
//    }
//
//    private String getPackSizeValue(JsonNode productDetails){
//        //cc3 pack size
//        Pattern pattern = Pattern.compile("-?(\\d+(\\.\\d+))|(\\.\\d)|(\\d+)?");
//        String piecesize = productDetails.get(PACK_SIZE).asText();
//        if (!ObjectUtils.isEmpty(piecesize)) {
//            List<GenericEntity> piecesizeMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "3", piecesize);
//            if (!piecesizeMapping.isEmpty()) {
//                String cc3piecesize = piecesizeMapping.get(0).getKey3();
//                String[] parts = cc3piecesize.split(" ");
//                if(parts.length ==2 && pattern.matcher(parts[0]).matches()) {
//                    double numericValue = Double.parseDouble(parts[0]);
//                    String unit = parts[1];
//                    String value = String.valueOf(unitConversionMap.get(unit)* numericValue);
//                    return value.split("\\.")[0];
//                }else{
//                    return cc3piecesize;
//                }
//            }
//        }
//        return "all";
//    }
//
//    private String getBrandValue(JsonNode productDetails){
//        String brand = productDetails.get(BRAND).asText();
//        if (!ObjectUtils.isEmpty(brand)) {
//            List<GenericEntity> brandMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "1", brand);
//            if (!brandMapping.isEmpty()) {
//                return brandMapping.get(0).getKey3();
//            }
//        }
//
//        return "all";
//    }
//
//    private String getCategoryValue(JsonNode productDetails){
//        String category = productDetails.get(BVGCATEGORY).asText();
//        if (!ObjectUtils.isEmpty(category)) {
//            List<GenericEntity> categoryMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "6", category);
//            if (!categoryMapping.isEmpty()) {
//                return categoryMapping.get(0).getKey3();
//            }
//        }
//        return "all";
//    }
//
//    private String getFlavorValue(JsonNode productDetails){
//        String flavour = productDetails.get(FLAVOR).asText();
//        if (!ObjectUtils.isEmpty(flavour)) {
//            List<GenericEntity> flavourMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "10", flavour);
//            if (!flavourMapping.isEmpty()) {
//                return flavourMapping.get(0).getKey3();
//            }
//        }
//        return "all";
//    }
//
//    private String getBeverageTypeValue(JsonNode productDetails){
//        String itemtype = productDetails.get(BEVERAGE_TYPE).asText();
//        if (!ObjectUtils.isEmpty(itemtype)) {
//            List<GenericEntity> itemtypeMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "4", itemtype);
//            if (!itemtypeMapping.isEmpty()) {
//                return itemtypeMapping.get(0).getKey3();
//            }
//        }
//        return "all";
//    }
//
//    private String getPackTypeValue(JsonNode productDetails){
//        String piecesizedesc = productDetails.get(PACK_TYPE).asText();
//        if (!ObjectUtils.isEmpty(piecesizedesc)) {
//            List<GenericEntity> piecesizedescMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "11", piecesizedesc);
//            if (!piecesizedescMapping.isEmpty()) {
//                return piecesizedescMapping.get(0).getKey3();
//            }
//        }
//        return "all";
//    }
//
//    private String getSalesUnitValue(JsonNode productDetails){
//        String cc2 = productDetails.get(SALES_UNIT).asText();
//        List<GenericEntity> cc2map = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "2", cc2);
//        if(!cc2map.isEmpty()) {
//            return cc2map.get(0).getKey3();
//        }
//        return "all";
//    }
//
//    private String getOutletClass(JsonNode assignment){
//        String class1 = assignment.get(ASS_CD).asText();
//        if (!ObjectUtils.isEmpty(class1)) {
//            List<GenericEntity> classMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "11", class1);
//            if (!classMapping.isEmpty()) {
//                return classMapping.get(0).getKey3();
//            }
//        }
//        return "all";
//    }
//
//    private void checkCondition(JsonNode schemeMap) {
//        List<String> errors = new ArrayList<>();
//
//        // Validate required nodes
//        validateRequiredNodes(schemeMap, errors);
//
//        // Validate MST_PROMODTL node if present
//        if (schemeMap.has(MST_PROMODTL)) {
//            JsonNode promoDtlNode = schemeMap.get(MST_PROMODTL);
//
//            if (promoDtlNode.isObject()) {
//                validatePromoDetailsObject(promoDtlNode, errors);
//            } else if (promoDtlNode.isArray() && !promoDtlNode.isEmpty()) {
//                validatePromoDetailsArray(promoDtlNode, errors);
//            }
//        }
//
//        if(schemeMap.has(MST_PROMOASSIGN)){
//            checkDistributorCode(schemeMap, errors);
//        }
//
//        handleErrors(errors);
//    }
//
//    private void validateRequiredNodes(JsonNode schemeMap, List<String> errors) {
//        if(!schemeMap.has(START_DT) || schemeMap.get(START_DT).asText().equalsIgnoreCase("1900-01-01T00:00:00")){
//            errors.add(ERROR_MSG + START_DT);
//        }
//
//        if(!schemeMap.has(END_DT) || schemeMap.get(END_DT).asText().equalsIgnoreCase("1900-01-01T00:00:00")){
//            errors.add(ERROR_MSG + END_DT);
//        }
//
//        if (!schemeMap.has(MST_PROMODTL)) {
//            errors.add(ERROR_MSG + MST_PROMODTL);
//        }
//
//        if (!schemeMap.has(MST_PROMOPRD)) {
//            errors.add(ERROR_MSG + MST_PROMOPRD);
//        }
//
//        if (!schemeMap.has(MST_PROMOASSIGN)) {
//            errors.add(ERROR_MSG + MST_PROMOASSIGN);
//        }
//    }
//
//    private void validatePromoDetailsObject(JsonNode promoDtlNode, List<String> errors) {
//        validateTtlBuyQty(promoDtlNode, errors);
//        validateMechanicType(promoDtlNode, errors);
//    }
//
//    private void validatePromoDetailsArray(JsonNode promoDtlNode, List<String> errors) {
//        JsonNode firstElement = promoDtlNode.get(0);
//        validateTtlBuyQty(firstElement, errors);
//        validateMechanicType(firstElement, errors);
//    }
//
//    private void validateTtlBuyQty(JsonNode promoDtlNode, List<String> errors) {
//        if (promoDtlNode.has(TTLBUY_QTY) && (promoDtlNode.get(TTLBUY_QTY).isNull() || promoDtlNode.get(TTLBUY_QTY).asText().trim().isEmpty())) {
//            errors.add("Request body doesn't have TTLBUY_QTY");
//        }
//    }
//
//    private void validateMechanicType(JsonNode promoDtlNode, List<String> errors) {
//        String mechanicType = promoDtlNode.has(MECHANIC_TYPE) ? promoDtlNode.get(MECHANIC_TYPE).asText().trim() : "";
//
//        if (!mechanicType.equalsIgnoreCase("1") && !mechanicType.equalsIgnoreCase("2") && !mechanicType.equalsIgnoreCase("5")) {
//            errors.add(MECHANIC_TYPE + " has wrong value");
//        } else if (mechanicType.equalsIgnoreCase("1") && (promoDtlNode.has(FOC_QTY) && (promoDtlNode.get(FOC_QTY).isNull() || promoDtlNode.get(FOC_QTY).asText().trim().isEmpty()))) {
//            errors.add("Request body doesn't have FOC_QTY");
//        }
//    }
//
//    private void checkDistributorCode(JsonNode schemeMap, List<String> errors){
//        AtomicBoolean flag1 = new AtomicBoolean(false);
//
//        JsonNode promoAssignments = schemeMap.get(MST_PROMOASSIGN);
//
//        promoAssignments.forEach(assignment -> {
//            if(assignment.has(ASS_TYPE) && assignment.get(ASS_TYPE).asText().equalsIgnoreCase("D")){
//                flag1.set(true);
//            }
//        });
//
//        if(!flag1.get()) errors.add("Request body doesn't have ASS_TYPE or ASS_TYPE = 'D' not given");
//    }
//
//    private void handleErrors(List<String> errors) {
//        if (!errors.isEmpty()) {
//            String errorString = com.applicate.services.channelkart.utils.StringUtils.format(
//                    "Error while validating Request body: {}",
//                    org.apache.commons.lang.StringUtils.join(errors, ",")
//            );
//            throw new TransformationException(errorString);
//        }
//    }
//
//
//}
//
//
