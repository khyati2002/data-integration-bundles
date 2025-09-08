package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.Geometry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class OutletDetailsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>>  {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ISO_DATE_TIME
    };
    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", getString(inputMap, "id"));
        result.put("activeStatus", getActiveStatus(inputMap, "activeStatus"));
        result.put("activeStatusReason", getString(inputMap, "activeStatusReason"));
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("creationTime", getLocalDateTime(inputMap, "creationTime"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("lastModifiedTime", getLocalDateTime(inputMap, "lastModifiedTime"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("version", getInteger(inputMap, "version"));

        // OutletDetails specific fields
        result.put("address", getString(inputMap, "address"));
        result.put("beat", getString(inputMap, "beat"));
        result.put("beatName", getString(inputMap, "beatName"));
        result.put("channel", getString(inputMap, "channel"));
        result.put("contactName", getString(inputMap, "contactName"));
        result.put("contactno", getString(inputMap, "contactno"));
        result.put("displayAddress", getString(inputMap, "displayAddress"));
        result.put("frequency", getString(inputMap, "frequency"));
        result.put("mapped", getBoolean(inputMap, "mapped"));
        result.put("outletcode", getString(inputMap, "outletcode"));
        result.put("outletName", getString(inputMap, "outletName"));
        result.put("outletType", getString(inputMap, "outletType"));
        result.put("locationHierarchy", getString(inputMap, "locationHierarchy"));
        result.put("loginid", getString(inputMap, "loginid"));
        result.put("source", getString(inputMap, "source"));
        result.put("lastOrderDate", getLocalDateTime(inputMap, "lastOrderDate"));
        result.put("latitude", getBigDecimal(inputMap, "latitude"));
        result.put("longitude", getBigDecimal(inputMap, "longitude"));
        result.put("account", getString(inputMap, "account"));
        result.put("gstNo", getString(inputMap, "gstNo"));
        result.put("marketId", getString(inputMap, "marketId"));
        result.put("marketName", getString(inputMap, "marketName"));
        result.put("outletCategory", getString(inputMap, "outletCategory"));
        result.put("outletClass", getString(inputMap, "outletClass"));
        result.put("tinNo", getString(inputMap, "tinNo"));
        result.put("hash", getString(inputMap, "hash"));
        result.put("coordinate", getGeometry(inputMap, "coordinate"));
        result.put("doo", getLocalDateTime(inputMap, "doo"));
        result.put("hierarchy", getString(inputMap, "hierarchy"));
        result.put("rowid", getInteger(inputMap, "rowid"));
        result.put("changed", getByte(inputMap, "changed"));
        result.put("outletDivision", getString(inputMap, "outletDivision"));
        result.put("distributionChannel", getString(inputMap, "distributionChannel"));
        result.put("soldTo", getString(inputMap, "soldTo"));
        result.put("subChannel", getString(inputMap, "subChannel"));
        result.put("subTerritory", getString(inputMap, "subTerritory"));
        result.put("email", getString(inputMap, "email"));
        result.put("blobKey", getString(inputMap, "blobKey"));
        result.put("controlGroup", getString(inputMap, "controlGroup"));
        result.put("normalizedHierarchy", getString(inputMap, "normalizedHierarchy"));
        result.put("priceListId", getString(inputMap, "priceListId"));
        result.put("prodauthcode", getString(inputMap, "prodauthcode"));
        result.put("outletWhatsappNumber", getString(inputMap, "outletWhatsappNumber"));
        result.put("dateOfClosing", getLocalDateTime(inputMap, "dateOfClosing"));
        result.put("vpo", getString(inputMap, "vpo"));
        result.put("segment", getString(inputMap, "segment"));
        result.put("outletAttr1", getString(inputMap, "outletAttr1"));
        result.put("outletAttr2", getString(inputMap, "outletAttr2"));
        result.put("outletAttr3", getString(inputMap, "outletAttr3"));
        result.put("outletAttr4", getString(inputMap, "outletAttr4"));
        result.put("outletAttr5", getString(inputMap, "outletAttr5"));
        result.put("outletAttr6", getString(inputMap, "outletAttr6"));
        result.put("fssaiNumber", getString(inputMap, "fssaiNumber"));
        result.put("paymentMode", getString(inputMap, "paymentMode"));
        result.put("salesMode", getString(inputMap, "salesMode"));
        result.put("tcsEligibility", getByte(inputMap, "tcsEligibility"));
        result.put("keyAccount", getByte(inputMap, "keyAccount"));
        result.put("outletDiscount", getBigDecimal(inputMap, "outletDiscount"));
        result.put("discountGroup", getString(inputMap, "discountGroup"));
        result.put("shipToAddress", getString(inputMap, "shipToAddress"));

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

    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            return new BigDecimal(value.toString());
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

    private Byte getByte(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Number) {
                return ((Number) value).byteValue();
            }
            return Byte.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime getLocalDateTime(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }
            String raw = value.toString().trim();
            if (raw.contains("T") && raw.contains("Z")) {
                raw = raw.substring(0, raw.indexOf("T")) + " " + raw.substring(raw.indexOf("T") + 1, raw.indexOf("Z"));
            }

            for (DateTimeFormatter formatter : DATE_FORMATTERS) {
                try {
                    return LocalDateTime.parse(raw, formatter);
                } catch (Exception ignored) {}
            }
            return null;
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
            return ActiveStatus.valueOf(value.toString().toUpperCase());
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

    private Geometry getGeometry(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Geometry) {
                return (Geometry) value;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

}