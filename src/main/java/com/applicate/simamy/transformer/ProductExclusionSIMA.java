package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

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
    GenericEntityService genericEntityService = SpringContext.getBean(GenericEntityService.class);
    OutletDetailsService outletDetailsService = SpringContext.getBean(OutletDetailsService.class);

    @Override
    public Object transform(Map<String, Object> cdm) {
            List<Object> dataList = JSONUtils.convert(cdm.get("data_value"), List.class);
            if (dataList == null || dataList.isEmpty()) {
                throw new TransformationException("Data list is empty");
            }
            List<Map<String, Object>> filteredDataList = new ArrayList<>();
            for (Object data : dataList) {
                Map<String, Object> input = JSONUtils.convert(data, Map.class);
                filteredDataList.add(input);
            }
            Map<String,Object> input = JSONUtils.convert(dataList.get(0), Map.class);
        final String table = ObjectUtils.isEmpty(input.get(TABLE)) ? NULL : input.get(TABLE).toString();
        String customerId = ObjectUtils.isEmpty(input.get(CUS_ID)) ? NULL : input.get(CUS_ID).toString();

        String changeIndicator = ObjectUtils.isEmpty(input.get(CHANGE_INDICATOR)) ? NULL : input.get(CHANGE_INDICATOR).toString();
        final String materialNumber = ObjectUtils.isEmpty(input.get(MATERIAL_NUMBER)) ? NULL : input.get(MATERIAL_NUMBER).toString();
        if (customerId.equalsIgnoreCase(NULL) ||
                changeIndicator.equalsIgnoreCase(NULL) ||
                materialNumber.equalsIgnoreCase(NULL) ||
                table.equalsIgnoreCase(NULL)) {
            throw new TransformationException("Illegal Values for any one customerId,changeIndicator,materialNumber,table");
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
        ObjectNode payload = processExclusion(filteredDataList,validFrom,validTo,generic,customerId);
        generic.setPayload(payload);
        generic.setKey9("MDM");
        if(payload==null || payload.isEmpty()) {
            generic.setExtendedAttributes(null);
            generic.setKey9("null");
        }
        else {
            generic.setExtendedAttributes(payload);
        }
        return genericEntityService.save(genericEntityService.refresh(generic));
    }

    private ObjectNode processExclusion(List<Map<String, Object>> cdm,String validFrom,String validTo,GenericEntity ge,String user) {

        ObjectNode payload = JSONUtils.convert(ge.getPayload(),ObjectNode.class);
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
                if(table.equalsIgnoreCase("929")&&genericEntityService.readModelsByNameAndKey1AndKey2("OutletCategory","14",user).isEmpty())
                    throw new TransformationException("The subtrade channel is not present");
                if ("I".equalsIgnoreCase(changeIndicator)) {
                    ObjectNode material = JSONUtils.getObjectMapper().createObjectNode();
                    material.put(VALID_FROM, validFrom);
                    material.put(VALID_TO, validTo);
                    finalPayload.put(materialNumber, material);
                } else if ("D".equalsIgnoreCase(changeIndicator)) {
                    finalPayload.remove(materialNumber);
                }
            });
        }
        return payload;
    }

    private List<GenericEntity> getExistingDetails( String customerId) {
            return genericEntityService.readModelsByNameAndKey1(NAME, customerId);
    }
}
