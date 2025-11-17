package com.applicate.kbpl.enrichment;

import com.applicate.services.channelkart.utils.NullUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.Stock;

public class ResetExpiry extends AbstractEnrichment<Stock> {
    @Override
    public EnrichmentResult apply(Stock cdm) {

        JsonNode extendedAttributes = cdm.getExtendedAttributes();
        if(NullUtils.isNotNull(extendedAttributes) && NullUtils.isNotNull(extendedAttributes.get("updateExpiry"))){
            JsonNode expiryDateFromDb = extendedAttributes.get("previousExpireDate");
            ((ObjectNode) extendedAttributes).set("expiredate",expiryDateFromDb);
            ((ObjectNode) extendedAttributes).remove("updateExpiry");
            ((ObjectNode) extendedAttributes).remove("previousExpireDate");
        }
        return new OperationResult.StepResult(OperationResult.Status.OK,"Reset stock expirydate Successfully");
    }
}
