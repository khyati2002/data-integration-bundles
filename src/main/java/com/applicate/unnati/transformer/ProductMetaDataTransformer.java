package com.applicate.unnati.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.jooq.generated.tables.pojos.Location;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ProductMetaDataTransformer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> output = new LinkedHashMap<>();

        output.put("id", asString(inputMap.get("id")));
        output.put("activeStatus", asActiveStatus(inputMap.get("activeStatus")));
        output.put("activeStatusReason", asString(inputMap.get("activeStatusReason")));
        output.put("createdBy", asString(inputMap.get("createdBy")));
        output.put("extendedAttributes", asJsonNode(inputMap.get("extendedAttributes")));
        output.put("lob", asString(inputMap.get("lob")));
        output.put("modifiedBy", asString(inputMap.get("modifiedBy")));
        output.put("version", asInteger(inputMap.get("version")));
        output.put("basePrice", asBigDecimal(inputMap.get("basePrice")));
        output.put("casePtr", asBigDecimal(inputMap.get("casePtr")));
        output.put("gst", asBigDecimal(inputMap.get("gst")));
        output.put("level", asString(inputMap.get("level")));
        output.put("maxQty", asInteger(inputMap.get("maxQty")));
        output.put("minQty", asInteger(inputMap.get("minQty")));
        output.put("packPtr", asBigDecimal(inputMap.get("packPtr")));
        output.put("priceList", asString(inputMap.get("priceList")));
        output.put("skuCode", asString(inputMap.get("skuCode")));
        output.put("tax", asString(inputMap.get("tax")));
        output.put("taxAmount", asBigDecimal(inputMap.get("taxAmount")));
        output.put("locationHierarchy",parseLocationHierarchy(inputMap, "locationHierarchy"));
        output.put("loginid", asString(inputMap.get("loginid")));
        output.put("fkProductmetadata", asString(inputMap.get("fkProductmetadata")));
        output.put("source", asString(inputMap.get("source")));
        output.put("batchCode", asString(inputMap.get("batchCode")));
        output.put("otherUnitPtr", asBigDecimal(inputMap.get("otherUnitPtr")));
        output.put("channel", asString(inputMap.get("channel")));
        output.put("account", asString(inputMap.get("account")));
        output.put("mrp", asBigDecimal(inputMap.get("mrp")));
        output.put("whCode", asString(inputMap.get("whCode")));
        output.put("hash", asString(inputMap.get("hash")));
        output.put("changed", asByte(inputMap.get("changed")));
        output.put("subChannel", asString(inputMap.get("subChannel")));
        output.put("outletcode", asString(inputMap.get("outletcode")));
        output.put("caseMrp", asBigDecimal(inputMap.get("caseMrp")));
        output.put("otherUnitMrp", asDouble(inputMap.get("otherUnitMrp")));
        output.put("fromDate", asString(inputMap.get("fromDate")));
        output.put("toDate", asString(inputMap.get("toDate")));
        output.put("caseToOtherUnitQuantity", asBigDecimal(inputMap.get("caseToOtherUnitQuantity")));
        output.put("caseToPieceQuantity", asBigDecimal(inputMap.get("caseToPieceQuantity")));
        output.put("otherUnitToPieceQuantity", asBigDecimal(inputMap.get("otherUnitToPieceQuantity")));
        output.put("pieceToOtherUnitQuantity", asBigDecimal(inputMap.get("pieceToOtherUnitQuantity")));
        output.put("ssp", asBigDecimal(inputMap.get("ssp")));
        output.put("priority", asInteger(inputMap.get("priority")));
        output.put("batchId", asString(inputMap.get("batchId")));
        output.put("productCode", asString(inputMap.get("productCode")));
        output.put("schemePrice", asBigDecimal(inputMap.get("schemePrice")));

        return output;
    }


    private  static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private  static Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static  Double asDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        try {
            return Double.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private  static BigDecimal asBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static  Byte asByte(Object value) {
        if (value == null) return null;
        if (value instanceof Byte) return (Byte) value;
        try {
            return Byte.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private  static ActiveStatus asActiveStatus(Object value) {
        if (value == null) return null;
        if (value instanceof ActiveStatus) return (ActiveStatus) value;
        try {
            return ActiveStatus.valueOf(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private  static JsonNode asJsonNode(Object value) {
        if (value == null) return null;
        if (value instanceof JsonNode) return (JsonNode) value;
        try {
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private  static Location parseLocationHierarchy(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        Location location = new Location();

        if (value instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) value;
            location.setCountry((String) m.get("country"));
            location.setRegion((String) m.get("region"));
            location.setState((String) m.get("state"));
            location.setCity((String) m.get("city"));
            location.setPincode((String) m.get("pincode"));
            location.setZone((String) m.get("zone"));
            location.setCountryCode((String) m.get("countryCode"));
            location.setRegionCode((String) m.get("regionCode"));
            location.setStateCode((String) m.get("stateCode"));
            location.setCityCode((String) m.get("cityCode"));
            location.setZoneCode((String) m.get("zoneCode"));
            return location;
        }
        return null;
    }
}