package com.applicate.unnati.validation;

import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flink-compatible ProductDetailsValidator for ITCL
 * Adapted from Spring-based service to use jOOQ and ServiceLocator pattern
 * 
 * Validates product details including image file name format and blob handling
 * for ImageMaster processing in the ITCL context.
 * 
 * @author System (Adapted for Flink)
 * @since Oct 2025
 */
public class ProductDetailsValidatorITCL extends AbstractValidationRule<ProductDetails> {

    /** The logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Apply validation rules to ProductDetails.
     *
     * @param cdm the ProductDetails object to validate
     * @return the validation result
     */
    @Override
    public OperationResult.StepResult apply(ProductDetails cdm) {
        try {
            RegexValidation regexValidation = new RegexValidation();
            StringBuilder ruleResult = new StringBuilder();
            String fileNameRegex = "([^&%#]*?)(.jpg|.gif|.png)$";
            
            JsonNode extendedAttributes = cdm.getExtendedAttributes();
            
            // Check if ImageMaster processing is required
            if (extendedAttributes != null && extendedAttributes.has("ImageMaster")) {
                
                // Remove ImageMaster flag as it's processed
                if (extendedAttributes instanceof ObjectNode) {
                    ((ObjectNode) extendedAttributes).remove("ImageMaster");
                }
                
                // Check if blob update is required
                if (extendedAttributes.has("updateBlob")) {
                    if (extendedAttributes instanceof ObjectNode) {
                        ((ObjectNode) extendedAttributes).remove("updateBlob");
                    }
                    // Reset blob key when updating blob
                    cdm.setBlobKey(null);
                }
                
                // Validate file name format
                String fileName = cdm.getFileName();
                if (fileName == null || fileName.isBlank() || !regexValidation.match(fileNameRegex, fileName)) {
                    ruleResult.append(
                        "Field FileName must have only alphanumeric character and should not contain special character like (&%#) should end with extension (.jpg|.gif|.png)");
                    return new OperationResult.StepResult(OperationResult.Status.ERROR, ruleResult.toString());
                }
            }
            
            return OperationResult.StepResult.OK;
            
        } catch (Exception e) {
            logger.error("Error during ProductDetails validation: {}", e.getMessage(), e);
            return new OperationResult.StepResult(OperationResult.Status.ERROR, 
                "Validation failed due to unexpected error: " + e.getMessage());
        }
    }
}