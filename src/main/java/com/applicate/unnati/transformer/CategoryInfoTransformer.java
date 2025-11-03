package com.applicate.unnati.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.collections.MapUtils.getString;

public class CategoryInfoTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(CategoryInfoTransformer.class);
    private static final String ACTIVE = "active";

    @Override
    public Map<String, Object> transform(Map<String, Object> responseEnvelope) {
        Map<String, Object> output = new LinkedHashMap<>();

        try {
            // Map AreaCode to categoryCode
            String areaCode = getString(responseEnvelope, "AreaCode");
            output.put("categoryCode", areaCode);

            // Map Description to categoryValue and name
            String description = getString(responseEnvelope, "Description");
            output.put("categoryValue", description);
            output.put("name", description);

            // Map dataAreaId to feature
            String dataAreaId = getString(responseEnvelope, "dataAreaId");
            output.put("feature", dataAreaId);

            // Set active status
            output.put("activeStatus", ACTIVE);
            output.put("activeStatusReason", ACTIVE);

            output.put("changed",1);

            // Optional fields - can be set if data is available
            // output.put("newDescription", getString(responseEnvelope, "newDescription"));
            // output.put("oldDescription", getString(responseEnvelope, "oldDescription"));

            logger.debug("Successfully transformed CategoryInfo for AreaCode: {}", areaCode);

        } catch (Exception e) {
            logger.error("Failed to transform CategoryInfo data", e);
            throw new RuntimeException("CategoryInfo transformation failed", e);
        }

        return output;
    }
}