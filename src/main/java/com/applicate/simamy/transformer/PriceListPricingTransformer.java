package com.applicate.simamy.transformer;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@SuppressWarnings("VulnerableCodeUsages")
public class PriceListPricingTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    static final String NAME = "PriceListPricingTransformer";
    static final String PRICE_LIST_ID = "priceListId";
    static final String SKU_CODE = "skuCode";
    static final String PRICE = "price";
    static final String START_DATE = "startDate";
    static final String END_DATE = "endDate";

    final GenericEntityRepository genericEntityRepository = SpringContext.getBean(GenericEntityRepository.class);
    @Override
    public Object transform(Map<String, Object> inputMap) {
        Map<String, Object> distPricingList = new HashMap<>();


        final String priceListID = ObjectUtils.isEmpty(inputMap.get(PRICE_LIST_ID)) ? null : inputMap.get(PRICE_LIST_ID).toString();
        final String skuCode = ObjectUtils.isEmpty(inputMap.get(SKU_CODE)) ? null : inputMap.get(SKU_CODE).toString();
        final String price = ObjectUtils.isEmpty(inputMap.get(PRICE)) ? null : inputMap.get(PRICE).toString();
        final String start = ObjectUtils.isEmpty(inputMap.get(START_DATE)) ? null : inputMap.get(START_DATE).toString();
        final String end = ObjectUtils.isEmpty(inputMap.get(END_DATE)) ? null : inputMap.get(END_DATE).toString();
        if(priceListID!=null&&skuCode!=null&&price!=null&&start!=null&&end!=null) {
            List<GenericEntity> genericEntities = getExistingDetails(priceListID);
            ObjectNode priceValidity = new ObjectMapper().createObjectNode();
            priceValidity.put(PRICE, price);
            priceValidity.put(END_DATE, end);
            priceValidity.put(START_DATE, start);
            GenericEntity entity;
            if (!genericEntities.isEmpty()) {
                entity = genericEntities.get(0);
                ObjectNode payload;
                JsonNode existingPayload = entity.getPayload();
                if (existingPayload != null) {
                    payload = JSONUtils.getObjectMapper().convertValue(existingPayload, ObjectNode.class);
                } else {
                    payload = new ObjectMapper().createObjectNode();
                }
                payload.set(skuCode, priceValidity);
                entity.setPayload(payload);
                SpringContext.getBean(GenericEntityService.class).refresh(entity);
                return JSONUtils.convert(entity,Map.class);

            } else {
                ObjectNode payload = new ObjectMapper().createObjectNode();
                payload.set(skuCode, priceValidity);
                distPricingList.put("id",NAME+"-"+priceListID);
                distPricingList.put("name", NAME);
                distPricingList.put("key1", priceListID);
                distPricingList.put("payload", payload);


                return distPricingList;
            }
        }
        else
            return null;
    }

    private List<GenericEntity> getExistingDetails(String priceListID ) {
        if(!(genericEntityRepository.findByNameAndKey1(NAME,priceListID ).isEmpty()))
            return genericEntityRepository.findByNameAndKey1(NAME,priceListID );
        return new ArrayList<>();
    }
}
