package com.applicate.hccb.enrichment;

import com.applicate.services.channelkart.repository.ProductDetailsImpl;
import com.applicate.services.channelkart.services.GenericObjectService;
import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.GenericObject;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import com.salescode.dim.jooq.impl.SchemeDefination;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class HCCBChannelEnrichment
        extends AbstractEnrichment<SchemeDefination> {
    private static GenericObjectService genericObjectService;
    private static ProductDetailsService productDetailsService;
    Logger logger = LoggerFactory.getLogger(HCCBChannelEnrichment.class);

    public HCCBChannelEnrichment() {
        genericObjectService = new GenericObjectService();
        productDetailsService = new ProductDetailsService(new ProductDetailsImpl());
    }

    @Override
    public EnrichmentResult apply(SchemeDefination cdm) {
        if (ObjectUtils.isNotEmpty(cdm.getProgramLevel()) && "pricingCondition".equalsIgnoreCase(cdm.getProgramLevel())) {
            return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
        }
        try {
            List<SchemeOutletBifurcations> allOutletBifurcationDetails = cdm.getSchemeOutletBifurcationsList();
            long currentTime = System.currentTimeMillis();
            if (this.checkIfEmpty(allOutletBifurcationDetails)) {
                for (SchemeOutletBifurcations outletBifurcation : allOutletBifurcationDetails) {
                    if (outletBifurcation.getChannel() == null || outletBifurcation.getChannel().isEmpty() || outletBifurcation.getChannel().equalsIgnoreCase("all")) continue;
                    String channelId = outletBifurcation.getChannel();
                    GenericObject ge = genericObjectService.fetchByValue("channel", channelId);
                    if (ObjectUtils.isNotEmpty(ge)) {
                        GenericObject geobject = ge;
                        outletBifurcation.setChannel(geobject.getKey3());
                        continue;
                    }
                    outletBifurcation.setChannel(channelId);
                }
            }
            this.enrichItemSchemeDescription(cdm);
            this.logger.info("Time taken for channel enrichment : {}", (Object)(System.currentTimeMillis() - currentTime));
        } catch (Exception ex) {
            throw new RuntimeException("Exception in channel enrichment {}", ex);
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
    }

    private boolean checkIfEmpty(List<SchemeOutletBifurcations> allOutletBifurcationDetails) {
        return NullUtils.isNotNull(allOutletBifurcationDetails) && !allOutletBifurcationDetails.isEmpty();
    }

    private void enrichItemSchemeDescription(SchemeDefination cdm) {
        if (cdm.getSchemeType().contains("item") && ObjectUtils.isNotEmpty(cdm.getSchemeCalculation().get(0).getSchemeDiscountedProductcode())) {
            Productdetails pd = productDetailsService.findByBatchCode(cdm.getSchemeCalculation().get(0).getSchemeDiscountedProductcode());
            String name = pd.getSkuDescription();
            String newDes = cdm.getSchemeDescription() + " (" + name + ")";
            cdm.setSchemeDescription(newDes);
            this.updateSlabDescription(cdm.getSchemeCalculation().get(0).getSlabInfo(), name);
        }
    }

    private void updateSlabDescription(JsonNode slabInfo, String name) {
        for (JsonNode node : slabInfo) {
            String newDes = node.get("schemeDescription").asText() + " (" + name + ")";
            ObjectNode objectNode = (ObjectNode)node;
            objectNode.put("schemeDescription", newDes);
        }
    }
}

