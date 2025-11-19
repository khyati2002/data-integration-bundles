//package com.applicate.simamy.enrichment;
//
//
//import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
//import com.applicate.services.channelkart.enrichments.EnrichmentResult;
//import com.applicate.services.channelkart.enrichments.Status;
//import com.applicate.services.channelkart.models.GenericEntity;
//import com.fasterxml.jackson.databind.JsonNode;
//
//public class SimaGroupingEnrichment extends AbstractEnrichment<GenericEntity> {
//
//    @Override
//    public EnrichmentResult apply(GenericEntity genericEntity) {
//        if(genericEntity.getName().equalsIgnoreCase("ProductGroup") || genericEntity.getName().equalsIgnoreCase("CustomerGroup")) {
//            JsonNode payload = genericEntity.getPayload();
//            JsonNode values = payload.get("values");
//            genericEntity.setPayload(values);
//        }
//        return new EnrichmentResult(Status.OK, "SimaGrouping Enriched Successfully");
//    }
//}
//
