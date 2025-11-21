package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.utils.ReflectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;


public class SimaInclusionTransformer extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {
    static final String NAME = "SchemeInclusion";
    static final String TRO = "TRO";
    static final String OUTLET_CODE = "Outlet id";
    final GenericEntityService genericEntityRepository = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);
    public Map<String,Object> transform(Map<String, Object> inputMap) {

        List<Object> dataList = JSONUtils.getObjectMapper().convertValue(inputMap.get("data_value"), List.class);
        if (dataList == null || dataList.isEmpty()) {
            throw new DataTransformationService.TransformationException("Data list is empty");
        }

        String schemeId = JSONUtils.getObjectMapper().convertValue(dataList.get(0),Map.class).get(TRO).toString();
        try {
            Supplier<GenericEntity> transformationSupplier = () -> {
                List<Map<String, Object>> filteredDataList = new ArrayList<>();
                for (Object data : dataList) {
                    Map<String, Object> input = JSONUtils.getObjectMapper().convertValue(data, Map.class);
                    String tro = input.get(TRO) != null ? input.get(TRO).toString() : null;
                    String outletCode = input.get(OUTLET_CODE) != null ? input.get(OUTLET_CODE).toString() : null;
                    if (tro != null && !tro.isEmpty() && outletCode != null && !outletCode.isEmpty()) {
                        filteredDataList.add(input);
                    }
                }
                if (filteredDataList.isEmpty()) {
                    throw new DataTransformationService.TransformationException("TRO or Outlet code not found");
                }
                String schemeIdToDelete = filteredDataList.get(0).get(TRO).toString();

                deleteInBatch(schemeIdToDelete);
                Map<String, Object> genericEntity = new HashMap<>();
                genericEntity.put("name", NAME);
                genericEntity.put("key1", schemeIdToDelete);
                genericEntity.put("id", schemeIdToDelete.concat("-" + NAME));
                ObjectNode payload = new ObjectMapper().createObjectNode();
                for (Object data : filteredDataList) {
                    Map<String, Object> input = JSONUtils.getObjectMapper().convertValue(data, Map.class);
                    String outlet = input.get(OUTLET_CODE).toString();
                    payload.put(outlet, outlet);
                }
                genericEntity.put("payload", payload);
                return JSONUtils.getObjectMapper().convertValue(genericEntity, GenericEntity.class);
            };

            // Execute transformation with distributed lock
            return ReflectionUtils.invokeMethod("withLock", schemeId, transformationSupplier);

        }
        catch (Exception e) {
            throw new DataTransformationService.TransformationException("Error during transformation: " + e.getMessage(), e);
        }
    }

    private void deleteInBatch(String schemeId) {
        List<GenericEntity> entities = genericEntityRepository.findByName(NAME);
        if (!entities.isEmpty()) {
            LocalDateTime maxLastModifiedTime = entities.stream()
                    .map(e -> e.getLastModifiedTime())
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            // Only delete if the max last modified time is older than 30 minutes
            if (maxLastModifiedTime != null && isOlderThan30Minutes(maxLastModifiedTime)) {
                genericEntityRepository.deleteInBatch(entities);
            }
            else{
                entities = genericEntityRepository.findByNameAndKeys(NAME,schemeId);
                genericEntityRepository.deleteInBatch(entities);
            }
        }

    }
    private boolean isOlderThan30Minutes(LocalDateTime lastModifiedTime) {
        if (lastModifiedTime == null) {
            return false;
        }
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minus(Duration.ofMinutes(30));
        return lastModifiedTime.isBefore(thirtyMinutesAgo);
    }
}
