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

        HashMap<String, Object> result = new HashMap<>();

        String dataAreaId = getString(source, "dataAreaId");

        // Build locationHierarchy as a plain string (like the reference code)
        result.put("locationHierarchy", buildLocationHierarchy(source, dataAreaId));

        result.put("channel", concatWithDataAreaId(source, "Channel", dataAreaId));
        result.put("displayAddress", getString(source, "City") + "," + getString(source, "StateName"));
        result.put("source", dataAreaId);
        result.put("segment", concatWithDataAreaId(source, "SegmentId", dataAreaId));
        result.put("outletAttr5", concatWithDataAreaId(source, "ShipToCode", dataAreaId));
        result.put("subChannel", concatWithDataAreaId(source, "SubChannel", dataAreaId));
        result.put("outletClass", concatWithDataAreaId(source, "VPO", dataAreaId));

        String subsegment = getString(source, "SubsegmentId");
        String segment = getString(source, "SegmentId");
        if (!subsegment.isEmpty() && !segment.isEmpty()) {
            result.put("outletDivision", subsegment + "-" + segment + "-" + dataAreaId);
        } else {
            result.put("outletDivision", "");
        }

        result.put("outletCode", concatWithDataAreaId(source, "CustomerAccount", dataAreaId));
        result.put("outletCategory", concatWithDataAreaId(source, "CustomerCategory", dataAreaId));

        String discountGroup = getString(source, "LineDiscountCode");
        result.put("discountGroup", discountGroup.isEmpty() ? "NA" : discountGroup + "-" + dataAreaId);

        result.put("priceListId", concatWithDataAreaId(source, "DiscountPriceGroupId", dataAreaId));
        result.put("vpo", getString(source, "VPO"));
        result.put("email", getString(source, "PrimaryContactEmail"));
        result.put("gstNo", getString(source, "GSTIN"));
        result.put("outletAttr1", concatWithDataAreaId(source, "SiteId", dataAreaId));
        result.put("address", getString(source, "Address"));
        result.put("outletAttr2", getString(source, "SWIFTNo"));
        result.put("outletAttr3", getString(source, "BankName"));

        String deactive = getString(source, "Deactive");
        String custGroup = getString(source, "CustGroup");
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

        String contactNo = getString(source, "PrimaryContactPhone");
        if (isValidMobile(contactNo)) {
            result.put("contactno", contactNo);
        }

        result.put("outletType", getString(source, "PartyType"));
        result.put("outletAttr4", getString(source, ""));
        result.put("outletName", getString(source, "CustomerName"));

        String paymentMode = getString(source, "PaymentTerms");
        result.put("paymentMode", paymentMode);
        result.put("outletAttr6", getString(source, "InvoiceAccount"));

        String calculateWithholdingTax = getString(source, "CalculateWithholdingTax");
        result.put("tcsEligibility", "Yes".equalsIgnoreCase(calculateWithholdingTax) ? "true" : "false");

        // Build extendedAttributes as a plain string (like the reference code)
        result.put("extendedAttributes", buildExtendedAttributes(source, paymentMode));

        return result;
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
