package com.applicate.cokesa.enrichment;


import com.applicate.services.channelkart.repository.TaxRepository;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.ProductMetaData;
import com.salescode.dim.etl.OperationResult.*;
import com.salescode.dim.jooq.impl.Tax;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class ProductMetadataEnrichmentCokeSA extends AbstractEnrichment<ProductMetaData> {

    private final TaxRepository taxRepository = (TaxRepository) ServiceLocator.lookup(Tax.class);
    private final GenericEntityService genericEntityService=(GenericEntityService) ServiceLocator.lookup(GenericEntity.class);

    @Override
    public StepResult apply(ProductMetaData productMetaData) {
        try {

            String skuCode = productMetaData.getSkuCode();

            if (skuCode == null || skuCode.trim().isEmpty()) {
                return new StepResult(Status.ERROR, "SKU Code is missing");
            }

            List<Tax> taxes = taxRepository.findByBatchCodeIn(List.of(skuCode));

            Tax exciseTax = taxes.stream()
                    .filter(t -> "EXCISE".equalsIgnoreCase(t.getTaxType()))
                    .findFirst()
                    .orElse(null);

            if (exciseTax == null) {
                return new StepResult(Status.OK, "No EXCISE tax found for SKU, skipping enrichment.");
            }


            productMetaData.setTaxAmount(BigDecimal.valueOf(exciseTax.getTaxRate()));

            double vatPercentage = fetchVatPercentageFromGenericEntity();
            productMetaData.setTax(String.valueOf(vatPercentage));

            return new StepResult(Status.OK, "EXCISE tax amount set successfully.");

        } catch (Exception e) {
            return new StepResult(Status.ERROR, "Error enriching tax amount: " + e.getMessage());
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
