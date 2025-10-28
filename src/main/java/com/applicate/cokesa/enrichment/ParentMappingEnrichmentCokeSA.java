package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.services.EntityParentMappingService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.jooq.impl.EntityParentMapping;


public class ParentMappingEnrichmentCokeSA extends AbstractEnrichment<User> {

    private final EntityParentMappingService entityParentMappingService = (EntityParentMappingService) ServiceLocator.lookup(EntityParentMapping.class);

    @Override
    public OperationResult.StepResult apply(User user) {

        try {
            if(StringUtils.isEqual(user.getDesignation().toString(), "[supplier]", true)){
                EntityParentMapping entityParentMapping = new EntityParentMapping();
                entityParentMapping.setId(user.getLoginId() + "-" +user.getImmediateParent().get(0).getImmediateParent());
                entityParentMapping.setUser(user.getLoginId());
                entityParentMapping.setDesignation("supplier");
                entityParentMapping.setParent(user.getImmediateParent().get(0).getImmediateParent());
                entityParentMapping.setParentDesignation("supervisor");
                entityParentMappingService.save(entityParentMappingService.refresh(entityParentMapping));
            }
        }
        catch (Exception e){
            return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage() + "Missing information for parent entity mapping");
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, "Supplier entry created in entity parent mapping");
    }
}
