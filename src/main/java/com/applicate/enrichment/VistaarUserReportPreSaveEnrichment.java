package com.applicate.enrichment;

import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.etl.OperationResult;

public class VistaarUserReportPreSaveEnrichment extends AbstractEnrichment<User> {

	@Override
	public OperationResult.StepResult apply(User user) {

		if (user.getDesignation().contains("reportadmin")) {
			user.setAssignedHierarchy("admin@applicate.in");
		}
		return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
	}

}
