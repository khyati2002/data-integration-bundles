package com.applicate.validation;

import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.commons.lang3.StringUtils;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.ProductMetaData;
import com.salescode.dim.jooq.impl.User;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VistaarProductMetadataValidator extends AbstractValidationRule<ProductMetaData> {
	public static final String ONLY_DECIMAL_VALUE_ARE_ALLOWED_IN = "only decimal value are allowed in ";
	String decimalRegex = "^([0-9]*\\.)[0-9]+$";
	String integerRegex = "(^[0-9]*$)";
	private static final String CASEPTS = "CFCPTS";
	private static final String PACKPTS = "PACPTS";
	private static final String SBU = "SBU";
	private static final String SUB_CAT_CODE = "SubCatCode";
	private static final String SUB_CAT_NAME = "SubCatName";
	private static final String MKTSKUCODE = "MktSkuCode";
	private static final String MKTSKUNAME = "MktSkuName";
	private static final String BRANDCODE = "BrandCode";
	private static final String BRANDNAME = "BrandName";
	private static final String CATCODE = "CatCode";
	private static final String CATNAME = "CatName";

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Entry point for validation. Runs all validation checks and returns aggregated result.
	 */
	@Override
	public OperationResult.StepResult apply(ProductMetaData cdm) {
		List<String> errors = new ArrayList<>();

		validateMandatoryFields(cdm, errors);
		validateNumericFields(cdm, errors);
		validateExtendedAttributes(cdm, errors);
		validateSupplier(cdm, errors);
		validateProductDetails(cdm, errors);

		if (!errors.isEmpty()) {
			String errorStr = String.format(
					"Validation error occurred for Product Metadata. Kindly go through provided errors and make sure those conditions should fulfill while retrying. %s",
					StringUtils.join(errors, ", "));
			logger.info(errorStr);
			return new OperationResult.StepResult(OperationResult.Status.ERROR, errorStr);
		}

		return OperationResult.StepResult.OK;
	}

	/**
	 * Validates mandatory fields such as skuCode and batchCode.
	 */
	private void validateMandatoryFields(ProductMetaData cdm, List<String> errors) {
		if (StringUtils.isBlank(cdm.getSkuCode()))
			errors.add("skuCode should not be null or empty and present in Product metadata");

		if (StringUtils.isBlank(cdm.getBatchCode()))
			errors.add("batchcode should not be null or empty and present in Product metadata");
	}

	/**
	 * Validates numeric fields including CasePtr, PackPtr, MRP, and tax.
	 */
	private void validateNumericFields(ProductMetaData cdm, List<String> errors) {
		validateDecimalField(cdm.getCasePtr(), "CasePtr", errors);
		validateDecimalField(cdm.getPackPtr(), "PackPtr", errors);

		if (cdm.getMrp() == null || cdm.getMrp().compareTo(BigDecimal.ZERO) == 0) {
			errors.add("mrp should not be null or empty in Product metadata");
		} else if (!validatePattern(decimalRegex, cdm.getMrp().toString()) &&
				!validatePattern(integerRegex, cdm.getMrp().toString())) {
			errors.add("only decimal or integer values are allowed in mrp");
		}

		if (StringUtils.isBlank(cdm.getTax())) {
			errors.add("tax should not be null or empty in Product metadata");
		} else if (!validatePattern(decimalRegex, cdm.getTax()) &&
				!validatePattern(integerRegex, cdm.getTax())) {
			errors.add("only decimal or integer values allowed in tax");
		}
	}

	/**
	 * Helper to validate decimal fields where only decimal numbers are allowed.
	 */
	private void validateDecimalField(BigDecimal value, String fieldName, List<String> errors) {
		if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
			errors.add(fieldName + " should not be null or empty in Product metadata");
		} else if (!validatePattern(decimalRegex, value.toString())) {
			errors.add("only decimal values are allowed in " + fieldName);
		}
	}

	/**
	 * Validates extended attributes JSON fields and their required checks.
	 */
	private void validateExtendedAttributes(ProductMetaData cdm, List<String> errors) {
		JsonNode extendAttr = cdm.getExtendedAttributes();

		if (extendAttr == null || extendAttr.isEmpty()) {
			errors.add(SBU + " fields missing");
			return;
		}

		validateExtendedNumericField(extendAttr, CASEPTS, errors);
		validateExtendedNumericField(extendAttr, PACKPTS, errors);
		validatePACIn1CFC(extendAttr, errors);
		validateSBU(extendAttr, errors);
	}

	/**
	 * Validates that a numeric field in extended attributes contains a number.
	 */
	private void validateExtendedNumericField(JsonNode extendAttr, String field, List<String> errors) {
		if (extendAttr.has(field)) {
			String value = extendAttr.get(field).asText();
			if (StringUtils.isBlank(value) || "null".equalsIgnoreCase(value))
				errors.add(field + " should not be null or empty");
			if (!validatePattern(decimalRegex, value) && !validatePattern(integerRegex, value))
				errors.add(ONLY_DECIMAL_VALUE_ARE_ALLOWED_IN + field);
		}
	}

	/**
	 * Validates PACIn1CFC to ensure it is a valid integer.
	 */
	private void validatePACIn1CFC(JsonNode extendAttr, List<String> errors) {
		if (extendAttr.has("PACIn1CFC")) {
			String value = extendAttr.get("PACIn1CFC").asText();
			if (StringUtils.isBlank(value) || "null".equalsIgnoreCase(value)) {
				errors.add("PACIn1CFC should not be null or empty");
				return;
			}
			try {
				if (Float.parseFloat(value) % 1 != 0)
					errors.add("only int value are allowed in PACIn1CFC");
			} catch (NumberFormatException e) {
				errors.add("only int value are allowed in PACIn1CFC");
			}
		}
	}

	/**
	 * Validates SBU field within extended attributes.
	 */
	private void validateSBU(JsonNode extendAttr, List<String> errors) {
		if (extendAttr.has(SBU)) {
			String sbu = extendAttr.get(SBU).asText();
			if (StringUtils.isBlank(sbu) || "null".equalsIgnoreCase(sbu))
				errors.add("SBU should not be null or empty");
		} else {
			errors.add(SBU + "cannot be null or empty");
		}
	}

	/**
	 * Validates that supplier (loginId) exists and belongs to WD designation.
	 */
	private void validateSupplier(ProductMetaData cdm, List<String> errors) {
		UserService userService = (UserService) ServiceLocator.lookup(User.class);
		if (cdm.getLoginid() == null) {
			errors.add("supplier should not be null or empty");
			return;
		}

		User user = userService.findByLoginId(cdm.getLoginid());
		if (user == null) {
			errors.add("supplier not present in database " + cdm.getLoginid());
		} else if (!user.hasDesignation("wd")) {
			errors.add("supplier is not wd");
		}
	}

	/**
	 * Validates product metadata against product details stored in DB.
	 */
	private void validateProductDetails(ProductMetaData cdm, List<String> errors) {
		ProductDetailsService productDetailsService = (ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);
		ProductDetails productDetails = productDetailsService.findByBatchCode(cdm.getBatchCode());
		if (productDetails == null) {
			errors.add("product details not found in database for batchcode " + cdm.getBatchCode());
			return;
		}

		String error = validateMismatchProdDetails(productDetails, cdm);
		error += validateOtherFields(cdm);
		if (!error.isEmpty()) errors.add(error);
	}

	/**
	 * Validates mismatch between product details and extended metadata fields.
	 */
	private String validateMismatchProdDetails(ProductDetails productDetails, ProductMetaData cdm) {
		JsonNode extAttr = cdm.getExtendedAttributes();
		StringBuilder error = new StringBuilder();

		validateMismatch(error, extAttr, BRANDCODE, productDetails.getBrandCode(), BRANDCODE);
		validateMismatch(error, extAttr, BRANDNAME, productDetails.getBrand(), BRANDNAME);
		validateMismatch(error, extAttr, CATCODE, productDetails.getCategoryCode(), CATCODE);
		validateMismatch(error, extAttr, CATNAME, productDetails.getCategory(), CATNAME);
		validateMismatch(error, extAttr, SUB_CAT_CODE, productDetails.getSubCategoryCode(), SUB_CAT_CODE);
		validateMismatch(error, extAttr, SUB_CAT_NAME, productDetails.getSubCategory(), SUB_CAT_NAME);
		validateMismatch(error, extAttr, MKTSKUCODE, productDetails.getMarketSkuCode(), MKTSKUCODE);
		validateMismatch(error, extAttr, MKTSKUNAME, productDetails.getMarketSku(), MKTSKUNAME);

		return error.toString();
	}

	/**
	 * Checks if an individual extended attribute matches the expected product detail.
	 */
	private void validateMismatch(StringBuilder error, JsonNode extAttr, String key, String actualValue, String label) {
		String extValue = getValue(extAttr, key);

		if (!StringUtils.isBlank(extValue) && !StringUtils.equalsIgnoreCase(extValue, StringUtils.defaultString(actualValue))) {
			error.append(label).append(" details mismatch with product details. ");
		}
	}

	/**
	 * Validates other required extended attributes to ensure they are not empty.
	 */
	private String validateOtherFields(ProductMetaData cdm) {
		JsonNode extAttr = cdm.getExtendedAttributes();
		StringBuilder error = new StringBuilder();

		if (extAttr == null) {
			return error.toString();
		}

		List<String> keys = Arrays.asList(
				BRANDCODE, BRANDNAME, CATCODE, CATNAME,
				SUB_CAT_CODE, SUB_CAT_NAME, MKTSKUCODE,
				MKTSKUNAME, "SysSkuName"
		);

		for (String key : keys) {
			String value = getValue(extAttr, key);
			if (StringUtils.isBlank(value)) {
				error.append(key).append(" should not be null or empty ");
			}
		}

		return error.toString();
	}

	/**
	 * Retrieves a value from extended attributes, treating "null" (string) as null.
	 */
	private String getValue(JsonNode extAttr, String key) {
		if (extAttr.has(key) && extAttr.get(key).asText().equalsIgnoreCase("null")) {
			return null;
		}
		return extAttr.has(key) ? extAttr.get(key).asText() : null;
	}

	/**
	 * Validates a string value against a regex pattern.
	 */
	public boolean validatePattern(String pattern, String value) {
		RegexValidation regexValidation = new RegexValidation();
		return regexValidation.match(pattern, value);

	}
}
