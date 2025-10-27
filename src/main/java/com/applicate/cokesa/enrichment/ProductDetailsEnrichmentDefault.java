package com.applicate.cokesa.enrichment;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import org.apache.commons.lang3.ObjectUtils;


public class ProductDetailsEnrichmentDefault  extends AbstractEnrichment<Productdetails> {
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
