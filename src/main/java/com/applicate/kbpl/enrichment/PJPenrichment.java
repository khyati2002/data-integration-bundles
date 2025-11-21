package com.applicate.kbpl.enrichment;

import com.applicate.services.channelkart.services.DeliveryPJPService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.NullUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.DeliveryPjp;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashSet;
import java.util.Set;

public class PJPenrichment extends AbstractEnrichment<DeliveryPjp> {

    DeliveryPJPService deliveryPJPService ;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public EnrichmentResult apply(DeliveryPjp cdm) {
        deliveryPJPService = (DeliveryPJPService) ServiceLocator.lookup(DeliveryPjp.class);
        JsonNode dayandfreq = cdm.getDayAndFrequency();

        if (ObjectUtils.isNotEmpty(cdm)) {
            DeliveryPjp dnfMap = deliveryPJPService.findByOutletCodeAndLoginIdAndMonthAndYear(cdm.getOutletcode(), cdm.getLoginid(), cdm.getMonth(), cdm.getYear());
            if (dnfMap == null) {
                return new OperationResult.StepResult(OperationResult.Status.OK);
            } else {
                if (NullUtils.isNotNull(dnfMap.getDayAndFrequency())) {
                    ArrayNode dnf = (ArrayNode) dnfMap.getDayAndFrequency();
                    Set<JsonNode> uniqueElements = new HashSet<>();

                    for (JsonNode element : dnf) {
                        uniqueElements.add(element);
                    }

                    for (JsonNode element : dayandfreq) {
                        if (uniqueElements.contains(element)) {
                            throw new IllegalArgumentException("dayandfreq already present in dnfMap");
                        }
                    }
                    dayandfreq.forEach(dnf::add);
                    JsonNode finalPjp = objectMapper.convertValue(dnf, JsonNode.class);
                    cdm.setDayAndFrequency((ArrayNode) finalPjp);
                }
            }
        }
        return new OperationResult.StepResult(OperationResult.Status.OK);
    }
}


