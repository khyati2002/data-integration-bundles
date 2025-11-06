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

    private final UserService userService = (UserService) ServiceLocator.lookup(User.class);
    private final SupplierMetaDataService supplierMetaDataService = (SupplierMetaDataService) ServiceLocator.lookup(com.salescode.dim.jooq.impl.SupplierMetaData.class);

    @Override
    public OperationResult.StepResult apply(User user) {
        String enrichmentMessage = "Data enrichment skipped";
        boolean isImmediateParentUpdated = false;

        if(user.getDesignation().contains("wd")) {
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
            if(supplierInDB!=null){
                supplierMetaData.setVersion(supplierInDB.getVersion());
            }

            List<SupplierMetadata> supplierMeta = new ArrayList<>();
            supplierMeta.add(supplierMetaData);
            user.setSupplierMetaData(supplierMeta);
        }
        if(isUserStockist(user)) {
            List<HierarchyMetadata> userHierarchyMetadata = user.getImmediateParent();
            try {
                if(userHierarchyMetadata != null) {
                    for (HierarchyMetadata hierarchyMetaData : userHierarchyMetadata) {
                        String immediateParent = hierarchyMetaData.getImmediateParent();
                        User psrorStockistByIdfromDB = userService.findByLoginId(immediateParent);
                        if (psrorStockistByIdfromDB == null) {
                            psrorStockistByIdfromDB = userService.findById(immediateParent);
                        }
                        if (psrorStockistByIdfromDB != null) {
                            if (!psrorStockistByIdfromDB.getLoginid().equals(immediateParent)) {
                                isImmediateParentUpdated = true;
                                hierarchyMetaData.setImmediateParent(psrorStockistByIdfromDB.getLoginid());
                            }
                        } else {
                            return new OperationResult.StepResult(OperationResult.Status.ERROR, StringUtils.format("User's immediate parent not found {} {}", user, immediateParent));
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("VistaarStockistLoginIDEnrichment Exception", e);
                return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getLocalizedMessage());
            }
            if(isImmediateParentUpdated) {
               enrichmentMessage+=" immediate parent updated to loginid";
            }
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, enrichmentMessage);
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
