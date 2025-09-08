package com.applicate.unnati.enrichment;


import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;

public class UserEnrichmentItcl extends AbstractEnrichment<CommonDataModel> {

    public EnrichmentResult apply(CommonDataModel commonDataModel) {
        return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
    }
}
