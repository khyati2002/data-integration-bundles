package com.applicate.transformer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.transformation.AbstractTransformer;

import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VistaarProductDetailImageTransformer extends AbstractTransformer<Map<String, Object>, Object> {

    private static final Logger logger = LoggerFactory.getLogger(VistaarProductDetailImageTransformer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SKULEVEL_TAG = "skuLevel";
    private static final String FILENAME_TAG = "FileName";
    private static final String SKUCODE_TAG = "skuCode";
    private static final String BATCHCODE_TAG = "batchCode";
    private static final String SSKU_TAG = "SSKU";
    private static final String MSKU_TAG = "MSKU";
    private static final String FILENAME = "file_name";
    private static final String SKUCODE = "sku_code";
    private static final String BATCHCODE = "batch_code";
    private static final String BLOB_KEY = "blob_key";
    private static final String EXTENDED_ATTRIBUTES = "extended_attributes";

    /**
     * Helper class to store parameters for the transform method.
     */
    private static class TransformParams {
        String msku;
        String ssku;
        String defaultBlobKey = "";
        boolean skuLevel = false;
        List<Map<String, Object>> data;
    }

    @Override
    public Object transform(Map<String, Object> s) {

        final ProductDetailsService productDetailsService = (ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);

        TransformParams params = initializeAndFetch(s, productDetailsService);

        if (params.msku != null && !params.msku.isBlank()) {

            if (!params.data.isEmpty()) {
                ObjectNode cdmExtendendeAttribute = objectMapper.createObjectNode().put("ImageMaster", "ImageMaster");
                for (int i = 0; i < params.data.size(); i++) {
                    try {
                        processData(params.data.get(i), params.skuLevel, cdmExtendendeAttribute, params.defaultBlobKey, s);
                    } catch (IOException e) {
                        logger.error("Exception occurred while adding extendedAttributes {}", e.getMessage());
                    }
                }
                return params.data;
            } else {
                defaultData(s);
                return s;
            }
        }
        return new HashMap<>();
    }

    /**
     * Helper method to reduce complexity in transform().
     * Initializes parameters and fetches data.
     */
    private TransformParams initializeAndFetch(Map<String, Object> s, ProductDetailsService productDetailsService) {
        TransformParams params = new TransformParams();
        String fileNameNoExt = ((String) s.get(FILENAME_TAG)).split("\\.")[0];
        params.msku = (String) s.get(MSKU_TAG);
        params.ssku = (s.get(SSKU_TAG) != null) ? s.get(SSKU_TAG).toString() : null;

        List<Map<String, Object>> blobData = productDetailsService.findBlobKeyByFileName(fileNameNoExt);
        params.data = productDetailsService.findProductImageData(params.msku, params.ssku);

        if (!blobData.isEmpty()) {
            params.defaultBlobKey = (String)blobData.get(0).getOrDefault(BLOB_KEY, " ");
        }

        if (params.ssku != null && !params.ssku.isBlank()) {
            params.skuLevel = true;
        }

        return params;
    }

    private void defaultData(Map<String,Object> source) {
        source.put(SKUCODE_TAG, source.get(SSKU_TAG));
        source.remove(SSKU_TAG);
        source.remove(MSKU_TAG);
        source.remove(FILENAME_TAG);
        source.remove("");
    }

    private void processData(Map<String,Object> data, boolean skuLevel, ObjectNode cdmExtendendeAttribute, String defaultBlobKey, Map<String,Object> source) throws IOException {

        Object extAtr = data.get(EXTENDED_ATTRIBUTES);
        ObjectNode extended;
        if (extAtr != null) {
            extended = (ObjectNode) objectMapper.readTree(extAtr.toString());
        } else {
            extended = objectMapper.createObjectNode();
        }

        if (skuLevel) {
            updateFileAndBlobDetails(skuLevel, cdmExtendendeAttribute, data, defaultBlobKey, source);
        } else {
            if (!extended.isEmpty() && extended.has(SKULEVEL_TAG)) {
                if (!getAsBoolean(extended, SKULEVEL_TAG)) {
                    updateFileAndBlobDetails(skuLevel, cdmExtendendeAttribute, data, defaultBlobKey, source);
                } else {
                    data.put(SKUCODE_TAG, data.get(SKUCODE));
                    data.put(BATCHCODE_TAG, data.get(BATCHCODE));
                }
            } else {
                updateFileAndBlobDetails(skuLevel, cdmExtendendeAttribute, data, defaultBlobKey, source);
            }
        }
        data.remove(EXTENDED_ATTRIBUTES);
        data.remove(BLOB_KEY);
        data.remove(SKUCODE);
        data.remove(BATCHCODE);
        data.remove(FILENAME);
    }

    private void updateFileAndBlobDetails(boolean skuLevel, ObjectNode cdmExtendendeAttribute, Map<String,Object>data, String defaultBlobKey, Map<String,Object> source) {
        cdmExtendendeAttribute.put(SKULEVEL_TAG, skuLevel);
        data.put(SKUCODE_TAG, data.get(SKUCODE));
        data.put(BATCHCODE_TAG, data.get(BATCHCODE));
        data.put("fileName", source.get(FILENAME_TAG));
        data.put("blobKey", defaultBlobKey);

        if (null != data.get(FILENAME)) {
            String fileName = (String) data.get(FILENAME);
            String cdmFileName = ((String) source.get(FILENAME_TAG)).split("\\.")[0];
            if (!cdmFileName.equalsIgnoreCase(fileName) && "".equals(defaultBlobKey)) {
                cdmExtendendeAttribute.put("updateBlob", true);
            }
        }

        data.put("extendedAttributes", cdmExtendendeAttribute);
    }

    /**
     * Safely gets a boolean value from a JsonNode, defaulting to false.
     * Replaces the non-existent 'optBoolean'.
     */
    private boolean getAsBoolean(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName) && node.get(fieldName) != null) {
            return node.get(fieldName).asBoolean(false);
        }
        return false;
    }
}