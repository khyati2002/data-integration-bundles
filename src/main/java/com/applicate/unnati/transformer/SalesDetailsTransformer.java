package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.jooq.JSON;
import java.util.*;

public class SalesDetailsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", getString(inputMap, "id"));
        result.put("activeStatus", getActiveStatus(inputMap, "activeStatus"));
        result.put("activeStatusReason", getString(inputMap, "activeStatusReason"));
        result.put("changed", getBoolean(inputMap, "changed"));
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("source", getString(inputMap, "source"));
        result.put("version", getInteger(inputMap, "version"));
        result.put("systemTime", getString(inputMap, "systemTime"));

        result.put("gpsLatitude", getString(inputMap, "gpsLatitude"));
        result.put("gpsLongitude", getString(inputMap, "gpsLongitude"));

        result.put("billAmount", getDouble(inputMap, "billAmount"));
        result.put("userHierarchy", getString(inputMap, "userHierarchy"));
        result.put("initialAmount", getDouble(inputMap, "initialAmount"));
        result.put("locationHierarchy", getString(inputMap, "locationHierarchy"));
        result.put("mrp", getDouble(inputMap, "mrp"));
        result.put("name", getString(inputMap, "name"));
        result.put("netAmount", getDouble(inputMap, "netAmount"));
        result.put("normalizedVolume", getDouble(inputMap, "normalizedVolume"));
        result.put("orderNumber", getString(inputMap, "orderNumber"));
        result.put("orderedDate", getString(inputMap, "orderedDate"));
        result.put("payByDate", getString(inputMap, "payByDate"));
        result.put("programNumber", getString(inputMap, "programNumber"));
        result.put("remarks", getString(inputMap, "remarks"));
        result.put("size", getString(inputMap, "size"));
        result.put("status", getString(inputMap, "status"));
        result.put("supplierid", getString(inputMap, "supplierid"));
        result.put("hierarchy", getString(inputMap, "hierarchy"));
        result.put("type", getString(inputMap, "type"));

        result.put("batchCode", getString(inputMap, "batchCode"));
        result.put("batchIds", getJooqJsonFromArray(inputMap, "batchIds"));
        result.put("batchId", getString(inputMap, "batchId"));
        result.put("skucode", getString(inputMap, "skuCode"));
        result.put("saleId", getString(inputMap, "invoiceNumber"));

        result.put("price", getDouble(inputMap, "price"));
        result.put("casePrice", getDouble(inputMap, "casePrice"));
        result.put("otherUnitPrice", getDouble(inputMap, "otherUnitPrice"));

        result.put("pieceQuantity", getDouble(inputMap, "pieceQuantity"));
        result.put("caseQuantity", getDouble(inputMap, "caseQuantity"));
        result.put("otherUnitQuantity", getDouble(inputMap, "otherUnitQuantity"));
        result.put("normalizedQuantity", getDouble(inputMap, "normalizedQuantity"));
        result.put("quantityUnit", getString(inputMap, "quantityUnit"));

        result.put("initialQuantity", getDouble(inputMap, "initialQuantity"));
        result.put("initialPieceQuantity", getDouble(inputMap, "initialPieceQuantity"));
        result.put("initialCaseQuantity", getDouble(inputMap, "initialCaseQuantity"));
        result.put("initialOtherUnitQuantity", getDouble(inputMap, "initialOtherUnitQuantity"));
        result.put("initialNormalizedQuantity", getDouble(inputMap, "initialNormalizedQuantity"));

        result.put("productInfo", getJooqJsonAsString(inputMap, "productInfo"));
        result.put("discountInfo", getJooqJsonAsString(inputMap, "discountInfo"));

        result.put("nw", getDouble(inputMap, "nw"));
        result.put("amount", getDouble(inputMap, "amount"));
        result.put("rowid", getInteger(inputMap, "rowid"));

        return result;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return Boolean.valueOf(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private ActiveStatus getActiveStatus(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof ActiveStatus) {
                return (ActiveStatus) value;
            }
            String statusStr = value.toString().toUpperCase().trim();
            return ActiveStatus.valueOf(statusStr);
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode getJsonNode(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof JsonNode) {
                return (JsonNode) value;
            }
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            return null;
        }
    }


    private String getJooqJsonAsString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return "{}";
        try {
            if (value instanceof JSON) {
                return ((JSON) value).data();
            }
            if (value instanceof JsonNode) {
                return objectMapper.writeValueAsString(value);
            }
            if (value instanceof Map || value instanceof Iterable) {
                return objectMapper.writeValueAsString(value);
            }
            String str = value.toString().trim();
            objectMapper.readTree(str);
            return str;
        } catch (Exception e) {
            return "{}";
        }
    }

    private String getJooqJsonFromArray(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return "[]";

        try {
            if (value instanceof JSON) {
                return ((JSON) value).data();
            }
            if (value instanceof ArrayNode || value instanceof Iterable) {
                return objectMapper.writeValueAsString(value);
            }
            String str = value.toString().trim();
            if (str.startsWith("\"[") && str.endsWith("]\"")) {
                str = str.substring(1, str.length() - 1).replace("\\\"", "\"");
            }

            objectMapper.readTree(str);
            return str;
        } catch (Exception e) {
            return "[]";
        }
    }

}


/*
 * Mock streaming raw data for SalesDetails testing
 */
class SalesDetailsMockData {

    public static String rawStreamingData = "{\n" +
                                                    "  \"requestId\": \"test-req-SALES-001\",\n" +
                                                    "  \"groupId\": \"SALE2025-001\",\n" +
                                                    "  \"lob\": \"ckcoeuat\",\n" +
                                                    "  \"loginId\": \"integration_user\",\n" +
                                                    "  \"batchNumber\": 1,\n" +
                                                    "  \"transformerInfo\": [\n" +
                                                    "    {\n" +
                                                    "      \"skipPreprocessing\": false,\n" +
                                                    "      \"skipPersist\": false,\n" +
                                                    "      \"entityName\": \"SalesDetails\",\n" +
                                                    "      \"transformerId\": \"genericSalesDetailsTransformer\",\n" +
                                                    "      \"operationType\": \"insert\"\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"features\": [\n" +
                                                    "    {\n" +
                                                    "      \"id\": \"SALE-DTL-12345\",\n" +
                                                    "      \"activeStatus\": \"ACTIVE\",\n" +
                                                    "      \"activeStatusReason\": \"Valid sales transaction\",\n" +
                                                    "      \"changed\": false,\n" +
                                                    "      \"createdBy\": \"sales_user\",\n" +
                                                    "      \"creationTime\": \"2025-11-06T10:30:00\",\n" +
                                                    "      \"extendedAttributes\": \"{ \\\"promotionApplied\\\": true, \\\"discountType\\\": \\\"SEASONAL\\\" }\",\n" +
                                                    "      \"hash\": \"sales_hash_xyz789\",\n" +
                                                    "      \"lastModifiedTime\": \"2025-11-06T10:30:00\",\n" +
                                                    "      \"lob\": \"ckcoeuat\",\n" +
                                                    "      \"modifiedBy\": \"sales_user\",\n" +
                                                    "      \"source\": \"SalesManagementSystem\",\n" +
                                                    "      \"version\": 1,\n" +
                                                    "      \"systemTime\": \"2025-11-06T10:30:00\",\n" +
                                                    "      \"gpsLatitude\": \"28.4595\",\n" +
                                                    "      \"gpsLongitude\": \"77.0266\",\n" +
                                                    "      \"billAmount\": 1375.0,\n" +
                                                    "      \"userHierarchy\": \"{ \\\"salesPerson\\\": \\\"SP001\\\", \\\"territory\\\": \\\"NORTH\\\" }\",\n" +
                                                    "      \"initialAmount\": 1500.0,\n" +
                                                    "      \"locationHierarchy\": \"{ \\\"country\\\": \\\"India\\\", \\\"state\\\": \\\"Haryana\\\", \\\"city\\\": \\\"Gurugram\\\" }\",\n" +
                                                    "      \"mrp\": 25.0,\n" +
                                                    "      \"name\": \"Cavin Care Rose Water 180ml\",\n" +
                                                    "      \"netAmount\": 1325.0,\n" +
                                                    "      \"normalizedVolume\": 55.0,\n" +
                                                    "      \"orderNumber\": \"ORD-2025-11-001\",\n" +
                                                    "      \"orderedDate\": \"2025-11-06T09:00:00\",\n" +
                                                    "      \"payByDate\": \"2025-11-20T23:59:59\",\n" +
                                                    "      \"programNumber\": \"PROG-2025-Q4\",\n" +
                                                    "      \"remarks\": \"Bulk order for retail chain\",\n" +
                                                    "      \"size\": \"180ML\",\n" +
                                                    "      \"status\": \"COMPLETED\",\n" +
                                                    "      \"supplierid\": \"SUPP-CC-001\",\n" +
                                                    "      \"hierarchy\": \"{ \\\"level1\\\": \\\"FMCG\\\", \\\"level2\\\": \\\"PersonalCare\\\" }\",\n" +
                                                    "      \"type\": \"PRIMARY_SALES\",\n" +
                                                    "      \"batchCode\": \"M01G002044_22.3800\",\n" +
                                                    "      \"batchIds\": \"[\\\"BATCH-001\\\", \\\"BATCH-002\\\"]\",\n" +
                                                    "      \"batchId\": \"M01G002044\",\n" +
                                                    "      \"skuCode\": \"CNFM180ROSE01TR\",\n" +
                                                    "      \"invoiceNumber\": \"INV-2025-11-001\",\n" +
                                                    "      \"price\": 25.0,\n" +
                                                    "      \"casePrice\": 600.0,\n" +
                                                    "      \"otherUnitPrice\": 300.0,\n" +
                                                    "      \"pieceQuantity\": 55.0,\n" +
                                                    "      \"caseQuantity\": 2.0,\n" +
                                                    "      \"otherUnitQuantity\": 5.0,\n" +
                                                    "      \"normalizedQuantity\": 55.0,\n" +
                                                    "      \"quantityUnit\": \"PC\",\n" +
                                                    "      \"initialQuantity\": 55.0,\n" +
                                                    "      \"initialPieceQuantity\": 55.0,\n" +
                                                    "      \"initialCaseQuantity\": 2.0,\n" +
                                                    "      \"initialOtherUnitQuantity\": 5.0,\n" +
                                                    "      \"initialNormalizedQuantity\": 55.0,\n" +
                                                    "      \"productInfo\": \"{ \\\"productCode\\\": \\\"CNFM180ROSE01TR\\\", \\\"hsnCode\\\": \\\"22029930\\\", \\\"manfDt\\\": \\\"2024-12-27\\\", \\\"expiryDt\\\": \\\"2025-06-25\\\" }\",\n" +
                                                    "      \"discountInfo\": \"{ \\\"tradeDiscPer\\\": 1.0, \\\"tradeDiscAmount\\\": 0.0, \\\"addDiscAmt\\\": 0.0, \\\"totalDiscount\\\": 50.0 }\",\n" +
                                                    "      \"nw\": 9900.0,\n" +
                                                    "      \"amount\": 1375.0,\n" +
                                                    "      \"rowid\": 21\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}";

    // Alternative format based on the provided mock body structure
    public static String rawStreamingDataCavincare = "{\n" +
                                                             "  \"requestId\": \"85d6615b-b3a4-41e5-97ad-d34f1a580daf\",\n" +
                                                             "  \"groupId\": \"PURCHINV199\",\n" +
                                                             "  \"fileId\": null,\n" +
                                                             "  \"lob\": \"ckcoeuat\",\n" +
                                                             "  \"submittedBy\": null,\n" +
                                                             "  \"transformerInfo\": [\n" +
                                                             "    {\n" +
                                                             "      \"entityName\": \"SalesDetails\",\n" +
                                                             "      \"transformerId\": \"Cavincare_Sales_Details_transformer\",\n" +
                                                             "      \"operationType\": \"insert\",\n" +
                                                             "      \"skipPreprocessing\": \"true\"\n" +
                                                             "    }\n" +
                                                             "  ],\n" +
                                                             "  \"topicName\": null,\n" +
                                                             "  \"preserveOnFailure\": true,\n" +
                                                             "  \"features\": [\n" +
                                                             "    {\n" +
                                                             "      \"id\": \"SALE-DTL-67890\",\n" +
                                                             "      \"activeStatus\": \"ACTIVE\",\n" +
                                                             "      \"SlNo\": 21,\n" +
                                                             "      \"skuCode\": \"CNFM180ROSE01TR\",\n" +
                                                             "      \"batchCode\": \"M01G002044_22.3800\",\n" +
                                                             "      \"ManfDt\": \"2024-12-27\",\n" +
                                                             "      \"ExpiryDt\": \"2025-06-25\",\n" +
                                                             "      \"HsnCode\": \"22029930\",\n" +
                                                             "      \"pieceQuantity\": 55.0,\n" +
                                                             "      \"OfferQty\": 0,\n" +
                                                             "      \"quantityUnit\": \"PC\",\n" +
                                                             "      \"price\": 25.0,\n" +
                                                             "      \"PurchPrice\": 25.0,\n" +
                                                             "      \"mrp\": 25.0,\n" +
                                                             "      \"DiscAmtLl\": 0,\n" +
                                                             "      \"LlGrossAmt\": 1375.0,\n" +
                                                             "      \"netAmount\": 1375.0,\n" +
                                                             "      \"LlTaxAmt\": 1.0,\n" +
                                                             "      \"LlTradeDiscPer\": 1.0,\n" +
                                                             "      \"LlTradeDiscAmount\": 0,\n" +
                                                             "      \"TaxPerc1\": 1.0,\n" +
                                                             "      \"TaxPerc2\": 1.0,\n" +
                                                             "      \"TaxPerc3\": 1.0,\n" +
                                                             "      \"TaxPerc4\": 1.0,\n" +
                                                             "      \"TaxPerc5\": 1.0,\n" +
                                                             "      \"TaxAmt1\": 1.0,\n" +
                                                             "      \"TaxAmt2\": 1.0,\n" +
                                                             "      \"TaxAmt3\": 0,\n" +
                                                             "      \"TaxAmt4\": 0,\n" +
                                                             "      \"TaxAmt5\": 0,\n" +
                                                             "      \"CstPerc1\": 0,\n" +
                                                             "      \"CstPerc2\": 0,\n" +
                                                             "      \"CstPerc3\": 0,\n" +
                                                             "      \"CstAmt1\": 0,\n" +
                                                             "      \"CstAmt2\": 0,\n" +
                                                             "      \"CstAmt3\": 0,\n" +
                                                             "      \"LlAddDiscAmt\": 0,\n" +
                                                             "      \"discountInfo\": \"{ \\\"tradeDiscPer\\\": 1.0, \\\"tradeDiscAmount\\\": 0, \\\"addDiscAmt\\\": 0 }\",\n" +
                                                             "      \"productInfo\": \"{ \\\"hsnCode\\\": \\\"22029930\\\", \\\"manfDt\\\": \\\"2024-12-27\\\", \\\"expiryDt\\\": \\\"2025-06-25\\\" }\",\n" +
                                                             "      \"lob\": \"ckcoeuat\",\n" +
                                                             "      \"createdBy\": \"integration_user\",\n" +
                                                             "      \"source\": \"CavincareInvoiceSystem\",\n" +
                                                             "      \"rowid\": 21,\n" +
                                                             "      \"normalizedQuantity\": 55.0,\n" +
                                                             "      \"initialQuantity\": 55.0,\n" +
                                                             "      \"initialPieceQuantity\": 55.0\n" +
                                                             "    }\n" +
                                                             "  ],\n" +
                                                             "  \"loginId\": \"integration_user\",\n" +
                                                             "  \"offset\": null,\n" +
                                                             "  \"retryCount\": null,\n" +
                                                             "  \"ignoreS3Log\": false,\n" +
                                                             "  \"headersMap\": null\n" +
                                                             "}";
}