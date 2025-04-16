package com.applicate.unnati.validation;


import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletDetails;

import java.util.regex.Pattern;


public class DuplicateMobileNumberValidatorITCL extends AbstractValidationRule<OutletDetails> {

    String regex = "(^[0-9]{10}$)";


    @SuppressWarnings("all")
    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {
        if(SecurityContextUtils.getPrincipal().equalsIgnoreCase("integration_user")) {
            return OperationResult.StepResult.OK;
        }
//        UserService userService = (UserService) ServiceLocator.lookup(User.class);
//        StringBuilder ruleResult = new StringBuilder();
//        com.salescode.dim.jooq.generated.tables.pojos.User user = userService.findByLoginId(cdm.getOutletcode());
//        Set<String> designation = userService.getDesignation(cdm.getOutletcode());
//        if (user != null && user.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
//            ruleResult.append("User is inactive in the system");
//            return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
//        }
//        if (user != null && designation != null && designation.contains("retailer")) {
//            if (cdm.getUserName().getMobile() != null) {
//                if (!cdm.getUserName().getMobile().isEmpty() && !checkMobileNumberPattern(cdm.getUserName()
//                        .getMobile())) {
//                    ruleResult.append("Mobile number field allowed only 10 digit valid number or blank.");
//                    return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
//                }
//                if (!StringUtils.isNullOrBlank(cdm.getUserName().getMobile())) {
//                    Optional<List<User>> currentUsersObject = userService.findByMobileSafely(cdm.getUserName()
//                            .getMobile());
//                    if (currentUsersObject.isPresent() && !currentUsersObject.get().isEmpty()) {
//                        List<User> currentUsers = currentUsersObject.get();
//                        for (User currentUser : currentUsers) {
//                            if (!(currentUser.getLoginid().equals(user.getLoginid())) && currentUser.getDesignation()
//                                    .contains("retailer")) {
//                                return new OperationResult.StepResult(OperationResult.Status.ERROR, "Entered mobile number is already registered with another user: " + currentUser.getLoginid() + ". Please try with a different number.");
//                            }
//                        }
//                    }
//                }
//            }
//        }
        return OperationResult.StepResult.OK;
    }

    public boolean checkMobileNumberPattern(String mobile) {
        return Pattern.matches(regex, mobile);
    }
}
