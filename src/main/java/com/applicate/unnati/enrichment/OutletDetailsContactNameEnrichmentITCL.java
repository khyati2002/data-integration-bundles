package com.applicate.unnati.enrichment;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.jooq.impl.OutletDetails;

public class OutletDetailsContactNameEnrichmentITCL extends AbstractEnrichment<OutletDetails>{

	@Override
	public EnrichmentResult apply(OutletDetails cdm){
		if (cdm.getContactName() != null) {
			String sanitized = cdm.getContactName().replaceAll("[^a-zA-Z0-9 _-]", "");
			cdm.setContactName(sanitized);
		}
		return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");
	}
}