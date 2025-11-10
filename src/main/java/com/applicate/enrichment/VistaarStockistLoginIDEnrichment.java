package com.applicate.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.SupplierMetaDataService;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.SupplierMetaData;
import com.salescode.dim.jooq.impl.User;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class VistaarStockistLoginIDEnrichment extends AbstractEnrichment<User> {
    private static final Logger logger = LoggerFactory.getLogger(VistaarStockistLoginIDEnrichment.class);
    private static final String STOCKIST = "stockist";

    @Override
    public OperationResult.StepResult apply(User user) {
        final UserService userService = (UserService) ServiceLocator.lookup(User.class);
        final SupplierMetaDataService supplierMetaDataService = (SupplierMetaDataService) ServiceLocator.lookup(SupplierMetaData.class);

        List<String> enrichmentMessages = new ArrayList<>();
        boolean wdEnriched = false;

        // 1. Enrich WD User if applicable
        if (user.getDesignation().contains("wd")) {
            enrichWdUser(user, supplierMetaDataService);
            wdEnriched = true;
        }

        // 2. Enrich Stockist User if applicable
        if (isUserStockist(user)) {
            OperationResult.StepResult stockistResult = enrichStockistParent(user, userService);

            if (stockistResult.getStatus() != OperationResult.Status.OK) {
                return stockistResult;
            }
            if (StringUtils.isNotBlank(stockistResult.getMessage())) {
                enrichmentMessages.add(stockistResult.getMessage());
            }
        }

        if (wdEnriched) {
            enrichmentMessages.add(0, "WD metadata enriched");
        }

        String finalMessage = String.join(", ", enrichmentMessages);
        if (finalMessage.isEmpty()) {
            finalMessage = "Data enrichment skipped";
        }

        return new OperationResult.StepResult(OperationResult.Status.OK, finalMessage);
    }

    /**
     * Enriches a "WD" user with default SupplierMetadata.
     */
    private void enrichWdUser(User user, SupplierMetaDataService supplierMetaDataService) {
        ObjectNode extendedAttributes = (ObjectNode) user.getExtendedAttributes();
        extendedAttributes.put("WDType", "WD");
        extendedAttributes.put("WDCode", user.getLoginid());
        extendedAttributes.put("orderFunction", "Y");
        extendedAttributes.put("minOrderValidation", "Y");

        SupplierMetadata supplierMetaData = new SupplierMetadata();
        supplierMetaData.setActiveStatus(ActiveStatus.ACTIVE);
        supplierMetaData.setActiveStatusReason(ActiveStatus.ACTIVE.getStatus());
        supplierMetaData.setId(user.getLoginid());
        supplierMetaData.setMax(1000000);
        supplierMetaData.setMin(0);
        supplierMetaData.setType("amount");
        supplierMetaData.setExtendedAttributes(extendedAttributes);

        com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata supplierInDB = supplierMetaDataService.findById(user.getLoginid());
        if (supplierInDB != null) {
            supplierMetaData.setVersion(supplierInDB.getVersion());
        }

        List<SupplierMetadata> supplierMeta = new ArrayList<>();
        supplierMeta.add(supplierMetaData);
        user.setSupplierMetaData(supplierMeta);
    }

    /**
     * Enriches a "Stockist" user by verifying and updating their immediate parent's ID.
     * Returns an ERROR StepResult on failure, or an OK StepResult with a message if updates occurred.
     */
    private OperationResult.StepResult enrichStockistParent(User user, UserService userService) {
        boolean isImmediateParentUpdated = false;
        List<HierarchyMetadata> userHierarchyMetadata = user.getImmediateParent();

        if (userHierarchyMetadata == null) {
            return new OperationResult.StepResult(OperationResult.Status.OK, "");
        }

        try {
            for (HierarchyMetadata hierarchyMetaData : userHierarchyMetadata) {
                String immediateParentId = hierarchyMetaData.getImmediateParent();

                // Find the parent by login ID first, then by primary ID
                User parentUser = userService.findByLoginId(immediateParentId);
                if (parentUser == null) {
                    parentUser = userService.findById(immediateParentId);
                }

                if (parentUser == null) {
                    return new OperationResult.StepResult(OperationResult.Status.ERROR, StringUtils.format("User's immediate parent not found {} {}", user, immediateParentId));
                }

                if (!parentUser.getLoginid().equals(immediateParentId)) {
                    isImmediateParentUpdated = true;
                    hierarchyMetaData.setImmediateParent(parentUser.getLoginid());
                }
            }
        } catch (Exception e) {
            logger.error("VistaarStockistLoginIDEnrichment Exception", e);
            return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getLocalizedMessage());
        }

        String message = isImmediateParentUpdated ? "immediate parent updated to loginid" : "";
        return new OperationResult.StepResult(OperationResult.Status.OK, message);
    }

    public boolean isUserStockist(User user) {
        Set<String> designations = user.getDesignation();
        if(designations != null) {
            for(String designation : designations) {
                if(designation.equalsIgnoreCase(STOCKIST)) {
                    return true;
                }
            }
        }
        return false;
    }
}
