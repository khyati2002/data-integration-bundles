package com.applicate.simasg.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.User;

public class SimaSgOutletEnrichment extends AbstractEnrichment<User> {
    @Override
    public EnrichmentResult apply(User outletDetails) {
        outletDetails.setDialCode("65");
        return new EnrichmentResult(Status.OK, "SimaSgOutletEnrichment Enriched Successful");
    }
}