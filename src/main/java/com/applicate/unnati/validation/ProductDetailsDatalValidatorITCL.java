package com.applicate.unnati.validation;

import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ProductDetailsDatalValidatorITCL extends AbstractValidationRule<ProductDetails> {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public OperationResult.StepResult apply(ProductDetails cdm) {

		List<String> errors = new ArrayList<>();
		if (cdm.getSkuCode() == null || cdm.getSkuCode().isEmpty()) {
			errors.add("skuCode should not be null or empty");
		}

		if (cdm.getActiveStatus() != null && !cdm.getActiveStatus().getStatus().matches("^(active|inactive)$")) {
			errors.add("Active status should not have values other than active or inactive.");
		}

		if (cdm.getSkuName() == null || cdm.getSkuName().isEmpty()) {
			errors.add("SSKU_NAME should not be null or empty");
		}

		if (cdm.getMarketSku() == null || cdm.getMarketSku().isEmpty()) {
			errors.add("MSKU_NAME should not be null or empty");
		}

		if (cdm.getMarketSkuCode() == null || cdm.getMarketSkuCode().isEmpty()) {
			errors.add("MSKU should not be null or empty");
		}

		if (cdm.getMarketSkuCode() == null || cdm.getMarketSkuCode().isEmpty()) {
			errors.add("marketSkuCode should not be null or empty");
		}

		if (cdm.getCategory() == null || cdm.getCategory().isEmpty()) {
			errors.add("CAT_NAME should not be null or empty");
		}

		if (cdm.getCategoryCode() == null || cdm.getCategoryCode().isEmpty()) {
			errors.add("CAT_CODE should not be null or empty");
		}

		if (cdm.getSubCategory() == null || cdm.getSubCategory().isEmpty()) {
			errors.add("SUBCAT_NAME should not be null or empty");
		}

		if (cdm.getSubCategoryCode() == null || cdm.getSubCategoryCode().isEmpty()) {
			errors.add("SUBCAT should not be null or empty");
		}

		if (cdm.getBrand() == null || cdm.getBrand().isEmpty()) {
			errors.add("BRAND_NAME should not be null or empty");
		}

		if (cdm.getBrandCode() == null || cdm.getBrandCode().isEmpty()) {
			errors.add("BRAND should not be null or empty");
		}

		if (cdm.getBrandCode() == null || cdm.getBrandCode().isEmpty()) {
			errors.add("brandCode should not be null or empty");
		}

		if (errors.size() > 0) {
			String errorstr = StringUtils.format("Validation error occured for ProductDetails : {}. Kindly go through provided errors and make sure those conditions should fulfill while retrying. {}", cdm.getSkuCode(), org.apache.commons.lang3.StringUtils.join(errors, ","));
			logger.error(errorstr);
			return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
		}
		return OperationResult.StepResult.OK;
	}

}
