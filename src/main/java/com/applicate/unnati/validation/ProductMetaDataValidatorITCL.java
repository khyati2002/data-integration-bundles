package com.applicate.unnati.validation;

import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;

import com.salescode.dim.jooq.impl.ProductDetails;
import com.salescode.dim.jooq.impl.ProductMetaData;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ProductMetaDataValidatorITCL extends AbstractValidationRule<ProductMetaData> {

	String decimalRegex = "^([0-9]*\\.)+?[0-9]+$";
	String integerRegex = "(^[0-9]*$)";
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public OperationResult.StepResult apply(ProductMetaData cdm) {
		ProductDetailsService productDetailsService = (ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);
		UserService userService = (UserService) ServiceLocator.lookup(User.class);

		List<String> errors = new ArrayList<>();

		if (cdm.getSkuCode() == null || cdm.getSkuCode().isEmpty()) {
			errors.add("skuCode should not be null or empty and present in Product Details");
		} else {
			if (!productDetailsService.checkIfBatchCodeExists(cdm.getBatchCode())) {
				errors.add("skuCode should be present in Product Details");
			}
		}

		JsonNode extendAttr = cdm.getExtendedAttributes();
		if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("casePtr")) {
			String valueAttr = extendAttr.get("casePtr").asText();
			if (valueAttr == null || valueAttr.isEmpty()) {
				errors.add("casePtr should not be null or empty");
			}
			if (!validatePattern(decimalRegex, valueAttr)) {
				errors.add("only decimal value are allowed in casePtr");
			}
		} else {
			errors.add("casePtr should not be null or empty");
		}
		if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("packPtr")) {
			String valueAttr = extendAttr.get("packPtr").asText();
			if (valueAttr == null || valueAttr.isEmpty()) {
				errors.add("packPtr should not be null or empty");
			}
			if (!validatePattern(decimalRegex, valueAttr)) {
				errors.add("only decimal value are allowed in packPtr");
			}
		} else {
			errors.add("packPtr should not be null or empty");
		}
		if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("mrp")) {
			String valueAttr = extendAttr.get("mrp").asText();
			if (valueAttr == null || valueAttr.isEmpty()) {
				errors.add("mrp should not be null or empty");
			}
			if (!validatePattern(decimalRegex, valueAttr)) {
				errors.add("only decimal value are allowed in mrp");
			}
		} else {
			errors.add("mrp should not be null or empty");
		}
		if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("tax")) {
			String valueAttr = extendAttr.get("tax").asText();
			if (valueAttr == null || valueAttr.isEmpty()) {
				errors.add("tax should not be null or empty");
			}
			if (!validatePattern(decimalRegex, valueAttr)) {
				errors.add("only decimal value are allowed in tax");
			}
		} else {
			errors.add("tax should not be null or empty");
		}
		if (!ObjectUtils.isEmpty(extendAttr) && extendAttr.has("caseToPieceQuantity")) {
			String valueAttr = extendAttr.get("caseToPieceQuantity").asText();
			if (valueAttr == null || valueAttr.isEmpty()) {
				errors.add("caseToPieceQuantity should not be null or empty");
			}
			if (!validatePattern(integerRegex, valueAttr)) {
				errors.add("only int value are allowed in caseToPieceQuantity");
			}
		} else {
			errors.add("caseToPieceQuantity should not be null or empty");
		}

		if (cdm.getLoginid() == null) {
			errors.add("supplier should not be null or empty");
		} else {
			User user = userService.findByLoginId(cdm.getLoginid());
			if (user == null) {
				errors.add("supplier not present in database.");
			}
		}

		if (errors.size() > 0) {
			String errorstr = StringUtils.format("Validation error occured for Product Metadata. Kindly go through provided errors and make sure those conditions should fulfill while retrying. {}", org.apache.commons.lang3.StringUtils.join(errors, ", "));
			logger.error(errorstr);
			return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
		}
		return OperationResult.StepResult.OK;
	}

	public boolean validatePattern(String pattern, String value) {
		RegexValidation regexValidation = new RegexValidation();
		return regexValidation.match(pattern, value);

	}

}
