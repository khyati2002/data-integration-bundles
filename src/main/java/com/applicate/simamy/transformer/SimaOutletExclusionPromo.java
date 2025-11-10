package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.ReflectionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public class SimaOutletExclusionPromo extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    static final String NAME = "OutletExclusionPromo";
    static final String TRO = "TRO";
    static final String OUTLET_CODE = "Outlet id";

    final GenericEntityRepository genericEntityRepository = SpringContext.getBean(GenericEntityRepository.class);

    @Override
    public Object transform(Map<String, Object> inputMap) {
        List<Object> dataList = JSONUtils.convert(inputMap.get("data_value"), List.class);
        if (dataList == null || dataList.isEmpty()) {
            throw new TransformationException("Data list is empty");
        }
        String schemeId = JSONUtils.convert(dataList.get(0), Map.class).get(TRO).toString();
        List<Map<String, Object>> filteredDataList = new ArrayList<>();
        Class<?> distributedCacheClass = null;
        try {
            distributedCacheClass = Class.forName("com.applicate.services.channelkart.cache.DistributedCache");

            Object distributedCacheBean = SpringContext.getBean(distributedCacheClass);
            Supplier<GenericEntity> transformationSupplier = () -> {
                for (Object data : dataList) {
                    Map<String, Object> input = JSONUtils.convert(data, Map.class);
                    String tro = input.get(TRO) != null ? input.get(TRO).toString() : null;
                    String outletCode = input.get(OUTLET_CODE) != null ? input.get(OUTLET_CODE).toString() : null;
                    if (tro != null && !tro.isEmpty() && outletCode != null && !outletCode.isEmpty()) {
                        filteredDataList.add(input);
                    }
                }
                if (filteredDataList.isEmpty()) {
                    throw new TransformationException("TRO or Outlet code not found");
                }
                String schemeIdToDelete = filteredDataList.get(0).get(TRO).toString();
                deleteInBatch(schemeIdToDelete);
                Map<String, Object> genericEntity = new HashMap<>();
                genericEntity.put("name", NAME);
                genericEntity.put("id", schemeIdToDelete.concat("-" + NAME));
                genericEntity.put("key1", schemeIdToDelete);
                ObjectNode payload = new ObjectMapper().createObjectNode();
                for (Object data : filteredDataList) {
                    Map<String, Object> input = JSONUtils.convert(data, Map.class);
                    String outlet = input.get(OUTLET_CODE).toString();
                    payload.put(outlet, outlet);
                }
                genericEntity.put("payload", payload);
                return JSONUtils.convert(genericEntity,GenericEntity.class);
            };
            return ReflectionUtils.invokeMethod(distributedCacheBean, "withLock", schemeId, transformationSupplier);

        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private void deleteInBatch(String schemeId) {
        List<GenericEntity> entities = genericEntityRepository.findByName(NAME);
        if (!entities.isEmpty()) {
            // Find the maximum lastModifiedTime among all entities
            Date maxLastModifiedTime = entities.stream()
                    .filter(e -> e.getLastModifiedTime() != null)
                    .map(GenericEntity::getLastModifiedTime)
                    .max(Date::compareTo)
                    .orElse(null);

            // Only delete if the max last modified time is older than 30 minutes
            if (maxLastModifiedTime != null && isOlderThan30Minutes(maxLastModifiedTime)) {
                genericEntityRepository.deleteAllInBatch(entities);
            }
            else{
                entities = genericEntityRepository.findByNameAndKey1(NAME,schemeId);
                genericEntityRepository.deleteAllInBatch(entities);
            }
        }

    }
    private boolean isOlderThan30Minutes(Date lastModifiedTime) {
        if (lastModifiedTime == null) {
            return false;
        }

        Instant lastModifiedInstant = lastModifiedTime.toInstant();
        Instant thirtyMinutesAgo = Instant.now().minus(Duration.ofMinutes(30));

        return lastModifiedInstant.isBefore(thirtyMinutesAgo);
    }
}
