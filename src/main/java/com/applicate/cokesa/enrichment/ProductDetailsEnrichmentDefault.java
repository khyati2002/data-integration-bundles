package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.models.ProductDetails;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import org.apache.commons.lang3.ObjectUtils;


public class ProductDetailsEnrichmentDefault  extends AbstractEnrichment<ProductDetails> {
    @Override
    public OperationResult.StepResult apply(Productdetails cdm) {

        String purchaseUnit = cdm.getPurchaseUnit();
        String skuCode = cdm.getSkuCode();

        if(purchaseUnit != null && !ObjectUtils.isEmpty(purchaseUnit)) {
            cdm.setBatchCode(skuCode + "-" + purchaseUnit);
        }
        return OperationResult.StepResult.OK ;

    }

}
