package com.applicate.unnati.validation;

import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ScoreDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.generated.tables.pojos.ScoreDetails;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ScoreDetailsValidatorITCL extends AbstractValidationRule<ScoreDetails> {

	public static final String MONTH_TAG = "Month";
	public static final String BRANCH_TAG = "branch";
	public static final String YEAR_TAG = "Year";
	public static final String SUCCESS_TAG = "success";
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public OperationResult.StepResult apply(ScoreDetails score){

		MetaDataService metaDataService= (MetaDataService) ServiceLocator.lookup(Metadata.class);

		List<String> errors = new ArrayList<>();
		TypeReference<List<String>> typeReference = new TypeReference<List<String>>() {};

		List<String> keysToValidate = JSONUtils.parse(
				metaDataService.fetchByValue("validation", "score_float").getDomainValues().toString(),
				typeReference);
		var extdVal = score.getExtendedAttributes();
		try {
			extdVal.fieldNames().forEachRemaining(fieldName -> {
				if (keysToValidate.contains(fieldName) && (extdVal.get(fieldName).asText().isEmpty()
						|| !(extdVal.get(fieldName).asText().matches("^-?[0-9]\\d*(\\.\\d+)?$"))))
					throw new IllegalArgumentException(StringUtils
							.format("Value should be in Number and can't be empty, Kindly check value of Params => "
									+ fieldName + " : {}", extdVal.get(fieldName)));
			});
		} catch (Exception e) {
			return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage());

		}

		String rcsidcheck = rcsidCheck(score);
		if(!rcsidcheck.equals(SUCCESS_TAG))
			return new OperationResult.StepResult(OperationResult.Status.ERROR,rcsidcheck);

		String branchcheck = branchCheck(score);
		if(!branchcheck.endsWith(SUCCESS_TAG))
			return new OperationResult.StepResult(OperationResult.Status.ERROR,branchcheck);

		String monthcheck = monthCheck(score);
		if(!monthcheck.equals(SUCCESS_TAG))
			return new OperationResult.StepResult(OperationResult.Status.ERROR,monthcheck);

		String yearcheck = yearCheck(score);
		if(!yearcheck.equals(SUCCESS_TAG))
			return new OperationResult.StepResult(OperationResult.Status.ERROR,yearcheck);

		String monthvalidationcheck = monthValidationCheck(score);
		if(!monthvalidationcheck.equals(SUCCESS_TAG))
			return new OperationResult.StepResult(OperationResult.Status.ERROR,monthvalidationcheck);

		String dateerrorcheck =	dateErrorCheck(score,errors);
		if(!dateerrorcheck.equals(SUCCESS_TAG))
			return new OperationResult.StepResult(OperationResult.Status.ERROR,dateerrorcheck);
		return OperationResult.StepResult.OK;
	}
	public String rcsidCheck(ScoreDetails score) {
		OutletDetailsService outletDetailsService= (OutletDetailsService)  ServiceLocator.lookup(OutletDetails.class);

		String errorstr = "";
		OutletDetails outletObj = outletDetailsService.findByOutletCode(score.getOutletcode());
		if (outletObj == null) {
			errorstr = "UID is not present in system please check the value ";
		}else if(!outletObj.getOutletCategory().equalsIgnoreCase("loyalty")){
			errorstr = "Loyalty score can not be uploaded for non loyalty outlet.";
		}
		if (!errorstr.equals(""))
			return errorstr;

		return SUCCESS_TAG;
	}
	public String branchCheck(ScoreDetails score) {
		String errorstr = "";
		var extdVal = score.getExtendedAttributes();

		if (extdVal.has(BRANCH_TAG)) {
			String branch = extdVal.get(BRANCH_TAG).asText();
			((ObjectNode) extdVal).remove(BRANCH_TAG);
			if (!StringUtils.isValidString(branch)) {
				errorstr = "branch should not be empty";
			}
		} else {
			errorstr = "branch should not be empty";
		}
		if (!errorstr.equals(""))
			return errorstr;
		return SUCCESS_TAG;
	}

	public String monthCheck(ScoreDetails score) {
		String errorstr = "";
		var extdVal = score.getExtendedAttributes();

		String monthVal = "";
		if (extdVal.has(MONTH_TAG)) {
			monthVal = extdVal.get(MONTH_TAG).asText();
			if (StringUtils.isValidString(monthVal) && monthVal.matches("^[0-9]*$")) {
				int tempVal = Integer.parseInt(monthVal);
				if (!(tempVal > 0 && tempVal < 13)) {
					errorstr = "Month can have value from 1 to 12 only ";
				}
			} else {
				errorstr = "Month should not be empty and it should be only digits";
			}
		} else {
			errorstr = "Month should not be empty ";
		}

		if (!errorstr.equals(""))
			return errorstr;
		return SUCCESS_TAG;

	}
	public String yearCheck(ScoreDetails score) {
		String errorstr = "";
		var extdVal = score.getExtendedAttributes();

		String yearVal = "";
		if (extdVal.has(YEAR_TAG)) {
			yearVal = extdVal.get(YEAR_TAG).asText();
			if (StringUtils.isValidString(yearVal) && yearVal.matches("^[0-9]*$")) {
				if (yearVal.length() != 4) {
					errorstr = "Year should contain extact 4 digits only.";
				}
			} else {
				errorstr = "Year should not be empty and it should be only digits";
			}

		} else {
			errorstr = "Year should not be null or empty ";
		}

		if (!errorstr.equals(""))
			return errorstr;
		return SUCCESS_TAG;
	}

	public String monthValidationCheck(ScoreDetails score) {
		var extdVal = score.getExtendedAttributes();
		String monthVal = extdVal.get(MONTH_TAG).asText();
		((ObjectNode) extdVal).remove(MONTH_TAG);
		String yearVal = extdVal.get(YEAR_TAG).asText();
		((ObjectNode) extdVal).remove(YEAR_TAG);

		try {
			String recordDateStr = "01/" + monthVal + "/" + yearVal;
			Date recordDate = new SimpleDateFormat("dd/MM/yyyy").parse(recordDateStr);

			Calendar cal1 = Calendar.getInstance();
			String currentDateStr = "01/" + (cal1.get(Calendar.MONTH) + 1) + "/" + cal1.get(Calendar.YEAR);
			Date currentDate = new SimpleDateFormat("dd/MM/yyyy").parse(currentDateStr);

			int result = recordDate.compareTo(currentDate);
			if (result >= 0) {
				throw new IllegalArgumentException("Month should be less than current month");
			}

		} catch (Exception e) {
			return e.getMessage();
		}
		return SUCCESS_TAG;
	}
	public String dateErrorCheck(ScoreDetails score,List<String> errors) {
		ScoreDetailsService scoreDetailsService =(ScoreDetailsService) ServiceLocator.lookup(ScoreDetails.class);


		if (score.getFeature().equalsIgnoreCase("loyalty")) {
			String errorstr = "";


			try {
				if (scoreDetailsService.pendingStatusValidator(score, errors) && !errors.isEmpty()) {
					errorstr = StringUtils.format("Please first change pending status for historical data.");
					logger.error(errorstr);
					return errorstr;
				}
			} catch (Exception e) {

				e.printStackTrace();
			}
		}

			return SUCCESS_TAG;

}

}
