package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.CreditOutlets;
import com.salescode.dim.jooq.impl.OutletDetails;

public class CreditOutletEnrichmentCokeSA extends AbstractEnrichment<CreditOutlets> {

    private final OutletDetailsService outletDetailsService=(OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
    @Override
    public OperationResult.StepResult apply(CreditOutlets creditOutlets) {
        String outletCode= creditOutlets.getOutletCode();

        String outletName=outletDetailsService.getOutletNameByOutletCode(outletCode);

        if(outletCode==null || outletCode.trim().isEmpty()){
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "found no outletName for this outletCode");
        }
        creditOutlets.setOutletName(outletName);
        return new OperationResult.StepResult(OperationResult.Status.OK,"OutletName set Successfully to creditOutlets");
    }
}
