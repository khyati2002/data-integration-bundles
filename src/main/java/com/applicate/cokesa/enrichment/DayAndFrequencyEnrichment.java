package com.applicate.cokesa.enrichment;


import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.DeliveryPJP;
import com.salescode.dim.etl.OperationResult;

import java.util.Date;

public class DayAndFrequencyEnrichment extends AbstractEnrichment<DeliveryPJP> {

    @Override
    public OperationResult.StepResult apply(DeliveryPJP deliveryPJP) {
        try {
            DeliveryPJPService pjpService = (DeliveryPJPService) ServiceLocator.lookup(DeliveryPJP.class);
            Date pjpDate = deliveryPJP.getPjpDate();
            if (pjpDate != null) {
                pjpService.addDayAndFrequency(deliveryPJP);
            }
        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage());
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, "Day and frequency persisted succesfully ");

    }
}
