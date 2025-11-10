package com.applicate.cokeph.enrichment;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.GenericObject;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;

public class CokephExclusionMasterEnrichment extends AbstractEnrichment<GenericObject> {
    public EnrichmentResult apply(GenericObject genericEntity) {
        if (ObjectUtils.isNotEmpty(genericEntity.getExtendedAttributes())) {
            ObjectNode payload = JSONUtils.getObjectMapper().convertValue(genericEntity.getPayload(), ObjectNode.class);
            ObjectNode extended = JSONUtils.getObjectMapper().convertValue(genericEntity.getExtendedAttributes(), ObjectNode.class);
            Iterator<String> fieldNames = genericEntity.getExtendedAttributes().fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                if (payload.has(key)) {
                    payload.remove(key);
                    extended.remove(key);
                }
            }
            genericEntity.setPayload(JSONUtils.getObjectMapper().convertValue(payload, JsonNode.class));
            genericEntity.setExtendedAttributes(JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class));
        }

        return new OperationResult.StepResult(OperationResult.Status.OK, "SimaExclusion Enriched Successful");
    }
}

