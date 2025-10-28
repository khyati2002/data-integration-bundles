package com.applicate.cokesa.enrichment;

import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.SecondaryProduct;
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