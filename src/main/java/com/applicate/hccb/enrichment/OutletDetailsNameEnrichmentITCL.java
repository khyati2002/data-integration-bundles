package com.applicate.hccb.enrichment;


import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.OutletDetails;

public class OutletDetailsNameEnrichmentITCL extends AbstractEnrichment<OutletDetails> {

    public OperationResult.StepResult apply(OutletDetails cdm) {
        if (cdm.getContactName() == null) {
            cdm.setContactName(cdm.getOutletName());
        }
        if (cdm.getActiveStatus() == null) {
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
            cdm.setActiveStatusReason(ActiveStatus.ACTIVE.name());
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
    }

}
