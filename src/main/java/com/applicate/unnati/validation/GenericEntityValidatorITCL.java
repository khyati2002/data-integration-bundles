package com.applicate.unnati.validation;

import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.impl.GenericEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class GenericEntityValidatorITCL extends AbstractValidationRule<GenericEntity> {
	public static final String MOBILE_TAG = "MobileNumber";

	@Override
	public OperationResult.StepResult apply(GenericEntity cdm) {

		MetaDataService metaDataService = (MetaDataService) ServiceLocator.lookup(Metadata.class);


		if (!cdm.getName().equals("E-Retailer")) {
			return OperationResult.StepResult.OK;
		}
		var ruleResultError = new StringBuilder();

		var mobileNumberError = new StringBuilder();

		var regexValidation = new RegexValidation();

		var regexNum = "^[0-9]*";

		var recommendedSlabNumberCheck = new StringBuilder();

		if (cdm.getPayload().has("recommendedSlab") && !validatePattern(regexNum, cdm.getPayload().get("recommendedSlab").asText()))
			recommendedSlabNumberCheck.append("only integer values are allowed in RecommendedSlab");

		checkForMandatoryColumns(cdm, ruleResultError);
		if (cdm.getPayload().has(MOBILE_TAG) && !cdm.getPayload().get(MOBILE_TAG).asText().isEmpty() && (cdm.getPayload().get(MOBILE_TAG).asText().length() != 10) || !(regexValidation.match(regexNum, cdm.getPayload().get(MOBILE_TAG).asText()))) {
			mobileNumberError.append("Mobileno column (allow only 10 digits)");
		}

		@SuppressWarnings("unchecked") List<String> keysToValidate = JSONUtils.parse(metaDataService.fetchByValue("genericObjectValidation", "loyaltyEnrollment", true).getDomainValues().toString(), ArrayList.class);

		Optional<String> typeOfProgram = checkTypeOfProgram(cdm, keysToValidate);

		String resultError = ((ruleResultError.length() > 0) ? (ruleResultError.toString() + "Cannot be empty. ") : "") + ((typeOfProgram.isEmpty()) ? ("TypeOfProgram : Field must have any one of : " + keysToValidate + ". ") : "") + ((mobileNumberError.length() > 0) ? mobileNumberError.toString() + ". " : "") + ((recommendedSlabNumberCheck.length() > 0) ? recommendedSlabNumberCheck.toString() : "");

		if (resultError.length() > 0)
			return new OperationResult.StepResult(OperationResult.Status.ERROR, " " + resultError);
		else return OperationResult.StepResult.OK;
	}

	public boolean validatePattern(String pattern, String value) {
		var regexValidation = new RegexValidation();
		return regexValidation.match(pattern, value);
	}

	private Optional<String> checkTypeOfProgram(GenericEntity cdm, List<String> keysToValidate) {
		return keysToValidate.stream().filter(program -> program.equals(cdm.getKey2())).findAny();
	}

	private StringBuilder checkForMandatoryColumns(GenericEntity cdm, StringBuilder ruleResultError) {
		if (cdm.getKey3().isBlank()) ruleResultError.append("UID ");

		if (cdm.getKey5().isBlank()) ruleResultError.append("WDCode ");

		if (cdm.getKey4().isBlank()) ruleResultError.append("SifyID ");

		if (cdm.getKey1().isBlank()) ruleResultError.append("Branch ");

		if (cdm.getPayload().get("OutletName").asText().isEmpty()) ruleResultError.append("OutletName ");

		return ruleResultError;
	}
}
