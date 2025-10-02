package com.applicate.hccb.enrichment;

import com.applicate.services.channelkart.repository.ProductDetailsImpl;
import com.applicate.services.channelkart.services.GenericObjectService;
import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.GenericObject;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import com.salescode.dim.jooq.generated.tables.pojos.SchemeOutletBifurcations;
import com.salescode.dim.jooq.impl.SchemeDefination;

import java.math.BigDecimal;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
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
                    if(ObjectUtils.isEmpty(ge)) {
                        log.error("Channel not found for schemeID: {}", cdm.getSchemeId());
                    }
                    outletBifurcation.setChannel(channelId);
                }
            }
            this.enrichItemSchemeDescription(cdm);
            this.logger.info("Time taken for channel enrichment : {}", (System.currentTimeMillis() - currentTime));
        } catch (Exception ex) {
            log.error("Error while setting channel {}", ex.getMessage());
            throw new RuntimeException("Exception in channel enrichment {}", ex);
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
    }

    private boolean checkIfEmpty(List<SchemeOutletBifurcations> allOutletBifurcationDetails) {
        return NullUtils.isNotNull(allOutletBifurcationDetails) && !allOutletBifurcationDetails.isEmpty();
    }

    private void enrichItemSchemeDescription(SchemeDefination cdm) {
        if (!isItemSchemeWithProductCode(cdm)) {
            return;
        }

        String inputCode = cdm.getSchemeCalculation().get(0).getSchemeDiscountedProductcode();
        Productdetails pd = productDetailsService.findByBatchCode(inputCode);

        if (pd == null && inputCode.contains("_")) {
            pd = findProductByEanCodeWithNearestMrp(inputCode);
        }

        if (pd != null) {
            String name = pd.getSkuDescription();
            String newDes = cdm.getSchemeDescription() + " (" + name + ")";
            cdm.setSchemeDescription(newDes);
            updateSchemeAndSlabDescription(cdm, pd.getSkuDescription());
            cdm.getSchemeCalculation().get(0).setSchemeDiscountedProductcode(pd.getBatchCode());
        } else {
            logger.error("No product details found for code: {}", inputCode);
            throw new DataTransformationService.TransformationException("No product details found");
        }
    }

    private boolean isItemSchemeWithProductCode(SchemeDefination cdm) {
        return cdm.getSchemeType().contains("item") && ObjectUtils.isNotEmpty(cdm.getSchemeCalculation().get(0).getSchemeDiscountedProductcode());
    }

    private Productdetails findProductByEanCodeWithNearestMrp(String inputCode) {
        try {
            String[] parts = inputCode.split("_");
            String eanCode = parts[0];
            BigDecimal targetMrpBd = new BigDecimal(parts[1]);

            List<Productdetails> sku = productDetailsService.findByEanCode(eanCode);
            if (sku == null) {
                log.error("Product Details not found for EAN Code {} | {}", eanCode, inputCode);
                return null;
            }

            Productdetails closestSku = sku.get(0);
            BigDecimal minDiff = closestSku.getMrp().subtract(targetMrpBd).abs();

            for (Productdetails s : sku) {
                BigDecimal diff = s.getMrp().subtract(targetMrpBd).abs();
                if (diff.compareTo(minDiff) < 0) {
                    minDiff = diff;
                    closestSku = s;
                }
            }

            return productDetailsService.findByBatchCode(closestSku.getBatchCode());

        } catch (Exception e) {
            logger.error("Error parsing EANCode_MRP: {}", inputCode, e);
            throw new DataTransformationService.TransformationException("Error parsing EANCode_MRP ",e);
        }
    }

    private void updateSchemeAndSlabDescription(SchemeDefination cdm, String skuDescription) {
        String newDes = cdm.getSchemeDescription() + " (" + skuDescription + ")";
        cdm.setSchemeDescription(newDes);
        this.updateSlabDescription(cdm.getSchemeCalculation().get(0).getSlabInfo(), skuDescription);
    }

    private void updateSlabDescription(JsonNode slabInfo, String name) {
        for (JsonNode node : slabInfo) {
            String newDes = node.get("schemeDescription").asText() + " (" + name + ")";
            ObjectNode objectNode = (ObjectNode)node;
            objectNode.put("schemeDescription", newDes);
        }
    }
}

