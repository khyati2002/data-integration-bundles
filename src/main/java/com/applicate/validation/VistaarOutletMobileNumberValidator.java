package com.applicate.validation;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class VistaarOutletMobileNumberValidator extends AbstractValidationRule<OutletDetails> {

	private static final String REGEX = "(^[0-9]{10}$)";
	private static final String PRIMARY = "STOCKIST";

	@Override
	public OperationResult.StepResult apply(OutletDetails cdm) {
		StringBuilder ruleResult = new StringBuilder();
		boolean isAlpha = isAlphaOutlet(cdm);

		if (!StringUtils.isNullOrBlank(cdm.getContactno())) {
			if (!checkMobileNumberPattern(cdm.getContactno())) {
				ruleResult.append("Given mobile number of the user did not match the required validations. Mobile number should exactly contain 10 numeric values.");
				return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
			}

			if (isAlpha && !cdm.getOutletType().equalsIgnoreCase(PRIMARY)) {
				OperationResult.StepResult duplicateCheck = checkForDuplicates(cdm);
				if (duplicateCheck != OperationResult.StepResult.OK) {
					return duplicateCheck;
				}
			}
		}
		else {
			if (isAlpha && !cdm.getOutletType().equalsIgnoreCase(PRIMARY) && cdm.getActiveStatus() == ActiveStatus.ACTIVE) {
				return new OperationResult.StepResult(OperationResult.Status.ERROR, "Mobile numbers of secondary outlets cannot be null");
			}
		}

		return OperationResult.StepResult.OK;
	}

	/**
	 * Check no. of digits in mobile number
	 * @param mobile
	 * @return
	 */
	private boolean checkMobileNumberPattern(String mobile) {
		RegexValidation regexValidation = new RegexValidation();
		return regexValidation.match(REGEX, mobile);
	}

	/**
	 * Check if the contact number is already used by another active outlet.
	 */
	private OperationResult.StepResult checkForDuplicates(OutletDetails outlet) {
		// Only validate duplicates if the outlet itself is ACTIVE
		if (outlet.getActiveStatus() != ActiveStatus.ACTIVE) {
			return OperationResult.StepResult.OK;
		}

		OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
		List<OutletDetails> existingOutlets = outletDetailsService.findByContactNumber(outlet.getContactno());

		for (OutletDetails existing : existingOutlets) {
			if (!existing.getOutletcode().equals(outlet.getOutletcode())
					&& existing.getActiveStatus() == ActiveStatus.ACTIVE
					&& !existing.getOutletType().equalsIgnoreCase(PRIMARY)) {
				return new OperationResult.StepResult(OperationResult.Status.ERROR,
						"Entered mobile number is already registered with another outlet. Please try with a different number.");
			}
		}
		return OperationResult.StepResult.OK;
	}

	/**
	 * Checks if an outlet is an "alpha" outlet by checking if the 'isAlpha' flag
	 * in its extendedAttributes is either the boolean true or the string "true".
	 *
	 * @param outlet The OutletDetails object.
	 * @return true if the condition is met, otherwise false.
	 */
	private boolean isAlphaOutlet(OutletDetails outlet) {
		if (outlet == null) {
			return false;
		}
		try {
			JsonNode extendedAttributes = outlet.getExtendedAttributes();
			if (extendedAttributes != null && extendedAttributes.has("isAlpha")) {
				JsonNode alphaNode = extendedAttributes.get("isAlpha");

				if (alphaNode.isBoolean()) {
					return alphaNode.asBoolean();
				}

				if (alphaNode.isTextual()) {
					return "true".equals(alphaNode.asText());
				}
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}
}