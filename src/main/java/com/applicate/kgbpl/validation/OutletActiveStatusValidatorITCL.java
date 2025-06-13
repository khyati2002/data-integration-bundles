package com.applicate.unnati.validation;


import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Checks if the Outlet is being activated.
 * If activeStatus is being changed to Active, throw error message.
 */
public class OutletActiveStatusValidatorITCL extends AbstractValidationRule<OutletDetails> {


    public OperationResult.StepResult apply(OutletDetails cdm) {

        OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);

        OutletDetails dbRecord = outletDetailsService.findByOutletCode(cdm.getOutletcode());

        if ("auto_generated".equals(cdm.getOutletcode())) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "null values are not allowed in outletCode column");
        }

//        if (ObjectUtils.isEmpty(dbRecord)) {
//            return new OperationResult.StepResult(OperationResult.Status.ERROR, "UID is not present in our system");
//        }

        if (dbRecord.getActiveStatus().equals(ActiveStatus.INACTIVE) && cdm.getActiveStatus()
                .equals(ActiveStatus.ACTIVE)) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "User activation access is blocked for admin users.");
        }

        return OperationResult.StepResult.OK;
    }

}
