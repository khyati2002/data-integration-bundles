package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService.TransformationException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Flink-compatible ProductDetailsTransformer for ITCL
 * Adapted to use jOOQ DSLContext instead of Spring's QueryService
 */
public class ProductDetailsTransformerITCL extends AbstractTransformer<Map<String, Object>, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ProductDetailsTransformerITCL.class);

    private static final String SKULEVEL_TAG = "skuLevel";
    private static final String FILENAME_TAG = "FileName";
    private static final String SKUCODE_TAG = "skuCode";
    private static final String EXTENDED_ATTRIBUTES_COL = "extended_attributes";
    private static final String BLOB_KEY_COL = "blob_key";
    private static final String SKU_CODE_COL = "sku_code";
    private static final String BATCH_CODE_COL = "batch_code";
    private static final String FILE_NAME_COL = "file_name";
    private static final String[] COLUMNS_TO_FETCH = {EXTENDED_ATTRIBUTES_COL, BLOB_KEY_COL, SKU_CODE_COL, BATCH_CODE_COL, FILE_NAME_COL};

    @Override
    public Object transform(Map<String, Object> s) {
        boolean skuLevel = false;
        checkIfFieldsPresent(s);
        
        // Get DSL Context from AbstractCDMService (which is set during Flink job initialization)
        DSLContext dslContext = AbstractCDMService.getDslContext();
        if (dslContext == null) {
            throw new TransformationException("DSLContext is not initialized. Make sure ServiceLocator is properly set up.");
        }

        // Query for blob key
        String blobKeyValue = getBlobKeyQuery((String) s.get(FILENAME_TAG));
        List<Map<String, Object>> blobData = executeQuery(dslContext, blobKeyValue);
        String defaultBlobKey = "";
        if (!blobData.isEmpty()) {
            defaultBlobKey = (String) blobData.get(0).getOrDefault(BLOB_KEY_COL, "");
        }

        // Build product image data query
        String productImageDataQuery = buildProductImageDataQuery(s);
        List<Map<String, Object>> data = executeQuery(dslContext, productImageDataQuery);
        
        // Determine SKU level
        if (!s.get("SSKU").toString().isBlank()) {
            skuLevel = true;
        }

        checkIfDataPresent(data);

        if (!data.isEmpty()) {
            ObjectNode cdmExtendedAttribute = JSONUtils.getObjectMapper().createObjectNode();
            cdmExtendedAttribute.put("ImageMaster", "ImageMaster");
            for (int i = 0; i < data.size(); i++) {
                try {
                    processData(data.get(i), skuLevel, cdmExtendedAttribute, defaultBlobKey, s);
                } catch (IOException e) {
                    logger.error("Exception occurred while adding extendedAttributes: {}", e.getMessage(), e);
                    throw new TransformationException("Failed to process extended attributes: " + e.getMessage(), e);
                }
            }
            return data;
        } else {
            defaultData(s);
            return s;
        }
    }

    private String getBlobKeyQuery(String fileName) {
        String fileNameWithoutExtension = fileName.split("\\.")[0];
        return String.format(
            "SELECT file_name, blob_key FROM ck_productdetails WHERE blob_key != '' AND blob_key IS NOT NULL AND file_name = '%s' LIMIT 1",
            fileNameWithoutExtension
        );
    }

    private String buildProductImageDataQuery(Map<String, Object> s) {
        StringBuilder query = new StringBuilder("SELECT ");
        query.append(String.join(", ", COLUMNS_TO_FETCH));
        query.append(" FROM ck_productdetails WHERE market_sku_code = '")
             .append(s.get("MSKU").toString()).append("'");
        
        if (!s.get("SSKU").toString().isBlank()) {
            query.append(" AND sku_code = '").append(s.get("SSKU").toString()).append("'");
        }
        
        return query.toString();
    }

    /**
     * Execute raw SQL query using jOOQ DSLContext
     */
    private List<Map<String, Object>> executeQuery(DSLContext dslContext, String sql) {
        try {
            Result<Record> result = dslContext.fetch(sql);
            return result.stream()
                    .map(Record::intoMap)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error executing query: {}", sql, e);
            throw new TransformationException("Database query failed: " + e.getMessage(), e);
        }
    }

    private void checkIfDataPresent(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            throw new TransformationException("No records found. Please check if records are present in your data!");
        }
    }

    private void checkIfFieldsPresent(Map<String, Object> s) {
        if (StringUtils.isNullOrBlank(s.get("SSKU")) && StringUtils.isNullOrBlank(s.get("MSKU"))) {
            throw new TransformationException("Please provide value for MSKU or both");
        }
        if (StringUtils.isNullOrBlank(s.get(FILENAME_TAG))) {
            throw new TransformationException("FileName cannot be empty. Please enter a valid filename!");
        }
    }

    private void defaultData(Map<String, Object> source) {
        source.put(SKUCODE_TAG, source.get("SSKU"));
        source.remove("SSKU");
        source.remove("MSKU");
        source.remove(FILENAME_TAG);
        source.remove("");
    }

    private void processData(Map<String, Object> data, boolean skuLevel, ObjectNode cdmExtendedAttribute, 
                           String defaultBlobKey, Map<String, Object> source) throws IOException {

        Object extAtr = data.get(EXTENDED_ATTRIBUTES_COL);
        ObjectNode extended = JSONUtils.getObjectMapper().createObjectNode();
        if (null != extAtr) {
            extended = (ObjectNode) JSONUtils.getObjectMapper().readTree(extAtr.toString());
        }
        
        if (skuLevel) {
            updateFileAndBlobDetails(skuLevel, cdmExtendedAttribute, data, defaultBlobKey, source);
        } else {
            if (extended.size() > 0 && extended.has(SKULEVEL_TAG)) {
                boolean skuLevelValue = extended.get(SKULEVEL_TAG).asBoolean(false);
                if (!skuLevelValue) {
                    updateFileAndBlobDetails(skuLevel, cdmExtendedAttribute, data, defaultBlobKey, source);
                } else {
                    data.put(SKUCODE_TAG, data.get(SKU_CODE_COL));
                    data.put("batchCode", data.get(BATCH_CODE_COL));
                }
            } else {
                updateFileAndBlobDetails(skuLevel, cdmExtendedAttribute, data, defaultBlobKey, source);
            }
        }
        
        // Remove the fetched columns from the result
        for (String column : COLUMNS_TO_FETCH) {
            data.remove(column);
        }
    }

    private void updateFileAndBlobDetails(boolean skuLevel, ObjectNode cdmExtendedAttribute,
                                        Map<String, Object> data, String defaultBlobKey, 
                                        Map<String, Object> source) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        cdmExtendedAttribute.put(SKULEVEL_TAG, skuLevel);
        data.put(SKUCODE_TAG, data.get(SKU_CODE_COL));
        data.put("batchCode", data.get(BATCH_CODE_COL));
        data.put("fileName", source.get(FILENAME_TAG));
        data.put("blobKey", defaultBlobKey);
        
        if (null != data.get(FILE_NAME_COL)) {
            String fileName = (String) data.get(FILE_NAME_COL);
            String cdmFileName = ((String) source.get(FILENAME_TAG)).split("\\.")[0];
            if (!cdmFileName.equalsIgnoreCase(fileName) && "".equals(defaultBlobKey)) {
                cdmExtendedAttribute.put("updateBlob", true);
            }
        }
        data.put("extendedAttributes", objectMapper.readTree(cdmExtendedAttribute.toString()));
    }
}