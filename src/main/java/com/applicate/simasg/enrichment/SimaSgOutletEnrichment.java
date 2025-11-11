package com.applicate.simasg.enrichment;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.etl.OperationResult;


public class SimaSgOutletEnrichment extends AbstractEnrichment<User> {
    @Override
    public EnrichmentResult apply(User outletDetails) {
        outletDetails.setDialCode("65");
        return new EnrichmentResult(OperationResult.Status.OK, "SimaSgOutletEnrichment Enriched Successful");
    }
}