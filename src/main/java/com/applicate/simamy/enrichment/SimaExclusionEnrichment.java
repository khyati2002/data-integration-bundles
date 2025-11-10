package com.applicate.simamy.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;

public class SimaExclusionEnrichment extends AbstractEnrichment<GenericEntity> {
    GenericEntityRepository repository = SpringContext.getBean(GenericEntityRepository.class);

    @Override
    public EnrichmentResult apply(GenericEntity genericEntity) {
        if (genericEntity.getExtendedAttributes() != null &&
                genericEntity.getKey9() != null &&
                !genericEntity.getKey9().equalsIgnoreCase("MDM") &&
                genericEntity.getKey9().equalsIgnoreCase("null")) {

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
        else if(genericEntity.getKey9() != null && genericEntity.getKey9().equalsIgnoreCase("MDM")) {
            genericEntity.setPayload(genericEntity.getExtendedAttributes());
            genericEntity.setKey9(null);
            genericEntity.setExtendedAttributes(null);
        } else if (genericEntity.getKey9() != null && genericEntity.getKey9().equalsIgnoreCase("null")) {
            genericEntity.setPayload(null);
            genericEntity.setExtendedAttributes(null);
            genericEntity.setKey9(null);
        }

        return new EnrichmentResult(Status.OK, "SimaExclusion Enriched Successful");
    }
}