package com.applicate.validation;

import java.util.ArrayList;
import java.util.List;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.applicate.services.channelkart.utils.StringUtils;

public class VistaarProductDetailsValidation extends AbstractValidationRule<ProductDetails> {

    private static final Logger logger = LoggerFactory.getLogger(VistaarProductDetailsValidation.class);

    @Override
    public OperationResult.StepResult apply(ProductDetails cdm) {
        List<String> errors = new ArrayList<>();

        if (cdm.getCaseToPieceQuantity() == null) {
            errors.add("PacIncfcs should not be null or empty");
        }

        if (StringUtils.isNullOrBlank(cdm.getSkuCode())) {
            errors.add("skuCode should not be null or empty");
        }
        if (StringUtils.isNullOrBlank(cdm.getSkuName())) {
            errors.add("SSKU_NAME should not be null or empty");
        }
        if (StringUtils.isNullOrBlank(cdm.getMarketSku())) {
            errors.add("MSKU_NAME should not be null or empty");
        }
        if (StringUtils.isNullOrBlank(cdm.getMarketSkuCode())) {
            errors.add("MSKU_CODE should not be null or empty");
        }
        if (StringUtils.isNullOrBlank(cdm.getCategory())) {
            errors.add("CAT_NAME should not be null or empty");
        }
        if (StringUtils.isNullOrBlank(cdm.getCategoryCode())) {
            errors.add("CAT_CODE should not be null or empty");
        }
        if (StringUtils.isNullOrBlank(cdm.getSubCategory())) {
            errors.add("SUBCAT_NAME should not be null or empty");
        }
        if (StringUtils.isNullOrBlank(cdm.getSubCategoryCode())) {
            errors.add("SUBCAT_CODE should not be null or empty");
        }
        if (StringUtils.isNullOrBlank(cdm.getBrand())) {
            errors.add("BRAND_NAME should not be null or empty");
        }
        if (StringUtils.isNullOrBlank(cdm.getBrandCode())) {
            errors.add("brandCode should not be null or empty");
        }

        if (!errors.isEmpty()) {
            String errorstr = StringUtils.format(
                    "Validation error occured for ProductDetails : {}. Kindly go through provided errors and make sure those conditions should fulfill while retrying. {}",
                    cdm.getSkuCode(), String.join(",", errors));
            logger.error(errorstr);
            return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
        }
        return OperationResult.StepResult.OK;
    }
}