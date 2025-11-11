package com.applicate.kgbpl.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class KgplCustomerMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final Pattern LAT_PATTERN =
            Pattern.compile("^[+-]?(?:90(?:\\.0+)?|(?:[0-8]?\\d)(?:\\.\\d+)?)$");
    private static final Pattern LON_PATTERN =
            Pattern.compile("^[+-]?(?:180(?:\\.0+)?|(?:1[0-7]\\d|\\d?\\d)(?:\\.\\d+)?)$");

    @Override
    public Map<String, Object> transform(Map<String, Object> source) {
        validateFields(source);

        // Use LinkedHashMap to maintain insertion order and ensure type safety
        HashMap<String, Object> result = new HashMap<>();

        String dataAreaId = getRawValue(source.get("dataAreaId"));

        // Ensure dataAreaId is never null
        if (dataAreaId == null || dataAreaId.isEmpty()) {
            dataAreaId = "";
        }

        // Build locationHierarchy as a plain string (like the reference code)
        result.put("locationHierarchy", buildLocationHierarchy(source, dataAreaId));

        result.put("channel", concatWithDataAreaId(source, "Channel", dataAreaId));
        result.put("displayAddress", getRawValue(source.get("City")) + "," + getRawValue(source.get("StateName")));
        result.put("source", dataAreaId);
        result.put("segment", concatWithDataAreaId(source, "SegmentId", dataAreaId));
        result.put("outletAttr5", concatWithDataAreaId(source, "ShipToCode", dataAreaId));
        result.put("subChannel", concatWithDataAreaId(source, "SubChannel", dataAreaId));
        result.put("outletClass", concatWithDataAreaId(source, "VPO", dataAreaId));

        String subsegment = getRawValue(source.get("SubsegmentId"));
        String segment = getRawValue(source.get("SegmentId"));
        if (!subsegment.isEmpty() && !segment.isEmpty()) {
            result.put("outletDivision", subsegment + "-" + segment + "-" + dataAreaId);
        } else {
            result.put("outletDivision", "");
        }

        result.put("outletCode", concatWithDataAreaId(source, "CustomerAccount", dataAreaId));
        result.put("outletCategory", concatWithDataAreaId(source, "CustomerCategory", dataAreaId));

        String discountGroup = getRawValue(source.get("LineDiscountCode"));
        result.put("discountGroup", discountGroup.isEmpty() ? "NA" : discountGroup + "-" + dataAreaId);

        result.put("priceListId", concatWithDataAreaId(source, "DiscountPriceGroupId", dataAreaId));
        result.put("vpo", getRawValue(source.get("VPO")));
        result.put("email", getRawValue(source.get("PrimaryContactEmail")));
        result.put("gstNo", getRawValue(source.get("GSTIN")));
        result.put("outletAttr1", concatWithDataAreaId(source, "SiteId", dataAreaId));
        result.put("address", getRawValue(source.get("Address")));
        result.put("outletAttr2", getRawValue(source.get("SWIFTNo")));
        result.put("outletAttr3", getRawValue(source.get("BankName")));

        String deactive = getRawValue(source.get("Deactive"));
        String custGroup = getRawValue(source.get("CustGroup"));
        String activeStatus = "Yes".equalsIgnoreCase(deactive) ? "inactive" : "active";

        if ((("KBPL".equalsIgnoreCase(dataAreaId) || "KGPL".equalsIgnoreCase(dataAreaId))
                && "COLEMPTY".equalsIgnoreCase(custGroup))
                || ("WBPL".equalsIgnoreCase(dataAreaId)
                && Arrays.asList("COLEMPTY", "COL-DMP", "COL-DST", "COL-RTL")
                .stream().anyMatch(cg -> cg.equalsIgnoreCase(custGroup)))) {
            activeStatus = "inactive";
        }

        result.put("activeStatus", activeStatus);
        result.put("latitude", sanitizeCoordinate(source.get("Latitude"), LAT_PATTERN));
        result.put("longitude", sanitizeCoordinate(source.get("Longitude"), LON_PATTERN));

        String contactNo = getRawValue(source.get("PrimaryContactPhone"));
        if (isValidMobile(contactNo)) {
            result.put("contactno", contactNo);
        } else {
            result.put("contactno", ""); // Always set a value, even if empty
        }

        result.put("outletType", getRawValue(source.get("PartyType")));
        result.put("outletAttr4", getRawValue(source.get("")));
        result.put("outletName", getRawValue(source.get("CustomerName")));

        String paymentMode = getRawValue(source.get("PaymentTerms"));
        result.put("paymentMode", paymentMode);
        result.put("outletAttr6", getRawValue(source.get("InvoiceAccount")));

        String calculateWithholdingTax = getRawValue(source.get("CalculateWithholdingTax"));
        result.put("tcsEligibility", "Yes".equalsIgnoreCase(calculateWithholdingTax) ? "true" : "false");

        // Build extendedAttributes as a plain string (like the reference code)
        result.put("extendedAttributes", buildExtendedAttributes(source, paymentMode));

        // Final safety check: ensure all values are Strings, not JsonNodes or other objects
        return sanitizeResultMap(result);
    }

    /**
     * Ensures all values in the result map are plain Strings, not JsonNodes or complex objects
     */
    private Map<String, Object> sanitizeResultMap(Map<String, Object> result) {
        Map<String, Object> sanitized = new HashMap<>();
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                sanitized.put(entry.getKey(), "");
            } else if (value instanceof String) {
                sanitized.put(entry.getKey(), value);
            } else if (value instanceof JsonNode) {
                sanitized.put(entry.getKey(), ((JsonNode) value).asText(""));
            } else {
                sanitized.put(entry.getKey(), value.toString());
            }
        }
        return sanitized;
    }

    private void validateFields(Map<String, Object> source) {
        String customerAccount = getRawValue(source.get("CustomerAccount"));
        if (customerAccount.isEmpty()) {
            throw new DataTransformationService.TransformationException("EntityValidation Failed: Field 'outletCode' cannot be empty ,CustomerAccount is missing");
        }
        String customerName = getRawValue(source.get("CustomerName"));
        if (customerName.isEmpty()) {
            throw new DataTransformationService.TransformationException("EntityValidation Failed: Field 'outletName' cannot be empty CustomerName is missing");
        }
    }

    /**
     * Safely extracts raw string value from any object type, including JsonNode
     * This method ensures ONLY String values are returned, never JsonNode or other objects
     */
    private String getRawValue(Object value) {
        if (value == null) {
            return "";
        }

        // Handle JsonNode objects if they come from the source
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            if (node.isNull() || node.isMissingNode()) {
                return "";
            }
            // Use asText() with empty string default
            String textValue = node.asText("");
            if (textValue == null || textValue.trim().isEmpty()) {
                return "";
            }
            return textValue.trim();
        }

        // Handle regular objects - ensure we return a clean string
        try {
            String strValue = value.toString();
            if (strValue == null || strValue.trim().isEmpty() || ObjectUtils.isEmpty(strValue.trim())) {
                return "";
            }
            return strValue.trim();
        } catch (Exception e) {
            // If toString() fails for any reason, return empty string
            return "";
        }
    }

    private boolean isValidMobile(String mobile) {
        return !mobile.isEmpty() && Pattern.matches("^[6-9]\\d{9}$", mobile);
    }

    /**
     * Concatenates value with dataAreaId, handling JsonNode objects
     */
    private String concatWithDataAreaId(Map<String, Object> source, String key, String dataAreaId) {
        String value = getRawValue(source.get(key));
        return value.isEmpty() ? "" : value + "-" + dataAreaId;
    }

    /**
     * Sanitizes coordinate values, ensuring they match valid lat/lon patterns
     */
    private String sanitizeCoordinate(Object value, Pattern pattern) {
        String s = getRawValue(value).trim();
        if (s.isEmpty()) return "0";
        return pattern.matcher(s).matches() ? s : "0";
    }

    /**
     * Builds locationHierarchy as a hierarchical string similar to the reference code
     * Format: "Area > City > Pincode > State > Country"
     */
    private String buildLocationHierarchy(Map<String, Object> source, String dataAreaId) {
        String area = getRawValue(source.get("AreaCode"));
        if (!area.isEmpty()) {
            area = area + "-" + dataAreaId;
        }

        String city = getRawValue(source.get("City"));
        String pincode = getRawValue(source.get("ZipCode"));
        String state = getRawValue(source.get("StateName"));
        String country = "India";

        StringBuilder hierarchy = new StringBuilder();

        if (!area.isEmpty()) {
            hierarchy.append(area.trim());
        }

        if (!city.isEmpty()) {
            if (hierarchy.length() > 0) hierarchy.append(" > ");
            hierarchy.append(city.trim());
        }

        if (!pincode.isEmpty()) {
            if (hierarchy.length() > 0) hierarchy.append(" > ");
            hierarchy.append(pincode.trim());
        }

        if (!state.isEmpty()) {
            if (hierarchy.length() > 0) hierarchy.append(" > ");
            hierarchy.append(state.trim());
        }

        if (hierarchy.length() > 0) hierarchy.append(" > ");
        hierarchy.append(country);

        return hierarchy.toString();
    }

    /**
     * Builds extendedAttributes as a formatted string
     * Format: "CreditLimit: value | DeactiveDate: value | salesHierarchyCode: value | preferredPaymentMode: value"
     */
    private String buildExtendedAttributes(Map<String, Object> source, String paymentMode) {
        String creditLimit = getRawValue(source.get("CreditLimit"));
        String deactiveDate = getRawValue(source.get("DeactiveDate"));
        String salesHierarchyCode = getRawValue(source.get("SalesHierarchyCode"));

        StringBuilder attributes = new StringBuilder();

        if (!creditLimit.isEmpty()) {
            attributes.append("CreditLimit: ").append(creditLimit);
        }

        if (!deactiveDate.isEmpty()) {
            if (attributes.length() > 0) attributes.append(" | ");
            attributes.append("DeactiveDate: ").append(deactiveDate);
        }

        if (!salesHierarchyCode.isEmpty()) {
            if (attributes.length() > 0) attributes.append(" | ");
            attributes.append("salesHierarchyCode: ").append(salesHierarchyCode);
        }

        if (!paymentMode.isEmpty()) {
            if (attributes.length() > 0) attributes.append(" | ");
            attributes.append("preferredPaymentMode: ").append(paymentMode);
        }

        return attributes.length() > 0 ? attributes.toString() : "";
    }
}