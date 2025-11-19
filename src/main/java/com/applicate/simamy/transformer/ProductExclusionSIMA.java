package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductExclusionSIMA extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    static final String MATERIAL_NUMBER = "MaterialNumber";
    static final String CHANGE_INDICATOR = "ChangeIndicator";
    static final String VALID_TO = "ValidTo";
    static final String VALID_FROM = "ValidFrom";
    static final String SUB_CHN = "ConsSubtrChnl";
    static final String NULL = "null";
    static final String TABLE = "Table";
    static final String CUS_ID = "CustomerID";
    static final String NAME = "PriceInclusionExclusion";
    GenericEntityService genericEntityService = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);
    OutletDetailsService outletDetailsService = (OutletDetailsService)ServiceLocator.lookup(OutletDetails.class);

    @Override
    public Map<String, Object> transform(Map<String, Object> cdm) {
            List<Object> dataList = JSONUtils.getObjectMapper().convertValue(cdm.get("data_value"), List.class);
            if (dataList == null || dataList.isEmpty()) {
                throw new DataTransformationService.TransformationException("Data list is empty");
            }
            List<Map<String, Object>> filteredDataList = new ArrayList<>();
            for (Object data : dataList) {
                Map<String, Object> input = JSONUtils.getObjectMapper().convertValue(data, Map.class);
                filteredDataList.add(input);
            }
            Map<String,Object> input = JSONUtils.getObjectMapper().convertValue(dataList.get(0), Map.class);
        final String table = ObjectUtils.isEmpty(input.get(TABLE)) ? NULL : input.get(TABLE).toString();
        String customerId = ObjectUtils.isEmpty(input.get(CUS_ID)) ? NULL : input.get(CUS_ID).toString();

        String changeIndicator = ObjectUtils.isEmpty(input.get(CHANGE_INDICATOR)) ? NULL : input.get(CHANGE_INDICATOR).toString();
        final String materialNumber = ObjectUtils.isEmpty(input.get(MATERIAL_NUMBER)) ? NULL : input.get(MATERIAL_NUMBER).toString();
        if (customerId.equalsIgnoreCase(NULL) ||
                changeIndicator.equalsIgnoreCase(NULL) ||
                materialNumber.equalsIgnoreCase(NULL) ||
                table.equalsIgnoreCase(NULL)) {
            throw new DataTransformationService.TransformationException("Illegal Values for any one customerId,changeIndicator,materialNumber,table");
        }
        String validFrom = "19991231";
        String validTo = "20311231";
        List<GenericEntity> genericEntity = getExistingDetails(customerId);
        GenericEntity generic;
        if(!genericEntity.isEmpty() && genericEntity.get(0)!=null)
            generic = genericEntity.get(0);
        else
            generic = new GenericEntity();
        generic.setName(NAME);
        generic.setKey2(validTo);
        generic.setKey3(validFrom);
        generic.setExtendedAttributes(null);
        generic.setKey4(table);
        generic.setKey5(changeIndicator);
        generic.setKey6(materialNumber);
        generic.setId(customerId);
        generic.setKey1(customerId);
        JsonNode payload = processExclusion(filteredDataList,validFrom,validTo,generic,customerId);
        generic.setPayload(payload);
        generic.setKey9("MDM");
        if(payload==null || payload.isEmpty()) {
            generic.setExtendedAttributes(null);
            generic.setKey9("null");
        }
        else {
            generic.setExtendedAttributes(payload);
        }
        return JSONUtils.getObjectMapper().convertValue(genericEntityService.save(generic),Map.class);
    }

    private JsonNode processExclusion(List<Map<String, Object>> cdm, String validFrom, String validTo, GenericEntity ge, String user) {

        ObjectNode payload = JSONUtils.getObjectMapper().convertValue(ge.getPayload(),ObjectNode.class);
        if(payload==null)
            payload=JSONUtils.getObjectMapper().createObjectNode();
        if(!cdm.isEmpty()){
            ObjectNode finalPayload = payload;
            cdm.forEach(e -> {
                String changeIndicator = e.get(CHANGE_INDICATOR).toString();
                String table = e.get(TABLE).toString();
                String materialNumber = e.get(MATERIAL_NUMBER).toString();
                if(StringUtils.isBlank(materialNumber) ||
                        (!table.equalsIgnoreCase("924") && !table.equalsIgnoreCase("929"))) {
                    return;
                }
                if(table.equalsIgnoreCase("929")&&genericEntityService.findByNameAndKeys("OutletCategory","14",user).isEmpty())
                    throw new DataTransformationService.TransformationException("The subtrade channel is not present");
                if ("I".equalsIgnoreCase(changeIndicator)) {
                    ObjectNode material = JSONUtils.getObjectMapper().createObjectNode();
                    material.put(VALID_FROM, validFrom);
                    material.put(VALID_TO, validTo);
                    finalPayload.set(materialNumber, material);
                } else if ("D".equalsIgnoreCase(changeIndicator)) {
                    finalPayload.remove(materialNumber);
                }
            });
        }
        return JSONUtils.getObjectMapper().convertValue(payload, JsonNode.class);
    }

    private List<GenericEntity> getExistingDetails( String customerId) {
            return genericEntityService.findByNameAndKeys(NAME, customerId);
    }
}
