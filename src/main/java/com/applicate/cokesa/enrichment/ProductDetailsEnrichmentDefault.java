package com.applicate.cokesa.enrichment;


import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.commons.lang3.ObjectUtils;
import com.salescode.dim.etl.OperationResult.*;

public class ProductDetailsEnrichmentDefault extends AbstractEnrichment<ProductDetails> {
    @Override
    public StepResult apply(ProductDetails cdm) {

        String purchaseUnit = cdm.getPurchaseUnit();
        String skuCode = cdm.getSkuCode();

        if(purchaseUnit != null && !ObjectUtils.isEmpty(purchaseUnit)) {
            cdm.setBatchCode(skuCode + "-" + purchaseUnit);
        }
        return new StepResult(Status.OK) ;

    }

}
