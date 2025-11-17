package com.applicate.kbpl.enrichment;

import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.GenericEntity;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

public class PricingEnrichmentJolt extends AbstractEnrichment<GenericEntity> {

    @Override
    public EnrichmentResult apply(GenericEntity genericEntity) {
        if(genericEntity.getName().equalsIgnoreCase("PricingPlan")) {
            JsonNode payload = genericEntity.getPayload();
            JsonNode values = payload.get("val");
            genericEntity.setPayload(values);
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, "Pricing Enriched Successfully");
    }
}

