package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.DeliveryPJPService;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp;
import com.salescode.dim.jooq.impl.CreditOutlets;
import com.salescode.dim.jooq.impl.OutletDetails;

public class CreditOutletEnrichmentCokeSA extends AbstractEnrichment<CreditOutlets> {

    @Override
    public OperationResult.StepResult apply(CreditOutlets creditOutlets) {
        OutletDetailsService outletDetailsService=(OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
        DeliveryPJPService deliveryPJPService=(DeliveryPJPService)  ServiceLocator.lookup(DeliveryPjp.class);

        String outletCode= creditOutlets.getOutletCode();

        String salesRep=deliveryPJPService.getLoginIdByOutletcode(outletCode);

        String outletName=outletDetailsService.getOutletNameByOutletCode(outletCode);

        if(outletCode==null || outletCode.trim().isEmpty()){
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "found no outletName for this outletCode");
        }
        creditOutlets.setOutletName(outletName);
        creditOutlets.setActiveStatus(ActiveStatus.ACTIVE);
        creditOutlets.setCreatedBy(salesRep);
        creditOutlets.setModifiedBy(salesRep);
        return new OperationResult.StepResult(OperationResult.Status.OK,"OutletName set Successfully to creditOutlets");
    }
}
