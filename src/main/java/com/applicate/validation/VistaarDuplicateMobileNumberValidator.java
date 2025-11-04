package com.applicate.validation;

import java.util.List;
import java.util.Optional;

//import com.applicate.services.channelkart.models.User;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;

public class VistaarDuplicateMobileNumberValidator extends AbstractValidationRule<User> {

	String regex = "(^[0-9]{10}$)";

	@SuppressWarnings("all")
	public OperationResult.StepResult apply(User cdm) {
		UserService userService = (UserService) ServiceLocator.lookup(UserService.class);
		
		StringBuilder ruleResult = new StringBuilder();
		
		if (cdm.getDesignation()!=null) {
			if (cdm.getMobile() != null) {
				if (!cdm.getMobile().isEmpty() && !checkMobileNumberPattern(cdm.getMobile())) {
					ruleResult.append("Mobile number field allowed only 10 digit valid number or blank.");
					return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
				}
				if (!cdm.getMobile().isEmpty()) {
					boolean mobileNumberChanged = false;
					Optional<List<User>> currentUsers = userService.findByMobileSafely(cdm.getMobile());
					if (currentUsers.isPresent()) {
						List<User> users = currentUsers.get();
						for (User obj : users) {
							if (obj.getLoginId().equals(cdm.getLoginId())) {
								mobileNumberChanged = true;
							}
						}
						if (!mobileNumberChanged) {
							for (User obj : users) {
								if (!obj.getLoginId().equals(cdm.getLoginId()) && obj.isActive()) {
									ruleResult.append(
											"Entered mobile number is already registered with another user. Please try with a different number.");
									return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
								}
							}
						}
					}
				}
			}
		}
		if (cdm != null && cdm.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
			ruleResult.append("User is inactive in the system");
		}
		return OperationResult.StepResult.OK;
	}

	public boolean checkMobileNumberPattern(String mobile) {
		RegexValidation regexValidation = new RegexValidation();
		return regexValidation.match(regex, mobile);
	}
}