package com.applicate.kbpl.validation;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.TempMasterMappingService;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.TempMasterMapping;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

public class PreTransformPJPValidation extends AbstractValidationRule<DeliveryPjp> {

    private static final String TENANT_CODE = "tenantcode";
    OutletDetailsService outletService ;
    UserService userService ;
    TempMasterMappingService masterMappingService ;

    @Override
    public OperationResult.StepResult apply(DeliveryPjp deliveryPJP) {
        outletService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
        userService = (UserService) ServiceLocator.lookup(User.class);
        masterMappingService = (TempMasterMappingService) ServiceLocator.lookup(TempMasterMapping.class);

        List<String> errorList = new ArrayList<>();

        if (!deliveryPJP.getExtendedAttributes().has(TENANT_CODE) || deliveryPJP.getExtendedAttributes().get(TENANT_CODE).asText().equalsIgnoreCase("null")) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Cannot persist PjP without supplier value");
        } else {
            String supplier = deliveryPJP.getExtendedAttributes().get(TENANT_CODE).asText();
            errorList.addAll(verifySupplierStatus(supplier));
            errorList.addAll(verifyUser(deliveryPJP));
            errorList.addAll(verifyOutlet(deliveryPJP, supplier));

            // Validation Failure
            if (!errorList.isEmpty()) {
                String errorStr = StringUtils.format("Error saving PJP , Reason : [{}]", org.apache.commons.lang3.StringUtils.join(errorList, ","));
                return new OperationResult.StepResult(OperationResult.Status.ERROR, errorStr);
            }

            return OperationResult.StepResult.OK;
        }
    }


    /**
     * Performs the following checks :
     * 1. Outlet is present in Outlet Master
     * 2. Outlet has active mapping to any supplier or not
     *
     * @param pjpObject tempPjpOutletCode
     */
    private List<String> verifyOutlet(DeliveryPjp pjpObject, String supplier) {
        List<String> errorList = new ArrayList<>();
        String outletCode = pjpObject.getOutletcode();
        if (ObjectUtils.isEmpty(outletCode)) {
            errorList.add("Cannot create pjp with null or empty outletCode");
        } else {
            if (ObjectUtils.isEmpty(outletService.findByOutletCode(outletCode))) {
                errorList.add("No outlet found with outletCode : " + outletCode);
            } else {
                List<String> activeSuppliers = masterMappingService.getActiveParentFromUserAndFeature(outletCode, "OutletMaster");
                if (activeSuppliers.isEmpty()) {
                    errorList.add(StringUtils.format("No active outlet - supplier mapping for the outlet : {} ", outletCode));
                } else if (!activeSuppliers.contains(supplier)) {
                    errorList.add(StringUtils.format("Supplier {} not mapped with outletCode {}", supplier, outletCode));
                }
            }
        }
        return errorList;
    }


    /**
     * Performs the following checks :
     * 1. User (MGR) is present in User Master
     * 2. User is mapped to any supplier or not.
     * 3. If pjp supplier is present in User's active parents from tempMasterMapping or not.
     *
     * @param pjpObject tempPjpObject
     */
    private List<String> verifyUser(DeliveryPjp pjpObject) {
        List<String> errorList = new ArrayList<>();
        String loginId = pjpObject.getLoginid();
        if (ObjectUtils.isEmpty(loginId)) {
            errorList.add("Cannot create pjp with null or empty loginId");
        } else {
            User existingUser = userService.findByLoginId(loginId);
            if (ObjectUtils.isEmpty(existingUser)) {
                errorList.add("No user found with loginId : " + loginId);
            }
            if (existingUser.getActiveStatus() == null || existingUser.getActiveStatus() != ActiveStatus.ACTIVE) {
                errorList.add("Inactive salesrep found with loginId : " + loginId);
            }
        }
        return errorList;
    }


    /**
     * Checks whether the supplier found in tempPJP is Active in the User Master or not
     *
     * @param supplier Supplier value from TempDeliveryPJP
     * @return List of errors if any during supplier Validation
     */
    private List<String> verifySupplierStatus(String supplier) {
        List<String> errorList = new ArrayList<>();
        User supplierUser = userService.findByLoginId(supplier);
        if(supplierUser == null) {
            errorList.add("No supplier found with loginId : "+ supplier);
        }
        else if(supplierUser.getActiveStatus() == null || supplierUser.getActiveStatus() != ActiveStatus.ACTIVE) {
            errorList.add(StringUtils.format("Inactive supplier {} received for PJP ", supplier));
        }
        return errorList;
    }
}
