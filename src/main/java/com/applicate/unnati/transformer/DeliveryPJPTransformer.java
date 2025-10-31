package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.JSON;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeliveryPJPTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // CommonDataModel fields
        result.put("id", getString(inputMap, "id"));
        result.put("activeStatus", getActiveStatus(inputMap, "activeStatus"));
        result.put("activeStatusReason", getString(inputMap, "activeStatusReason"));
        result.put("createdBy", getString(inputMap, "createdBy"));
        result.put("extendedAttributes", getJsonNode(inputMap, "extendedAttributes"));
        result.put("lob", getString(inputMap, "lob"));
        result.put("modifiedBy", getString(inputMap, "modifiedBy"));
        result.put("version", getInteger(inputMap, "version"));
        result.put("source", getString(inputMap, "source"));
        result.put("hash", getString(inputMap, "hash"));

        // DeliveryPJP specific fields
        result.put("beat", getString(inputMap, "beat"));
        result.put("dayAndFrequency", getJSONAsString(inputMap, "dayAndFrequency"));
        result.put("month", getString(inputMap, "month"));
        result.put("year", getString(inputMap, "year"));

        // Field name mappings (camelCase to lowercase in POJO)
        result.put("outletcode", getString(inputMap, "outletCode"));
        result.put("loginid", getString(inputMap, "loginId"));
        result.put("supplierid", getString(inputMap, "supplierId"));

        // Designation with toLowerCase transformation
        result.put("designation", getDesignationLowerCase(inputMap, "designation"));

        result.put("approvedBy", getString(inputMap, "approvedBy"));
        result.put("destinationCode", getString(inputMap, "destinationCode"));
        result.put("destinationName", getString(inputMap, "destinationName"));

        result.put("pjpDate", getLocalDateTimeValue(inputMap, "pjpDate"));

        result.put("pjpPlan", getString(inputMap, "pjpPlan"));
        result.put("sourceCode", getString(inputMap, "sourceCode"));
        result.put("sourceName", getString(inputMap, "sourceName"));
        result.put("status", getString(inputMap, "status"));
        result.put("statusRemarks", getString(inputMap, "statusRemarks"));
        result.put("type", getString(inputMap, "type"));
        result.put("referenceNumber", getString(inputMap, "referenceNumber"));
        result.put("turnAroundTime", getString(inputMap, "turnAroundTime"));

        // FIXED: changed field should be Boolean, not Byte
        result.put("changed", getBoolean(inputMap, "changed"));

        result.put("outletVisited", getBoolean(inputMap, "outletVisited"));
        result.put("sequence", getInteger(inputMap, "sequence"));
        result.put("beatId", getString(inputMap, "beatId"));

        // REMOVED: deliveryDate (doesn't exist in POJO)
        // ADD: rowid field (auto-generated, typically null from input)
        result.put("rowid", getInteger(inputMap, "rowid"));

        return result;
    }

    // Helper methods

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

    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            String str = value.toString().toLowerCase();
            return "true".equals(str) || "1".equals(str) || "yes".equals(str);
        } catch (Exception e) {
            return null;
        }
    }

    private String getDesignationLowerCase(Map<String, Object> map, String key) {
        String value = getString(map, key);
        return (value != null) ? value.toLowerCase() : null;
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
            // Handle Map objects from streaming data
            if (value instanceof Map) {
                String jsonString = objectMapper.writeValueAsString(value);
                return objectMapper.readTree(jsonString);
            }
            return objectMapper.readTree(value.toString());
        } catch (Exception e) {
            System.err.println("Failed to parse JsonNode for key: " + key);
            return null;
        }
    }

    private String getJSONAsString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;

        try {
            // If it's already a jOOQ JSON, extract the string
            if (value instanceof JSON) {
                return ((JSON) value).data();
            }

            // If it's JsonNode, convert to string
            if (value instanceof JsonNode) {
                return objectMapper.writeValueAsString(value);
            }

            // If it's a List or Map (from streaming data), convert to JSON string
            if (value instanceof java.util.List || value instanceof java.util.Map) {
                return objectMapper.writeValueAsString(value);
            }

            // If it's already a string, return as-is
            if (value instanceof String) {
                return (String) value;
            }

            // Fallback: convert to string
            return objectMapper.writeValueAsString(value);

        } catch (Exception e) {
            System.err.println("Failed to convert to JSON string for key: " + key);
            return null;
        }
    }

    private LocalDateTime getLocalDateTimeValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        try {
            return parseToLocalDateTime(value);
        } catch (Exception e) {
            System.err.println("Failed to parse LocalDateTime for key: " + key + ", value type: "
                                       + value.getClass().getName() + ", value: " + value + " -> " + e.getMessage());
            return null;
        }
    }

    private LocalDateTime parseToLocalDateTime(Object value) {
        // 1) Already the right type
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof LocalDate) return ((LocalDate) value).atStartOfDay();
        if (value instanceof OffsetDateTime) return ((OffsetDateTime) value).toLocalDateTime();
        if (value instanceof Instant) return LocalDateTime.ofInstant((Instant) value, ZoneId.systemDefault());

        // 2) jOOQ JSON wrapper
        if (value instanceof JSON) {
            String s = ((JSON) value).data();
            return parseToLocalDateTime(s);
        }

        // 3) Jackson JsonNode
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            if (node.isTextual()) {
                return parseToLocalDateTime(node.asText());
            } else if (node.isArray() && node.size() > 0) {
                return parseToLocalDateTime(node.get(0));
            } else if (node.isObject()) {
                // try to find textual date field
                if (node.has("date")) return parseToLocalDateTime(node.get("date"));
                if (node.has("value")) return parseToLocalDateTime(node.get("value"));
                // fallback to node.toString()
                return parseToLocalDateTime(node.toString());
            }
        }

        // 4) Collections / arrays
        if (value instanceof List) {
            List<?> l = (List<?>) value;
            if (l.isEmpty()) return null;
            return parseToLocalDateTime(l.get(0));
        }
        if (value.getClass().isArray()) {
            Object[] arr = (Object[]) value;
            if (arr.length == 0) return null;
            return parseToLocalDateTime(arr[0]);
        }

        // 5) Map (possible POJO-like map with year/month/day)
        if (value instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) value;
            // detect year/month/day ints
            if (m.containsKey("year") && m.containsKey("month") && m.containsKey("day")) {
                try {
                    int y = Integer.parseInt(String.valueOf(m.get("year")));
                    int mo = Integer.parseInt(String.valueOf(m.get("month")));
                    int d = Integer.parseInt(String.valueOf(m.get("day")));
                    return LocalDate.of(y, mo, d).atStartOfDay();
                } catch (Exception ignored) { }
            }
            // try to find common string fields
            if (m.containsKey("date")) return parseToLocalDateTime(m.get("date"));
            if (m.containsKey("value")) return parseToLocalDateTime(m.get("value"));
            // fallback to map.toString()
            return parseToLocalDateTime(m.toString());
        }

        // 6) Numbers (epoch seconds or millis)
        if (value instanceof Number) {
            long n = ((Number) value).longValue();
            // heuristic: > 10^12 -> millis, else seconds
            if (Math.abs(n) > 1_000_000_000_000L) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(n), ZoneId.systemDefault());
            } else {
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(n), ZoneId.systemDefault());
            }
        }

        // 7) String handling (most common case)
        String s = value.toString().trim();
        if (s.isEmpty()) return null;

        // If string looks like JSON array/object, parse with ObjectMapper to JsonNode
        if (s.startsWith("[") || s.startsWith("{")) {
            try {
                JsonNode node = objectMapper.readTree(s);
                return parseToLocalDateTime(node);
            } catch (Exception ignored) { /* fall through to text parsing */ }
        }

        // Remove surrounding quotes/brackets if someone stringified an array like ["2025-10-31"]
        if (s.startsWith("[") && s.endsWith("]")) {
            String inner = s.substring(1, s.length() - 1).trim();
            // remove surrounding quotes
            if (inner.startsWith("\"") && inner.endsWith("\"") && inner.length() >= 2) {
                inner = inner.substring(1, inner.length() - 1);
            }
            return parseToLocalDateTime(inner);
        }

        // Normalize a trailing 'Z'
        if (s.endsWith("Z")) {
            try {
                return OffsetDateTime.parse(s).toLocalDateTime();
            } catch (Exception ignored) {}
        }

        // ISO_LOCAL_DATE_TIME or date-only
        try {
            // e.g. "2025-10-31T00:00:00"
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        try {
            // e.g. "2025-10-31"
            if (s.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDate.parse(s).atStartOfDay();
            }
        } catch (Exception ignored) {}

        // "yyyy-MM-dd HH:mm:ss"
        try {
            if (s.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(s, f);
            }
        } catch (Exception ignored) {}

        // Give up
        throw new IllegalArgumentException("Unrecognized date/time format or type: " + value.getClass() + " -> " + s);
    }



}
