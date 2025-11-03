/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService.TransformationException;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flink-compatible ProductDetailsTransformer
 * Adapted from Spring-based service to use jOOQ and Flink ETL framework
 *
 * @author Manish Srivastava (Original)
 * @author System (Adapted for Flink)
 * @since Sept 2020
 */
public class ProductDetailsTransformer extends AbstractTransformer<Map<String, Object>, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ProductDetailsTransformer.class);
    
    /** The Constant batch_code_key. */
    private static final String BATCH_CODE_KEY = "batchCode";
    
    /** Template compilation cache for Jolt transformations */
    private final Map<String, Chainr> templateCompilationCache = new ConcurrentHashMap<>();

    /**
     * Transform using Jolt specification and prepare batch code if needed.
     *
     * @param s the input data map
     * @return the transformed object
     */
    @Override
    public Object transform(Map<String, Object> s) {
        TransformerInfo transformerInfo = this.getTransformerInfo();
        
        if (transformerInfo == null || transformerInfo.getCode() == null) {
            throw new TransformationException("TransformerInfo or code specification is null");
        }
        
        // Parse the Jolt specification from the database JSON field
        String code = transformerInfo.getCode().data();
        ArrayNode codeNode;
        try {
            codeNode = JSONUtils.getObjectMapper().readValue(code, ArrayNode.class);
        } catch (Exception e) {
            throw new TransformationException("Failed to parse Jolt specification: " + e.getMessage(), e);
        }
        if (NullUtils.isNotNull(codeNode) && NullUtils.isNotNull(s)) {
            try {
                // Use cached Jolt transformation
                String transformerId = transformerInfo.getId();
                Chainr chainr = templateCompilationCache.computeIfAbsent(transformerId, 
                    id -> {
                        Object spec = JsonUtils.jsonToObject(String.valueOf(codeNode));
                        return Chainr.fromSpec(spec);
                    }
                );
                
                Object transformedOutput = chainr.transform(s);
                Map<String, Object> result = JSONUtils.getObjectMapper().readValue(
                    JsonUtils.toPrettyJsonString(transformedOutput),
                    new TypeReference<Map<String, Object>>() {}
                );
                
                if (result != null && !result.isEmpty()) {
                    // Check if batchCode needs to be prepared
                    if (!result.containsKey(BATCH_CODE_KEY)) {
                        prepareBatchCode(result);
                    }
                    return result;
                } else {
                    throw new TransformationException("Transformer Error: Transformed output is null or empty");
                }
            } catch (Exception ex) {
                logger.error("Jolt Transformer Exception: {}", ex.getLocalizedMessage(), ex);
                throw new TransformationException("Transformer Error: Jolt Transformer Exception . Reason : " + ex.getLocalizedMessage(), ex);
            }
        } else {
            throw new TransformationException("Either jolt specification/input json found null");
        }
    }

    /**
     * Prepare batch code using ProductDetailsService.
     * This method adapts the original Spring-based service call to work with Flink's service locator.
     *
     * @param result the result map to prepare batch code for
     */
    private void prepareBatchCode(Map<String, Object> result) {
        try {
            // Check if DSLContext is available
            if (AbstractCDMService.getDslContext() == null) {
                throw new TransformationException("DSLContext is not initialized. Make sure ServiceLocator is properly set up.");
            }

            // Get ProductDetailsService through ServiceLocator
            ProductDetailsService productDetailsService = (ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);
            if (productDetailsService == null) {
                throw new TransformationException("ProductDetailsService not found in ServiceLocator");
            }

            // Convert map to ProductDetails object for batch code preparation
            ProductDetails productDetails = JSONUtils.getObjectMapper().convertValue(result, ProductDetails.class);
            
            // Call the service method to fill batch code
            productDetailsService.fillBatchCode(productDetails);
            
            // Update the result map with the generated batch code
            if (productDetails.getBatchCode() != null) {
                result.put(BATCH_CODE_KEY, productDetails.getBatchCode());
            }
            
            logger.debug("Batch code prepared successfully: {}", productDetails.getBatchCode());
            
        } catch (Exception e) {
            logger.error("Error preparing batch code: {}", e.getMessage(), e);
            throw new TransformationException("Failed to prepare batch code: " + e.getMessage(), e);
        }
    }
}