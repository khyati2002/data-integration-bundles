package com.applicate.hccb.enrichment;


import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.CustomerAccountsService;
import com.applicate.services.channelkart.services.HierarchyMetadataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

public class UserDetailsEnrichmentITCL extends AbstractEnrichment<User> {


    @Override
    public OperationResult.StepResult apply(User cdm) {

        CustomerAccountsService customerAccountsService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccount.class);
        HierarchyMetadataService hierarchyMetaDataService = (HierarchyMetadataService) ServiceLocator.lookup(HierarchyMetadata.class);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (cdm == null) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Enrichment error: User not found null");
        }
//        if (cdm.getPassword() == null) {
//            cdm.setPassword(UserService.DEFAULT_ENCODED_PASSWORD);
//        }
        if (cdm.getLocationHierarchy() == null) {
            cdm.setLocationHierarchy(customerAccountsService.getAdminInfo().getLocationHierarchy());
        }
        if (cdm.getSupplierMetaData() != null && !cdm.getSupplierMetaData().isEmpty()) {
            SupplierMetadata supplierMetaData = cdm.getSupplierMetaData().get(0);
            int min = supplierMetaData.getMin() == null ? 0 : supplierMetaData.getMin();
            int max = supplierMetaData.getMax() == null ? 0 : supplierMetaData.getMax();
            if (min <= 0 && max <= 0 && supplierMetaData.getType() == null) cdm.setSupplierMetaData(new ArrayList<>());
        }
        if (cdm.getActiveStatus() == null || cdm.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
            cdm.setActiveStatus(ActiveStatus.INACTIVE);
            if (StringUtils.isBlank(cdm.getActiveStatusReason()) || !cdm.getActiveStatusReason()
                    .startsWith("Deactivated")) {
                cdm.setActiveStatusReason("Deactivated by " + SecurityContextUtils.getPrincipal() + " on " + dateFormat.format(new Date()));
            }
        } else if (cdm.getActiveStatus().equals(ActiveStatus.ACTIVE)) {
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
            if (StringUtils.isBlank(cdm.getActiveStatusReason()) || cdm.getActiveStatusReason()
                    .startsWith("Deactivated")) {
                cdm.setActiveStatusReason(ActiveStatus.ACTIVE.name());
            }
        }

//            if(cdm.getSsoId() == null || cdm.getSsoId().equals("none")) {
//                Optional<SSOConfiguration> ssoconfig= ssoService.getConfigurationByUserDesignation(cdm);
//                if(ssoconfig != null && ssoconfig.isPresent()) {
//                    cdm.setSsoId(ssoconfig.get().getRegistrationId());
//                }
//            }

        if (cdm.getDesignation().contains("supplier") && ObjectUtils.isEmpty(cdm.getImmediateParent())) {
            User admin = customerAccountsService.getAdminInfo();
            if (!cdm.getLoginid().equalsIgnoreCase(admin.getLoginid())) {
                Collection<HierarchyMetadata> adminMetaData = hierarchyMetaDataService.findByImmediateParent(admin.getLoginid());
                cdm.setImmediateParent(adminMetaData.stream().collect(Collectors.toList()));
                String hierarchy = customerAccountsService.getAdminHierarchy(cdm.getLoginid());
                cdm.setHierarchy(hierarchy);
            }
        }
        return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
    }

}

