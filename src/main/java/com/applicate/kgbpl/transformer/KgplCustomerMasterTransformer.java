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
        validateFields(source);

        HashMap<String, Object> result = new HashMap<>();

        String dataAreaId = getStringValue(source, "dataAreaId");

        // Build locationHierarchy as a plain string
        result.put("locationHierarchy", buildLocationHierarchy(source, dataAreaId));

        result.put("channel", concatWithDataAreaId(source, "Channel", dataAreaId));
        result.put("displayAddress", getStringValue(source, "City") + "," + getStringValue(source, "StateName"));
        result.put("source", dataAreaId);
        result.put("segment", concatWithDataAreaId(source, "SegmentId", dataAreaId));
        result.put("outletAttr5", concatWithDataAreaId(source, "ShipToCode", dataAreaId));
        result.put("subChannel", concatWithDataAreaId(source, "SubChannel", dataAreaId));
        result.put("outletClass", concatWithDataAreaId(source, "VPO", dataAreaId));

        String subsegment = getStringValue(source, "SubsegmentId");
        String segment = getStringValue(source, "SegmentId");
        if (!subsegment.isEmpty() && !segment.isEmpty()) {
            result.put("outletDivision", subsegment + "-" + segment + "-" + dataAreaId);
        } else {
            result.put("outletDivision", "");
        }

        result.put("outletCode", concatWithDataAreaId(source, "CustomerAccount", dataAreaId));
        result.put("outletCategory", concatWithDataAreaId(source, "CustomerCategory", dataAreaId));

        String discountGroup = getStringValue(source, "LineDiscountCode");
        result.put("discountGroup", discountGroup.isEmpty() ? "NA" : discountGroup + "-" + dataAreaId);

        result.put("priceListId", concatWithDataAreaId(source, "DiscountPriceGroupId", dataAreaId));
        result.put("vpo", getStringValue(source, "VPO"));
        result.put("email", getStringValue(source, "PrimaryContactEmail"));
        result.put("gstNo", getStringValue(source, "GSTIN"));
        result.put("outletAttr1", concatWithDataAreaId(source, "SiteId", dataAreaId));
        result.put("address", getStringValue(source, "Address"));
        result.put("outletAttr2", getStringValue(source, "SWIFTNo"));
        result.put("outletAttr3", getStringValue(source, "BankName"));

        String deactive = getStringValue(source, "Deactive");
        String custGroup = getStringValue(source, "CustGroup");
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

        String contactNo = getStringValue(source, "PrimaryContactPhone");
        if (isValidMobile(contactNo)) {
            result.put("contactno", contactNo);
        }

        result.put("outletType", getStringValue(source, "PartyType"));
        result.put("outletAttr4", getStringValue(source, ""));
        result.put("outletName", getStringValue(source, "CustomerName"));

        String paymentMode = getStringValue(source, "PaymentTerms");
        result.put("paymentMode", paymentMode);
        result.put("outletAttr6", getStringValue(source, "InvoiceAccount"));

        String calculateWithholdingTax = getStringValue(source, "CalculateWithholdingTax");
        result.put("tcsEligibility", "Yes".equalsIgnoreCase(calculateWithholdingTax) ? "true" : "false");

        // Build extendedAttributes as a plain string
        result.put("extendedAttributes", buildExtendedAttributes(source, paymentMode));

        return result;
    }

    private void validateFields(JsonNode source) {
        String customerAccount = getStringValue(source, "CustomerAccount");
        if (customerAccount.isEmpty()) {
            throw new DataTransformationService.TransformationException("EntityValidation Failed: Field 'outletCode' cannot be empty ,CustomerAccount is missing");
        }
        String customerName = getStringValue(source, "CustomerName");
        if (customerName.isEmpty()) {
            throw new DataTransformationService.TransformationException("EntityValidation Failed: Field 'outletName' cannot be empty CustomerName is missing");
        }
    }

    /**
     * Extract string directly from JsonNode - NO Map conversion, NO convertValue calls
     */
    private String getStringValue(JsonNode source, String key) {
        if (source == null || !source.has(key)) {
            return "";
        }
        JsonNode node = source.get(key);
        if (node == null || node.isNull() || node.isMissingNode()) {
            return "";
        }
        String value = node.asText().trim();
        return value.isEmpty() || "null".equals(value) ? "" : value;
    }

    private boolean isValidMobile(String mobile) {
        return !mobile.isEmpty() && Pattern.matches("^[6-9]\\d{9}$", mobile);
    }

    /**
     * Concatenates value with dataAreaId - using direct JsonNode access
     */
    private String concatWithDataAreaId(JsonNode source, String key, String dataAreaId) {
        String value = getStringValue(source, key);
        return value.isEmpty() ? "" : value + "-" + dataAreaId;
    }

    /**
     * Sanitizes coordinate values - using direct JsonNode access
     */
    private String sanitizeCoordinate(JsonNode source, String key, Pattern pattern) {
        String s = getStringValue(source, key);
        if (s.isEmpty()) return "0";
        return pattern.matcher(s).matches() ? s : "0";
    }

    /**
     * Builds locationHierarchy as a hierarchical string
     * Format: "Area > City > Pincode > State > Country"
     */
    private String buildLocationHierarchy(JsonNode source, String dataAreaId) {
        String area = getStringValue(source, "AreaCode");
        if (!area.isEmpty()) {
            area = area + "-" + dataAreaId;
        }

        String city = getStringValue(source, "City");
        String pincode = getStringValue(source, "ZipCode");
        String state = getStringValue(source, "StateName");
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
        String creditLimit = getStringValue(source, "CreditLimit");
        String deactiveDate = getStringValue(source, "DeactiveDate");
        String salesHierarchyCode = getStringValue(source, "SalesHierarchyCode");

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
