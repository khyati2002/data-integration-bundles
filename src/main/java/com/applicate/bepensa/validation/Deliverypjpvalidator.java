package com.applicate.bepensa.validation;

import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.DeliveryPJP;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;

public class Deliverypjpvalidator extends AbstractValidationRule<DeliveryPJP> {
    private final UserService userService = (UserService) ServiceLocator.lookup(User.class);
    private final OutletDetailsService outletService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);

    @Override
    public OperationResult.StepResult apply(DeliveryPJP deliveryPJP) {
        boolean isSupplierPresent = isSupplierPresent(deliveryPJP.getLoginid());
        boolean isOutletPresent = isOutletPresent(deliveryPJP.getOutletcode());

        if (!isSupplierPresent && !isOutletPresent) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR , "Sales rep and outlet are not present in our system");
        } else if (!isSupplierPresent) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR , "Sales rep is not present in our system");
        } else if (!isOutletPresent) {
            return new OperationResult.StepResult(OperationResult.Status.OK , "Outlet is not present in our system");
        }

        return OperationResult.StepResult.OK;
    }

    private boolean isSupplierPresent(String salesrep) {
        User user = userService.findByLoginId(salesrep);
        if (user != null) {
//            Set<String> designations = user.getDesignation();
//            for (String designation : designations) {
//                if (designation.equalsIgnoreCase("salesrep")) {
//                    return true;
//                }
//            }
            return true;
        }
        return false;
    }

    private boolean isOutletPresent(String outlet) {
        OutletDetails dOutlet = outletService.findByOutletCode(outlet);
        return dOutlet != null;
    }
}
