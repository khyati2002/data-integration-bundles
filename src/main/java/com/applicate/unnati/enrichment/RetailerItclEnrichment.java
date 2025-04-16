package com.applicate.unnati.enrichment;


import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.User;

public class RetailerItclEnrichment extends AbstractEnrichment<User> {

    @Override
    public OperationResult.StepResult apply(User cdm) {
        Integer version = cdm.getVersion();
        if (version == null) {
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
            cdm.setActiveStatusReason(ActiveStatus.ACTIVE.name());
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, " User Data enriched successfully");
    }

}
