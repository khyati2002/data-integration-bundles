package com.applicate.enrichment;

import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;

import java.util.ArrayList;
import java.util.List;

public class VistaarPsrDeactivatePreSaveEnrichment extends AbstractEnrichment<User> {
	@Override
	public OperationResult.StepResult apply(User user) {
		List<String> enrichmentResult = new ArrayList<>();
		enrichmentResult.add(enrichMobileNumber(user));
		return new OperationResult.StepResult(OperationResult.Status.OK, String.join(",", enrichmentResult));
	}

	public String enrichMobileNumber(User user) {
		String enrichmentMsg = "PSR mobile enrichment skipped";
		if (user.getDesignation().contains("psr") && user.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
			user.setMobile(null);
			enrichmentMsg = "PSR mobile enrichment successful";
		}
		return enrichmentMsg;
	}
}
