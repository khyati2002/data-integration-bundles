package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.EntityParentMapping;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.services.EntityParentMappingService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.utils.StringUtils;


public class ParentMappingEnrichmentCokeSA extends AbstractEnrichment<User> {

    private final EntityParentMappingService entityParentMappingService = SpringContext.getBean(EntityParentMappingService.class);

    @Override
    public EnrichmentResult apply(User user) {

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
            return new EnrichmentResult(Status.ERROR, e.getMessage() + "Missing information for parent entity mapping");
        }
        return new EnrichmentResult(Status.OK, "Supplier entry created in entity parent mapping");
    }
}
