package com.applicate.kgbpl.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class KgplCustomerMasterTransformer extends AbstractTransformer<JsonNode, Map<String, Object>> {

    private static final Pattern LAT_PATTERN =
            Pattern.compile("^[+-]?(?:90(?:\\.0+)?|(?:[0-8]?\\d)(?:\\.\\d+)?)$");
    private static final Pattern LON_PATTERN =
            Pattern.compile("^[+-]?(?:180(?:\\.0+)?|(?:1[0-7]\\d|\\d?\\d)(?:\\.\\d+)?)$");

    @Override
    public Map<String, Object> transform(JsonNode source) {
        // Convert JsonNode to Map first
        Map<String, Object> sourceMap = convertJsonNodeToMap(source);

        validateFields(sourceMap);

        HashMap<String, Object> result = new HashMap<>();

        String dataAreaId = getString(sourceMap, "dataAreaId");

        // Build locationHierarchy as a plain string (like the reference code)
        result.put("locationHierarchy", buildLocationHierarchy(sourceMap, dataAreaId));

        result.put("channel", concatWithDataAreaId(sourceMap, "Channel", dataAreaId));
        result.put("displayAddress", getString(sourceMap, "City") + "," + getString(sourceMap, "StateName"));
        result.put("source", dataAreaId);
        result.put("segment", concatWithDataAreaId(sourceMap, "SegmentId", dataAreaId));
        result.put("outletAttr5", concatWithDataAreaId(sourceMap, "ShipToCode", dataAreaId));
        result.put("subChannel", concatWithDataAreaId(sourceMap, "SubChannel", dataAreaId));
        result.put("outletClass", concatWithDataAreaId(sourceMap, "VPO", dataAreaId));

        String subsegment = getString(sourceMap, "SubsegmentId");
        String segment = getString(sourceMap, "SegmentId");
        if (!subsegment.isEmpty() && !segment.isEmpty()) {
            result.put("outletDivision", subsegment + "-" + segment + "-" + dataAreaId);
        } else {
            result.put("outletDivision", "");
        }

        result.put("outletCode", concatWithDataAreaId(sourceMap, "CustomerAccount", dataAreaId));
        result.put("outletCategory", concatWithDataAreaId(sourceMap, "CustomerCategory", dataAreaId));

        String discountGroup = getString(sourceMap, "LineDiscountCode");
        result.put("discountGroup", discountGroup.isEmpty() ? "NA" : discountGroup + "-" + dataAreaId);

        result.put("priceListId", concatWithDataAreaId(sourceMap, "DiscountPriceGroupId", dataAreaId));
        result.put("vpo", getString(sourceMap, "VPO"));
        result.put("email", getString(sourceMap, "PrimaryContactEmail"));
        result.put("gstNo", getString(sourceMap, "GSTIN"));
        result.put("outletAttr1", concatWithDataAreaId(sourceMap, "SiteId", dataAreaId));
        result.put("address", getString(sourceMap, "Address"));
        result.put("outletAttr2", getString(sourceMap, "SWIFTNo"));
        result.put("outletAttr3", getString(sourceMap, "BankName"));

        String deactive = getString(sourceMap, "Deactive");
        String custGroup = getString(sourceMap, "CustGroup");
        String activeStatus = "Yes".equalsIgnoreCase(deactive) ? "inactive" : "active";

        if ((("KBPL".equalsIgnoreCase(dataAreaId) || "KGPL".equalsIgnoreCase(dataAreaId))
                && "COLEMPTY".equalsIgnoreCase(custGroup))
                || ("WBPL".equalsIgnoreCase(dataAreaId)
                && Arrays.asList("COLEMPTY", "COL-DMP", "COL-DST", "COL-RTL")
                .stream().anyMatch(cg -> cg.equalsIgnoreCase(custGroup)))) {
            activeStatus = "inactive";
        }

        result.put("activeStatus", activeStatus);
        result.put("latitude", sanitizeCoordinate(sourceMap, "Latitude", LAT_PATTERN));
        result.put("longitude", sanitizeCoordinate(sourceMap, "Longitude", LON_PATTERN));

        String contactNo = getString(sourceMap, "PrimaryContactPhone");
        if (isValidMobile(contactNo)) {
            result.put("contactno", contactNo);
        }

        result.put("outletType", getString(sourceMap, "PartyType"));
        result.put("outletAttr4", getString(sourceMap, ""));
        result.put("outletName", getString(sourceMap, "CustomerName"));

        String paymentMode = getString(sourceMap, "PaymentTerms");
        result.put("paymentMode", paymentMode);
        result.put("outletAttr6", getString(sourceMap, "InvoiceAccount"));

        String calculateWithholdingTax = getString(sourceMap, "CalculateWithholdingTax");
        result.put("tcsEligibility", "Yes".equalsIgnoreCase(calculateWithholdingTax) ? "true" : "false");

        // Build extendedAttributes as a plain string (like the reference code)
        result.put("extendedAttributes", buildExtendedAttributes(sourceMap, paymentMode));

        return result;
    }

    /**
     * Convert JsonNode to Map<String, Object>
     * Extracts all fields and converts them to string values
     */
    private Map<String, Object> convertJsonNodeToMap(JsonNode node) {
        Map<String, Object> map = new HashMap<>();
        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            // Convert JsonNode values to strings, handling all types
            if (value == null || value.isNull() || value.isMissingNode()) {
                map.put(key, null);
            } else {
                map.put(key, value.asText());
            }
        });
        return map;
    }

    private void validateFields(Map<String, Object> source) {
        String customerAccount = getString(source, "CustomerAccount");
        if (customerAccount.isEmpty()) {
            throw new DataTransformationService.TransformationException("EntityValidation Failed: Field 'outletCode' cannot be empty ,CustomerAccount is missing");
        }
        String customerName = getString(source, "CustomerName");
        if (customerName.isEmpty()) {
            throw new DataTransformationService.TransformationException("EntityValidation Failed: Field 'outletName' cannot be empty CustomerName is missing");
        }
    }

    /**
     * Simple null-safe string extractor - returns empty string if null or empty
     */
    private String getString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return "";
        }
        String strValue = String.valueOf(value).trim();
        return strValue.isEmpty() || "null".equals(strValue) ? "" : strValue;
    }

    private boolean isValidMobile(String mobile) {
        return !mobile.isEmpty() && Pattern.matches("^[6-9]\\d{9}$", mobile);
    }

    /**
     * Concatenates value with dataAreaId
     */
    private String concatWithDataAreaId(Map<String, Object> source, String key, String dataAreaId) {
        String value = getString(source, key);
        return value.isEmpty() ? "" : value + "-" + dataAreaId;
    }

    /**
     * Sanitizes coordinate values, ensuring they match valid lat/lon patterns
     */
    private String sanitizeCoordinate(Map<String, Object> source, String key, Pattern pattern) {
        String s = getString(source, key);
        if (s.isEmpty()) return "0";
        return pattern.matcher(s).matches() ? s : "0";
    }

    /**
     * Builds locationHierarchy as a hierarchical string similar to the reference code
     * Format: "Area > City > Pincode > State > Country"
     */
    private String buildLocationHierarchy(Map<String, Object> source, String dataAreaId) {
        String area = getString(source, "AreaCode");
        if (!area.isEmpty()) {
            area = area + "-" + dataAreaId;
        }

        String city = getString(source, "City");
        String pincode = getString(source, "ZipCode");
        String state = getString(source, "StateName");
        String country = "India";

        StringBuilder hierarchy = new StringBuilder();

        if (!area.isEmpty()) {
            hierarchy.append(area);
        }

        if (!city.isEmpty()) {
            if (hierarchy.length() > 0) hierarchy.append(" > ");
            hierarchy.append(city);
        }

        if (!pincode.isEmpty()) {
            if (hierarchy.length() > 0) hierarchy.append(" > ");
            hierarchy.append(pincode);
        }

        if (!state.isEmpty()) {
            if (hierarchy.length() > 0) hierarchy.append(" > ");
            hierarchy.append(state);
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
        String creditLimit = getString(source, "CreditLimit");
        String deactiveDate = getString(source, "DeactiveDate");
        String salesHierarchyCode = getString(source, "SalesHierarchyCode");

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
