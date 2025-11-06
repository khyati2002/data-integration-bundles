package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.jooq.JSON;
import java.util.*;
import java.util.stream.Collectors;

public class SalesDetailsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ProductDetailsTransformer productDetailsTransformer = new ProductDetailsTransformer();

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
        result.put("productCode", getString(inputMap, "productCode"));
        result.put("productDetails", getProductDetails(inputMap));

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProductDetails(Map<String, Object> rawMap) {
        Object value = rawMap.get("productDetails");
        if (!(value instanceof Map)) return null;

        Map<String, Object> productMap = new LinkedHashMap<>((Map<String, Object>) value);
        productMap.remove("productDetails"); // defensive
        return productDetailsTransformer.transform(productMap);
    }



}
