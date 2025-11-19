//package com.applicate.simamy.transformer;
//
//import com.applicate.services.channelkart.models.GenericEntity;
//import com.applicate.services.channelkart.repository.GenericEntityRepository;
//import com.applicate.services.channelkart.services.SpringContext;
//import com.applicate.services.channelkart.transformers.AbstractTransformer;
//import com.applicate.services.channelkart.utils.JSONUtils;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.apache.commons.lang3.ObjectUtils;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class SimaExclusion extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
//    static final String MATERIAL_NUMBER = "MaterialNumber";
//    static final String CHANGE_INDICATOR = "ChangeIndicator";
//    static final String VALID_TO = "ValidTo";
//    static final String VALID_FROM = "ValidFrom";
//    static final String SUB_CHN = "ConsSubtrChnl";
//    static final String NULL = "null";
//    static final String TABLE = "Table";
//    static final String CUS_ID = "CustomerID";
//    static final String NAME = "PriceInclusionExclusion";
//    GenericEntityRepository genericEntityRepository = SpringContext.getBean(GenericEntityRepository.class);
//
//    @Override
//    public Object transform(Map<String, Object> cdm) {
//        Map<String, Object> exclusionList = new HashMap<>();
//        final String Id = ObjectUtils.isEmpty(cdm.get(SUB_CHN)) ? NULL : cdm.get(SUB_CHN).toString();
//        final String customerId = ObjectUtils.isEmpty(cdm.get(CUS_ID)) ? NULL : cdm.get(CUS_ID).toString();
//        List<GenericEntity> genericEntity = getExistingDetails(cdm,Id, customerId);
//        final String changeIndicator = ObjectUtils.isEmpty(cdm.get(CHANGE_INDICATOR)) ? NULL : cdm.get(CHANGE_INDICATOR).toString();
//        final String materialNumber = ObjectUtils.isEmpty(cdm.get(MATERIAL_NUMBER)) ? NULL : cdm.get(MATERIAL_NUMBER).toString();
//        final String table = ObjectUtils.isEmpty(cdm.get(TABLE)) ? NULL : cdm.get(TABLE).toString();
//        final String validTo = ObjectUtils.isEmpty(cdm.get(VALID_TO)) ? NULL : cdm.get(VALID_TO).toString();
//        final String validFrom = ObjectUtils.isEmpty(cdm.get(VALID_FROM)) ? NULL : cdm.get(VALID_FROM).toString();
//        exclusionList.put("key5", changeIndicator);
//        exclusionList.put("key6", materialNumber);
//        ObjectNode materialValidity = new ObjectMapper().createObjectNode();
//        materialValidity.put(VALID_TO, validTo);
//        materialValidity.put(VALID_FROM, validFrom);
//        ObjectNode extended = (genericEntity.isEmpty() || genericEntity.get(0).getExtendedAttributes() == null) ? new ObjectMapper().createObjectNode() : JSONUtils.getObjectMapper().convertValue(genericEntity.get(0).getExtendedAttributes(), ObjectNode.class);
//        extended.put(materialNumber, materialNumber);
//        if (changeIndicator.equals("D") && !materialNumber.equals(NULL))
//            exclusionList.put("extendedAttributes", JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class));
//        if ((changeIndicator.equals("D") && !genericEntity.isEmpty())) {
//            GenericEntity generic = genericEntity.get(0);
//            generic.setExtendedAttributes(JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class));
//            generic.setKey2(validTo);
//            generic.setKey3(validFrom);
//            generic.setKey4(table);
//            generic.setKey5(changeIndicator);
//            generic.setKey6(materialNumber);
//            return generic;
//        }
//        setIdAndKey1(cdm, exclusionList, Id, customerId);
//        exclusionList.put("key4", cdm.get(TABLE));
//        ObjectNode payload = new ObjectMapper().createObjectNode();
//        if (changeIndicator.equals("I") && !materialNumber.equals(NULL))
//            payload.set(materialNumber, JSONUtils.getObjectMapper().convertValue(materialValidity, JsonNode.class));
//        JsonNode key2 = JSONUtils.getObjectMapper().convertValue(payload, JsonNode.class);
//        exclusionList.put("payload", key2);
//        exclusionList.put("key2", validTo);
//        exclusionList.put("key3", validFrom);
//        exclusionList.put("name", NAME);
//        return exclusionList;
//    }
//
//    private void setIdAndKey1(Map<String, Object> cdm, Map<String, Object> exclusionList, String id, String customerId) {
//        final String Id = "id";
//        final String key_1 = "key1";
//        if(ObjectUtils.isNotEmpty(cdm.get(TABLE))){
//            if(cdm.get(TABLE).toString().equals("924")){
//                exclusionList.put(key_1, customerId );
//                exclusionList.put(Id, customerId);
//            }else if(cdm.get(TABLE).toString().equals("929")){
//                exclusionList.put(key_1, id);
//                exclusionList.put(Id, id );
//            }
//        }
//    }
//    private List<GenericEntity> getExistingDetails(Map<String, Object> cdm, String id, String customerId) {
//        if(cdm.get(TABLE).toString().equals("924")){
//            return genericEntityRepository.findByNameAndKey1(NAME, customerId);
//        }else if(cdm.get(TABLE).toString().equals("929")){
//            return genericEntityRepository.findByNameAndKey1(NAME, id);
//        }
//        return new ArrayList<>();
//    }
//}
