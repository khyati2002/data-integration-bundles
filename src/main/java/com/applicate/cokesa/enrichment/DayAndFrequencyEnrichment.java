package com.applicate.cokesa.enrichment;


import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.DeliveryPJP;
import com.applicate.services.channelkart.services.DeliveryPJPService;
import com.applicate.services.channelkart.services.SpringContext;
import com.salescode.dim.etl.OperationResult;

import java.util.Date;

public class DayAndFrequencyEnrichment extends AbstractEnrichment<DeliveryPJP> {

    @Override
    public OperationResult.StepResult apply(DeliveryPJP deliveryPJP) {
        try {
            DeliveryPJPService pjpService = SpringContext.getBean(DeliveryPJPService.class);
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
