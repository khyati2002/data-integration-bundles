package com.applicate.validation;

import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;


public class VistaarProductImageValidation extends AbstractValidationRule<ProductDetails> {

    @Override
    public OperationResult.StepResult apply(ProductDetails productDetails) {
        RegexValidation regexValidation = new RegexValidation();
        StringBuilder ruleResult = new StringBuilder();
        String fileNameRegex = "([^&%#]*?)(.jpg|.gif|.png)$ ";

        if (productDetails.getExtendedAttributes()!= null && productDetails.getExtendedAttributes().has("ImageMaster")) {
            if (productDetails.getExtendedAttributes().isObject()) {
                ((ObjectNode) productDetails.getExtendedAttributes()).remove("ImageMaster");
            }

            if ( !regexValidation.match(fileNameRegex, productDetails.getFileName())) {
                ruleResult.append(
                        "Field FileName must have only alphanumeric character and should not contain special character like (&%#) should end with extension (.jpg|.gif|.png)");
                return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
            }
        }
        return OperationResult.StepResult.OK;
    }
}