package com.applicate.lbpl.enrichment;

import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.User;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Set;

public class SupplierCategoryEnrichmentLBPL extends AbstractEnrichment<User> {


    @Override
    public EnrichmentResult apply(User userObject) {

        if(userObject.getDesignation().contains("supplier") && ObjectUtils.isNotEmpty(userObject.getExtendedAttributes().get("categorycode4"))) {
            String categoryCode4 = userObject.getExtendedAttributes().get("categorycode4").asText();
            if(categoryCode4.equalsIgnoreCase("7")){
                userObject.setDesignation(Set.of("retailer", "supplier"));
            }
        }
        return OperationResult.StepResult(OperationResult.Status.OK, "User data enriched.");
    }
}

