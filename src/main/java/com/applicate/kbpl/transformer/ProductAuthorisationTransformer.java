package com.applicate.kbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProductAuthorisationTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    public static final String KGPL = "-KGPL";
    public static final String PAYLOAD = "payload";

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> responseMap = new HashMap<>();

        ifEmpty(inputMap.get("key5")).ifPresentOrElse(val -> responseMap.put("key5", val + KGPL), () -> {
            throw new DataTransformationService.TransformationException("tenantcode cannot be empty");
        });

        ifEmpty(inputMap.get("key3")).ifPresentOrElse(val -> responseMap.put("key3", val + KGPL), () -> {
            throw new DataTransformationService.TransformationException("authorizeditemcode cannot be empty");
        });

        ifEmpty(inputMap.get("key5")).ifPresent(tenantCode -> {
            ifEmpty(inputMap.get("key3")).ifPresent(authorizedItemCode -> {
                StringBuilder idBuilder = new StringBuilder();
                idBuilder.append(tenantCode).append("-KGPL_").append(authorizedItemCode).append("-KGPL_AuthorisedProducts");
                responseMap.put("id", idBuilder.toString());
            });
        });

        if(!ObjectUtils.isEmpty(inputMap.get(PAYLOAD))) {
            Map<String, Map<String, String>> map = (Map<String, Map<String, String>>) inputMap.get(PAYLOAD);

            Map<String, Map<String, String>> modifiedPayload = new HashMap<>();

            for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
                String newKey = entry.getKey() + KGPL;
                Map<String, String> newInnerMap = new HashMap<>();
                for (Map.Entry<String, String> innerEntry : entry.getValue().entrySet()) {
                    newInnerMap.put(innerEntry.getKey(), innerEntry.getValue() + KGPL);
                }
                modifiedPayload.put(newKey, newInnerMap);
            }

            responseMap.put(PAYLOAD, modifiedPayload);
        }

        ifEmpty(inputMap.get("name")).ifPresent(val -> responseMap.put("name", val));
        ifEmpty(inputMap.get("key1")).ifPresent(val -> responseMap.put("key1", val));
        responseMap.put("source", KGPL);

        return responseMap;
    }

    private static Optional<String> ifEmpty(Object object) {
        if(ObjectUtils.isEmpty(object))  {
            return Optional.empty();
        } else {
            return Optional.of(object.toString());
        }
    }
}
