package com.applicate.unnati.validation;

import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;

import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.generated.tables.pojos.OutletActivity;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UnnatiOutletAttendanceValidator extends AbstractValidationRule<OutletActivity> {
	private static final String RCSID = "RCSID";
	private static final String SIFYID = "SIFYID";
	private static final String WDNAME = "WDNAME";
	private static final String DSID = "DSID";
	private static final String DSNAME = "DSNAME";
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public OperationResult.StepResult apply(OutletActivity cdm) {
		if (!cdm.getActivity().equals("attendance")) {
			return OperationResult.StepResult.OK;
		}
		final OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
		List<String> errors = new ArrayList<>();
		if (cdm.getOutletcode() == null || cdm.getOutletcode().trim().isEmpty() || cdm.getOutletcode().equals("null")) {
			errors.add("UID is null or empty");
		} else {
			OutletDetails dbOutlet = outletDetailsService.findByOutletCode(cdm.getOutletcode());
			if (dbOutlet == null) {
				errors.add("UID is not present in DB.");
			}
		}
		if (cdm.getLoginid() == null || cdm.getLoginid().trim().isEmpty() || cdm.getLoginid().equals("null")) {
			errors.add("WD Dest is null or empty");
		}
		if (cdm.getExtendedAttributes() != null) {
			if (!cdm.getExtendedAttributes().has(RCSID) || (cdm.getExtendedAttributes().get(RCSID) == null || cdm.getExtendedAttributes().get(RCSID).asText().trim().isEmpty() || cdm.getExtendedAttributes().get(RCSID).asText().trim().equals("null"))) {
				errors.add("RCSID is null or empty.");
			}
			if (!cdm.getExtendedAttributes().has(SIFYID) || (cdm.getExtendedAttributes().get(SIFYID) == null || cdm.getExtendedAttributes().get(SIFYID).asText().trim().isEmpty() || cdm.getExtendedAttributes().get(SIFYID).asText().trim().equals("null"))) {
				errors.add("SifyID is null or empty.");
			}
			if (!cdm.getExtendedAttributes().has(WDNAME) || (cdm.getExtendedAttributes().get(WDNAME) == null || cdm.getExtendedAttributes().get(WDNAME).asText().trim().isEmpty() || cdm.getExtendedAttributes().get(WDNAME).asText().trim().equals("null"))) {
				errors.add("WD Name is null or empty.");
			}
			if (!cdm.getExtendedAttributes().has(DSID) || (cdm.getExtendedAttributes().get(DSID) == null || cdm.getExtendedAttributes().get(DSID).asText().trim().isEmpty() || cdm.getExtendedAttributes().get(DSID).asText().trim().equals("null"))) {
				errors.add("DSID is null or empty.");
			}
			if (!cdm.getExtendedAttributes().has(DSNAME) || (cdm.getExtendedAttributes().get(DSNAME) == null || cdm.getExtendedAttributes().get(DSNAME).asText().trim().isEmpty() || cdm.getExtendedAttributes().get(DSNAME).asText().trim().equals("null"))) {
				errors.add("DS Name is null or empty.");
			}
		}
		if (!errors.isEmpty()) {
			String errorstr = StringUtils.format("Validation error occured for OutletActivity. Kindly go through provided errors and make sure those conditions should fulfill while retrying. {}", String.join(",", errors));
			logger.error(errorstr);
			return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
		}
		return OperationResult.StepResult.OK;
	}
}