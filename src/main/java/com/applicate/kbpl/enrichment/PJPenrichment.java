package com.applicate.kbpl.enrichment;

import com.applicate.services.channelkart.services.DeliveryPJPService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.DeliveryPjp;
import org.apache.commons.lang3.ObjectUtils;
import org.jooq.JSON;

import java.util.HashSet;
import java.util.Set;

public class PJPenrichment extends AbstractEnrichment<DeliveryPjp> {

    DeliveryPJPService deliveryPJPService ;


    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public EnrichmentResult apply(DeliveryPjp cdm) {
        deliveryPJPService = (DeliveryPJPService) ServiceLocator.lookup(DeliveryPjp.class);
        JSON dayandfreq = cdm.getDayAndFrequency();

        if (ObjectUtils.isNotEmpty(cdm)) {
            DeliveryPjp dnfMap = deliveryPJPService.findByOutletCodeAndLoginIdAndMonthAndYear(cdm.getOutletcode(), cdm.getLoginid(), cdm.getMonth(), cdm.getYear());
            if (dnfMap == null) {
                return new OperationResult.StepResult(OperationResult.Status.OK);
            } else {
                if (NullUtils.isNotNull(dnfMap.getDayAndFrequency())) {
                    JsonNode jsonNode = null;
                    try {
                        jsonNode = objectMapper.readTree(dnfMap.getDayAndFrequency().data());
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    if (!(jsonNode instanceof ArrayNode)) {
                        throw new IllegalArgumentException("dayAndFrequency is not an array");
                    }
                    ArrayNode dnf = (ArrayNode) jsonNode;

                    Set<JsonNode> uniqueElements = new HashSet<>();

                    for (JsonNode element : dnf) {
                        uniqueElements.add(element);
                    }

                    try {
                        JsonNode dayandfreqNode = objectMapper.readTree(dayandfreq.data());
                        if (!(dayandfreqNode instanceof ArrayNode)) {
                            throw new IllegalArgumentException("dayAndFrequency (cdm) is not an array");
                        }
                        ArrayNode dayandfreqArray = (ArrayNode) dayandfreqNode;
                        for (JsonNode element : dayandfreqArray) {
                            if (uniqueElements.contains(element)) {
                                throw new IllegalArgumentException("dayandfreq already present in dnfMap");
                            }
                        }
                        dayandfreqArray.forEach(dnf::add);
                        JsonNode finalPjp = objectMapper.convertValue(dnf, JsonNode.class);
                        cdm.setDayAndFrequency(JSON.valueOf(finalPjp.toString()));
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }

                }
            }
        }
        return new OperationResult.StepResult(OperationResult.Status.OK);
    }
}


