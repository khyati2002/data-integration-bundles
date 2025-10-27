package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.models.ProductMetaData;
import com.applicate.services.channelkart.models.Tax;
import com.applicate.services.channelkart.repository.TaxRepository;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.SpringContext;
import com.salescode.dim.etl.OperationResult;

import java.util.List;
import java.util.Objects;

public class ProductMetadataEnrichmentCokeSA extends AbstractEnrichment<ProductMetaData> {

    private final TaxRepository taxRepository = SpringContext.getBean(TaxRepository.class);
    private final GenericEntityService genericEntityService = SpringContext.getBean(GenericEntityService.class);

    @Override
    public OperationResult.StepResult apply(ProductMetaData productMetaData) {
        try {

            String skuCode = productMetaData.getSkuCode();

            if (skuCode == null || skuCode.trim().isEmpty()) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "SKU Code is missing");
            }

            List<Tax> taxes = taxRepository.findByBatchCodeIn(List.of(skuCode));

            Tax exciseTax = taxes.stream()
                    .filter(t -> "EXCISE".equalsIgnoreCase(t.getTaxType()))
                    .findFirst()
                    .orElse(null);

            if (exciseTax == null) {
                return new OperationResult.StepResult(OperationResult.Status.OK, "No EXCISE tax found for SKU, skipping enrichment.");
            }

            Float taxAmount = (float) exciseTax.getTaxRate();

            productMetaData.setTaxAmount(taxAmount);

            double vatPercentage = fetchVatPercentageFromGenericEntity();
            productMetaData.setTax(String.valueOf(vatPercentage));

            return new OperationResult.StepResult(OperationResult.Status.OK, "EXCISE tax amount set successfully.");

        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Error enriching tax amount: " + e.getMessage());
        }
    }

    private double fetchVatPercentageFromGenericEntity() {
        List<GenericEntity> taxes = genericEntityService.readModelsByName("TaxDefined");

        return taxes.stream()
                .map(GenericEntity::getPayload)
                .filter(payload -> payload.has("taxProgram") && "VAT".equalsIgnoreCase(payload.get("taxProgram").asText()))
                .map(payload -> payload.has("taxValueIfPercentage") ? payload.get("taxValueIfPercentage").asDouble() :
                        payload.has("value") ? payload.get("value").asDouble() : null)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(15.0);
    }
}
