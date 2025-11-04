package com.applicate.enrichment;

import java.util.Set;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.User;

public class VistaarverifiedEnrichment extends AbstractEnrichment<User> {

	@Override
	public OperationResult.StepResult apply(User user) {

		if (!user.getDesignation().isEmpty()) {
			Set<String> userDesignation = user.getDesignation();
			Object[] des = userDesignation.toArray();
			if (des[0].toString().equalsIgnoreCase("DISTRICT") || des[0].toString().equalsIgnoreCase("BRANCH") || des[0].toString().equalsIgnoreCase("REPORTADMIN")) {
				user.setVerified((byte) 1);
				return new OperationResult.StepResult(OperationResult.Status.OK, "District or Branch verified status has been updated to true");
			} else {
				return new OperationResult.StepResult(OperationResult.Status.OK, "Verified Enrichment Skipped");
			}
		}
		return new OperationResult.StepResult(OperationResult.Status.OK, "Verified Enrichment Skipped");
	}

}
