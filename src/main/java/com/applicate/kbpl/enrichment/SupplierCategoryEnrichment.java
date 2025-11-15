package com.applicate.kbpl.enrichment;

import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Set;

public class SupplierCategoryEnrichment extends AbstractEnrichment<User> {

    @Override
    public EnrichmentResult apply(User userObject) {
        if (userObject.getDesignation().contains("supplier") && userObject.getExtendedAttributes()!= null && ObjectUtils.isNotEmpty(userObject.getExtendedAttributes().get("parentdistributorcode"))) {
            String parentDistributorCode = userObject.getExtendedAttributes().get("parentdistributorcode").asText();
            if (ObjectUtils.isNotEmpty(parentDistributorCode)) {
                userObject.setDesignation(Set.of("retailer", "supplier"));
            }
        }
        return new OperationResult.StepResult(OperationResult.Status.OK);
    }
}
