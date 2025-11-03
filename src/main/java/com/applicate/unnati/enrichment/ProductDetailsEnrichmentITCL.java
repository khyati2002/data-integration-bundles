package com.applicate.unnati.enrichment;



import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.ProductDetails;

import java.math.BigDecimal;

public class ProductDetailsEnrichmentITCL extends AbstractEnrichment<ProductDetails> {

    @Override
    public EnrichmentResult apply(ProductDetails cdm) {
        String s = StringUtils.isEmpty(cdm.getSkuDescription()) ? cdm.getSkuName()
                : cdm.getSkuDescription() + "~~" + cdm.getMarketSku() + "~~" + cdm.getCategory() + "~~"
                + cdm.getSubCategory() + "~~"
                + (String.valueOf(cdm.getMrp()) == null ? "0" : String.valueOf(cdm.getMrp()));
        float cMrp = Float.parseFloat(String.valueOf(cdm.getMrp()) == null ? "0" : String.valueOf(cdm.getMrp())) * Float.parseFloat(cdm.getCaseToPieceQuantity() == null ? "0" : String.valueOf(cdm.getCaseToPieceQuantity()));
        cdm.setCaseMrp(BigDecimal.valueOf(cMrp));
        cdm.setSuggestionText(s);
        return OperationResult.StepResult.OK;
    }

}
