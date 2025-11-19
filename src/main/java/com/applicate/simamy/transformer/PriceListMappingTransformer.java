package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.GenericEntity;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class PriceListMappingTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    static final String NAME = "PriceListMappingTransformer";
    static final String OUT_CODE = "outletCode";
    static final String PRICE_LIST_ID = "priceListId";
    final GenericEntityService genericEntityRepository = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);
    @Override
    public Map<String,Object> transform(Map<String, Object> inputMap) {
        final String outletCode = ObjectUtils.isEmpty(inputMap.get(OUT_CODE)) ? null : inputMap.get(OUT_CODE).toString();
        final String priceListId = ObjectUtils.isEmpty(inputMap.get(PRICE_LIST_ID)) ? null : inputMap.get(PRICE_LIST_ID).toString();
        Map<String, Object> outletPricingList = new HashMap<>();
        List<GenericEntity> genericEntities = getExistingDetails(outletCode);
        if(outletCode!=null && priceListId!=null) {
            GenericEntity entity;
            if (!genericEntities.isEmpty()) {
                entity = genericEntities.get(0);
                entity.setKey2(priceListId);
                return JSONUtils.getObjectMapper().convertValue(entity,Map.class);
            } else {
                outletPricingList.put("id",NAME+"-"+outletCode);
                outletPricingList.put("name", NAME);
                outletPricingList.put("key1", outletCode);
                outletPricingList.put("key2", priceListId);
                return outletPricingList;


            }
        }
        return null;
    }
    private List<GenericEntity> getExistingDetails(String outletCode ) {
        if(!(genericEntityRepository.findByNameAndKey1(NAME,outletCode ).isEmpty()))
            return genericEntityRepository.findByNameAndKey1(NAME,outletCode);
        return new ArrayList<>();
    }
}