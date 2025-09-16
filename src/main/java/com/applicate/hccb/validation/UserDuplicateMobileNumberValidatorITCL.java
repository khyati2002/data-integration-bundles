package com.applicate.hccb.validation;


import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.User;

public class UserDuplicateMobileNumberValidatorITCL extends AbstractValidationRule<User> {

    String regex = "(^[0-9]{10}$)";

    @SuppressWarnings("all")
    @Override
    public OperationResult.StepResult apply(User cdm) {
        if (SecurityContextUtils.getPrincipal().equalsIgnoreCase("integration_user")) {
            return OperationResult.StepResult.OK;
        }
//		UserService userService = (UserService) ServiceLocator.lookup(User.class);
//		StringBuilder ruleResult = new StringBuilder();
//		com.salescode.dim.jooq.generated.tables.pojos.User user = userService.findByLoginIdUser(cdm.getLoginid());
//		if (cdm.getMobile() != null) {
//			if (user!= null && user.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
//				ruleResult.append("User is inactive in the system");
//				return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
//			}
//		}
//		if (cdm != null && cdm.getDesignation()!=null && cdm.getDesignation().contains("retailer")) {
//			if (cdm.getMobile() != null) {
//				if (!cdm.getMobile().isEmpty() && !checkMobileNumberPattern(cdm.getMobile())) {
//					ruleResult.append("Mobile number field allowed only 10 digit valid number or blank.");
//					return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
//				}
//				if (!StringUtils.isNullOrBlank(cdm.getMobile()) && cdm.getDesignation().contains("retailer")) {
//					Optional<List<User>> currentUsersObject = userService.findByMobileSafely(cdm.getMobile());
//					if (currentUsersObject.isPresent()) {
//						List<User> currentUsers = currentUsersObject.get();
//						for (User currentUser : currentUsers) {
//							if (!(currentUser.getLoginid().equals(cdm.getLoginid())) && currentUser.getDesignation().contains("retailer")) {
//								return new OperationResult.StepResult(OperationResult.Status.ERROR, "Entered mobile number is already registered with another user: " + currentUser.getLoginid()+". Please try with a different number.");
//							}
//						}
//					}
//				}
//			}
//		}
        return OperationResult.StepResult.OK;
    }

}
