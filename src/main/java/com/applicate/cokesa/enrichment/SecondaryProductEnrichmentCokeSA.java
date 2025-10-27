package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.models.SecondaryProduct;
import com.salescode.dim.etl.OperationResult;

public class SecondaryProductEnrichmentCokeSA extends AbstractEnrichment<SecondaryProduct> {
    @Override
    public OperationResult.StepResult apply(SecondaryProduct secondaryProduct) {

        if(secondaryProduct.getChannel()==null || secondaryProduct.getChannel().isEmpty()) {
            secondaryProduct.setChannel("all");
        }
        if(secondaryProduct.getOutletType()==null || secondaryProduct.getOutletType().isEmpty()) {
            secondaryProduct.setOutletType("all");
        }
        if(secondaryProduct.getOutletClass()==null || secondaryProduct.getOutletClass().isEmpty()) {
            secondaryProduct.setOutletClass("all");
        }

        return OperationResult.StepResult.OK;
    }
}