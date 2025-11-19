package com.applicate.niine.enrichment;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.commons.lang3.ObjectUtils;

public class ProductDetailsEnrichmentNiine extends AbstractEnrichment<ProductDetails> {
    @Override
    public OperationResult.StepResult apply(ProductDetails cdm) {

        String purchaseUnit = cdm.getPurchaseUnit();
        String skuCode = cdm.getSkuCode();

        if (purchaseUnit != null && !ObjectUtils.isEmpty(purchaseUnit)) {
            cdm.setBatchCode(skuCode + "-" + purchaseUnit);
        }
        return OperationResult.StepResult.OK;

    }

}