package com.applicate.kgbpl.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;

// USE FLINK SHADED JACKSON - THIS IS CRITICAL
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class KgplCustomerMasterTransformer extends AbstractTransformer<JsonNode, Map<String, Object>> {

    private static final Pattern LAT_PATTERN =
            Pattern.compile("^[+-]?(?:90(?:\\.0+)?|(?:[0-8]?\\d)(?:\\.\\d+)?)$");
    private static final Pattern LON_PATTERN =
            Pattern.compile("^[+-]?(?:180(?:\\.0+)?|(?:1[0-7]\\d|\\d?\\d)(?:\\.\\d+)?)$");

    // Use Flink's shaded ObjectMapper
    private static final ObjectMapper objectMapper = new org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper();

    @Override
    public Map<String, Object> transform(JsonNode source) {
        validateFields(source);

        HashMap<String, Object> result = new HashMap<>();

        String dataAreaId = getJsonValue(source, "dataAreaId");

        // Build locationHierarchy as a plain string
        result.put("locationHierarchy", buildLocationHierarchy(source, dataAreaId));

        result.put("channel", concatWithDataAreaId(source, "Channel", dataAreaId));
        result.put("displayAddress", getJsonValue(source, "City") + "," + getJsonValue(source, "StateName"));
        result.put("source", dataAreaId);
        result.put("segment", concatWithDataAreaId(source, "SegmentId", dataAreaId));
        result.put("outletAttr5", concatWithDataAreaId(source, "ShipToCode", dataAreaId));
        result.put("subChannel", concatWithDataAreaId(source, "SubChannel", dataAreaId));
        result.put("outletClass", concatWithDataAreaId(source, "VPO", dataAreaId));

        String subsegment = getJsonValue(source, "SubsegmentId");
        String segment = getJsonValue(source, "SegmentId");
        if (!subsegment.isEmpty() && !segment.isEmpty()) {
            result.put("outletDivision", subsegment + "-" + segment + "-" + dataAreaId);
        } else {
            result.put("outletDivision", "");
        }

        result.put("outletCode", concatWithDataAreaId(source, "CustomerAccount", dataAreaId));
        result.put("outletCategory", concatWithDataAreaId(source, "CustomerCategory", dataAreaId));

        String discountGroup = getJsonValue(source, "LineDiscountCode");
        result.put("discountGroup", discountGroup.isEmpty() ? "NA" : discountGroup + "-" + dataAreaId);

        result.put("priceListId", concatWithDataAreaId(source, "DiscountPriceGroupId", dataAreaId));
        result.put("vpo", getJsonValue(source, "VPO"));
        result.put("email", getJsonValue(source, "PrimaryContactEmail"));
        result.put("gstNo", getJsonValue(source, "GSTIN"));
        result.put("outletAttr1", concatWithDataAreaId(source, "SiteId", dataAreaId));
        result.put("address", getJsonValue(source, "Address"));
        result.put("outletAttr2", getJsonValue(source, "SWIFTNo"));
        result.put("outletAttr3", getJsonValue(source, "BankName"));

        String deactive = getJsonValue(source, "Deactive");
        String custGroup = getJsonValue(source, "CustGroup");
        String activeStatus = "Yes".equalsIgnoreCase(deactive) ? "inactive" : "active";

        if ((("KBPL".equalsIgnoreCase(dataAreaId) || "KGPL".equalsIgnoreCase(dataAreaId))
                && "COLEMPTY".equalsIgnoreCase(custGroup))
                || ("WBPL".equalsIgnoreCase(dataAreaId)
                && Arrays.asList("COLEMPTY", "COL-DMP", "COL-DST", "COL-RTL")
                .stream().anyMatch(cg -> cg.equalsIgnoreCase(custGroup)))) {
            activeStatus = "inactive";
        }

        result.put("activeStatus", activeStatus);
        result.put("latitude", sanitizeCoordinate(source, "Latitude", LAT_PATTERN));
        result.put("longitude", sanitizeCoordinate(source, "Longitude", LON_PATTERN));

        String contactNo = getJsonValue(source, "PrimaryContactPhone");
        if (isValidMobile(contactNo)) {
            result.put("contactno", contactNo);
        }

        result.put("outletType", getJsonValue(source, "PartyType"));
        result.put("outletAttr4", "");
        result.put("outletName", getJsonValue(source, "CustomerName"));

        String paymentMode = getJsonValue(source, "PaymentTerms");
        result.put("paymentMode", paymentMode);
        result.put("outletAttr6", getJsonValue(source, "InvoiceAccount"));

        String calculateWithholdingTax = getJsonValue(source, "CalculateWithholdingTax");
        result.put("tcsEligibility", "Yes".equalsIgnoreCase(calculateWithholdingTax) ? "true" : "false");

        result.put("extendedAttributes", buildExtendedAttributes(source, paymentMode));

        return result;
    }

    private void validateFields(JsonNode source) {
        String customerAccount = getJsonValue(source, "CustomerAccount");
        if (customerAccount.isEmpty()) {
            throw new DataTransformationService.TransformationException(
                    "EntityValidation Failed: Field 'outletCode' cannot be empty, CustomerAccount is missing");
        }
        String customerName = getJsonValue(source, "CustomerName");
        if (customerName.isEmpty()) {
            throw new DataTransformationService.TransformationException(
                    "EntityValidation Failed: Field 'outletName' cannot be empty, CustomerName is missing");
        }
    }

    /**
     * Safely extracts a text value from JsonNode
     * Handles null, missing fields, and empty strings
     */
    private String getJsonValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return "";
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return "";
        }
        String value = fieldNode.asText();
        return (value != null && !value.trim().isEmpty()) ? value.trim() : "";
    }

    private boolean isValidMobile(String mobile) {
        return !mobile.isEmpty() && Pattern.matches("^[6-9]\\d{9}$", mobile);
    }

    private String concatWithDataAreaId(JsonNode source, String key, String dataAreaId) {
        String value = getJsonValue(source, key);
        return value.isEmpty() ? "" : value + "-" + dataAreaId;
    }

    private String sanitizeCoordinate(JsonNode source, String fieldName, Pattern pattern) {
        String s = getJsonValue(source, fieldName);
        if (s.isEmpty()) return "0";
        return pattern.matcher(s).matches() ? s : "0";
    }

    /**
     * Builds locationHierarchy as a hierarchical string
     * Format: "Area > City > Pincode > State > Country"
     */
    private String buildLocationHierarchy(JsonNode source, String dataAreaId) {
        String area = getJsonValue(source, "AreaCode");
        if (!area.isEmpty()) {
            area = area + "-" + dataAreaId;
        }

        String city = getJsonValue(source, "City");
        String pincode = getJsonValue(source, "ZipCode");
        String state = getJsonValue(source, "StateName");
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
    private String buildExtendedAttributes(JsonNode source, String paymentMode) {
        String creditLimit = getJsonValue(source, "CreditLimit");
        String deactiveDate = getJsonValue(source, "DeactiveDate");
        String salesHierarchyCode = getJsonValue(source, "SalesHierarchyCode");

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
