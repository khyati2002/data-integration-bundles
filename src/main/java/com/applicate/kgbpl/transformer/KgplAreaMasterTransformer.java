package com.applicate.kgbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;

import java.util.HashMap;
import java.util.Map;

public class KgplAreaMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> source) {

        validateFields(source);

        Map<String, Object> result = new HashMap<>();

        // 1️⃣ Extract raw fields
        String areaCode = getRawValue(source.get("AreaCode"));
        String dataAreaId = getRawValue(source.get("dataAreaId"));
        String description = getRawValue(source.get("Description"));

        // 2️⃣ Shift equivalent
        result.put("rawCategoryCode", areaCode);
        result.put("dataAreaId", dataAreaId);
        result.put("categoryValue", description);

        // 3️⃣ Default operation
        result.put("name", "outletMaster");
        result.put("feature", "area");

        // 4️⃣ Modify-overwrite-beta (derived keys)
        String id = String.join("-", areaCode, dataAreaId, "area", "outletMaster");
        String categoryCode = String.join("-", areaCode, dataAreaId);
        result.put("id", id);
        result.put("categoryCode", categoryCode);

        // 5️⃣ Remove intermediate fields (simulate "remove" operation)
        result.remove("dataAreaId");
        result.remove("rawCategoryCode");

        return result;
    }

    // ----------------------------------------------------------------------
    // Helper Methods
    // ----------------------------------------------------------------------

    private void validateFields(Map<String, Object> source) {
        if (getRawValue(source.get("AreaCode")).isEmpty())
            throw new DataTransformationService.TransformationException(
                    "Validation Failed: 'AreaCode' cannot be empty");
        if (getRawValue(source.get("dataAreaId")).isEmpty())
            throw new DataTransformationService.TransformationException(
                    "Validation Failed: 'dataAreaId' cannot be empty");
    }

    private String getRawValue(Object value) {
        if (value == null) return "";
        if (value instanceof String) {
            String s = ((String) value).trim();
            return s.isEmpty() ? "" : s;
        }
        return value.toString().trim();
    }
}