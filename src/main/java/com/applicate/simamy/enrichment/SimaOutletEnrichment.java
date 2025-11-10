package com.applicate.simamy.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.User;

public class SimaOutletEnrichment extends AbstractEnrichment<User> {

    @Override
    public EnrichmentResult apply(User outletDetails) {
        outletDetails.setDialCode("60");
        return new EnrichmentResult(Status.OK, "SimaOutletEnrichment Enriched Successful");
    }
}
