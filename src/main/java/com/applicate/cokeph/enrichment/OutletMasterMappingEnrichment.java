package com.applicate.cokeph.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.OutletDetails;

public class OutletMasterMappingEnrichment extends AbstractEnrichment<OutletDetails> {

    private final TempMasterMappingService masterMappingService = SpringContext.getBean(TempMasterMappingService.class);

    @Override
    public EnrichmentResult apply(OutletDetails outletDetails) {
        boolean isRetailer=outletDetails.getUserName()==null || ( outletDetails.getUserName().getDesignation()!=null && outletDetails.getUserName().getDesignation().contains("retailer"));
        if(isRetailer) {
            ObjectNode extended = new ObjectMapper().createObjectNode();

            // Extract current preseller and distributor from the extended attributes
            String currentPreseller = outletDetails.getExtendedAttributes().hasNonNull("preseller")
                    ? outletDetails.getExtendedAttributes().get("preseller").asText()
                    : null;
            String currentDistributor = outletDetails.getExtendedAttributes().hasNonNull("DistributorCode")
                    ? outletDetails.getExtendedAttributes().get("DistributorCode").asText()
                    : null;

            // Initialize TempMasterMapping object
            TempMasterMapping tempMasterMapping = new TempMasterMapping();
            tempMasterMapping.setActiveStatus(outletDetails.getActiveStatus());

            // Handle feature setting based on presence of preseller and outletDivision
            if (currentPreseller != null) {
                tempMasterMapping.setFeature("UserMaster");
                tempMasterMapping.setUserLoginId(currentPreseller);
                tempMasterMapping.setParent(currentDistributor);
                extended.put("DistributorCode", currentDistributor);
                extended.put("OutletCode", outletDetails.getOutletCode());
                extended.put("HierarchyUpdateCheck", 0);
            }
            // Set the extended attributes and save
            tempMasterMapping.setExtendedAttributes(extended);
            TempMasterMapping refreshedObject = masterMappingService.refresh(tempMasterMapping);
            masterMappingService.save(refreshedObject);
        }

        return EnrichmentResult.OK;
    }
}
