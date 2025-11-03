package com.applicate.unnati.enrichment;

import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.ProductDetails;

/**
 * Flink-compatible ProductDetailsEnrichment
 * Adapted from Spring-based service to use jOOQ ProductDetails
 * 
 * Enriches product details with suggestion text and batch code
 * when these fields are empty or null.
 * 
 * @author System (Adapted for Flink)
 * @since Nov 2025
 */
public class ProductDetailsEnrichment extends AbstractEnrichment<ProductDetails> {

    @Override
    public EnrichmentResult apply(ProductDetails cdm) {
        if (StringUtils.isEmpty(cdm.getSuggestionText())) {
            cdm.setSuggestionText(cdm.getSkuCode() + " " + cdm.getSkuDescription());
        }

        if (StringUtils.isEmpty(cdm.getBatchCode())) {
            cdm.setBatchCode(cdm.getSkuCode());
        }

        return OperationResult.StepResult.OK;
    }
}