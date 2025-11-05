package com.applicate.validation;

import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;

public class VistaarUserEvValidation extends AbstractValidationRule<User> {

	@Override
	public OperationResult.StepResult apply(User user) {

		StringBuilder ruleResult = new StringBuilder();

		if (user.getUseraccountid() == null && (user.getName() == null || user.getName().isEmpty()))
			ruleResult.append("The user is not present in the database");

		if (ruleResult.length() > 0) {
			return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
		} else {
			return OperationResult.StepResult.OK;
		}
	}


}
