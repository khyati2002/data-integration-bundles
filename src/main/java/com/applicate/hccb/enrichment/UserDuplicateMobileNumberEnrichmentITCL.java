package com.applicate.hccb.enrichment;


import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.User;

public class UserDuplicateMobileNumberEnrichmentITCL extends AbstractEnrichment<User> {

	@Override
	public OperationResult.StepResult apply(User cdm) {
		if (cdm!=null && cdm.getDesignation().contains("retailer") && (cdm.getMobile() == null || cdm.getMobile().isEmpty())) {
			cdm.setVerified(false);
		}
		return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
	}
}
