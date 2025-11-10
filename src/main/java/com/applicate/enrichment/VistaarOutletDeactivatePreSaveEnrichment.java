package com.applicate.enrichment;

import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.OperationResult;

import java.util.ArrayList;
import java.util.List;

public class VistaarOutletDeactivatePreSaveEnrichment extends AbstractEnrichment<OutletDetails> {

	/**
	 * making contactno null when outlet becomes inactive
	 * @return enrichment result
	 */
	@Override
	public OperationResult.StepResult apply(OutletDetails outletDetails) {
		List<String> enrichmentMessages = new ArrayList<>();
		enrichmentMessages.add(enrichMobileNumber(outletDetails));
		return new OperationResult.StepResult(OperationResult.Status.OK, String.join(",", enrichmentMessages));
	}

	/**
	 * enriching mobile number
	 * @return enrichment message
	 */
	private String enrichMobileNumber(OutletDetails outletDetails) {
		String enrichmentMsg = "Outlet mobile enrichment skipped";
		if (outletDetails.getActiveStatus() != null
				&& outletDetails.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
			outletDetails.setContactno(null); // clear mobile number if inactive
			enrichmentMsg = "Outlet mobile enrichment successful";
		}
		return enrichmentMsg;
	}
}
