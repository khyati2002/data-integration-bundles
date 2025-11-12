package com.applicate.kgbpl.transformer;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;

import java.util.*;
import java.util.regex.Pattern;

public class KgplCustomerMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final Pattern LAT_PATTERN =
            Pattern.compile("^[+-]?(?:90(?:\\.0+)?|(?:[0-8]?\\d)(?:\\.\\d+)?)$");
    private static final Pattern LON_PATTERN =
            Pattern.compile("^[+-]?(?:180(?:\\.0+)?|(?:1[0-7]\\d|\\d?\\d)(?:\\.\\d+)?)$");

    @Override
    public Map<String, Object> transform(Map<String, Object> source) {
        validateFields(source);

        Map<String, Object> result = new HashMap<>();
        String dataAreaId = getRawValue(source.get("dataAreaId"));

        // ---------------- Location Hierarchy ----------------
        result.put("locationHierarchy", setClientLocationHierarchy(source, dataAreaId));

        // ---------------- Basic Fields ----------------
        result.put("channel", concatWithDataAreaId(source, "Channel", dataAreaId));
        result.put("displayAddress", setDisplayAddress(source));
        result.put("source", dataAreaId);
        result.put("segment", concatWithDataAreaId(source, "SegmentId", dataAreaId));
        result.put("outletAttr5", concatWithDataAreaId(source, "ShipToCode", dataAreaId));
        result.put("subChannel", concatWithDataAreaId(source, "SubChannel", dataAreaId));
        result.put("outletClass", concatWithDataAreaId(source, "VPO", dataAreaId));
        result.put("outletDivision", buildOutletDivision(source, dataAreaId));

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

        // ---------------- Active Status ----------------
        result.put("activeStatus", determineActiveStatus(source, dataAreaId));

        // ---------------- Coordinates ----------------
        result.put("latitude", sanitizeCoordinate(source.get("Latitude"), LAT_PATTERN));
        result.put("longitude", sanitizeCoordinate(source.get("Longitude"), LON_PATTERN));

        // ---------------- Contact ----------------
        String contactNo = getValidContact(source.get("PrimaryContactPhone"));
        if (!contactNo.isEmpty()) result.put("contactno", contactNo);

        // ---------------- Miscellaneous ----------------
        result.put("outletType", getRawValue(source.get("PartyType")));
        result.put("outletAttr4", getRawValue(source.get(""))); // kept as-is
        result.put("outletName", getRawValue(source.get("CustomerName")));

        String paymentMode = getRawValue(source.get("PaymentTerms"));
        result.put("paymentMode", paymentMode);
        result.put("outletAttr6", getRawValue(source.get("InvoiceAccount")));

        String calculateWithholdingTax = getRawValue(source.get("CalculateWithholdingTax"));
//        result.put("tcsEligibility", "Yes".equalsIgnoreCase(calculateWithholdingTax) ? "true" : "false");
        result.put("tcsEligibility", (byte) ("Yes".equalsIgnoreCase(calculateWithholdingTax) ? 1 : 0));
        // ---------------- Extended Attributes ----------------
        result.put("extendedAttributes", buildExtendedAttributes(source, paymentMode));

        return result;
    }

    // ----------------------------------------------------------------------
    // Helper methods (Flink-shaded Jackson)
    // ----------------------------------------------------------------------

    private String setClientLocationHierarchy(Map<String, Object> source, String dataAreaId) {
        String area = concatWithDataAreaId(source, "AreaCode", dataAreaId);
        String city = getRawValue(source.get("City"));
        String pincode = getRawValue(source.get("ZipCode"));
        String state = getRawValue(source.get("StateName"));
        String country = "India";

        StringBuilder location = new StringBuilder();

        if (!area.isEmpty()) location.append(area.trim());
        if (!city.isEmpty()) {
            if (location.length() > 0) location.append(" > ");
            location.append(city.trim());
        }
        if (!state.isEmpty()) {
            if (location.length() > 0) location.append(" > ");
            location.append(state.trim());
        }
        if (!pincode.isEmpty()) {
            if (location.length() > 0) location.append(" > ");
            location.append(pincode.trim());
        }
        if (!country.isEmpty()) {
            if (location.length() > 0) location.append(" > ");
            location.append(country.trim());
        }

        return location.length() > 0 ? location.toString() : "India";
    }

    private String setDisplayAddress(Map<String, Object> source) {
        String city = getRawValue(source.get("City"));
        String state = getRawValue(source.get("StateName"));
        return (city.isEmpty() && state.isEmpty()) ? "" : city + "," + state;
    }


    private String determineActiveStatus(Map<String, Object> source, String dataAreaId) {
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
        return activeStatus;
    }

    private String getValidContact(Object mobileObj) {
        String mobile = getRawValue(mobileObj);
        return isValidMobile(mobile) ? mobile : "";
    }
    private String flattenToString(Object value) {
        if (value == null) return "";
        try {
            if (value instanceof org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode) {
                org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode node =
                        (org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode) value;
                return node.isValueNode() ? node.asText("") : node.toString();
            }
            String s = value.toString().trim();
            return s.isEmpty() ? "" : s;
        } catch (Exception e) {
            return "";
        }
    }

    private String buildOutletDivision(Map<String, Object> source, String dataAreaId) {
        String subsegment = flattenToString(source.get("SubsegmentId"));
        String segment = flattenToString(source.get("SegmentId"));
        String dataArea = flattenToString(dataAreaId);

        if (!subsegment.isEmpty() && !segment.isEmpty()) {
            return subsegment + "-" + segment + "-" + dataArea;
        } else if (!segment.isEmpty()) {
            return segment + "-" + dataArea;
        } else if (!subsegment.isEmpty()) {
            return subsegment + "-" + dataArea;
        }
        return "";
    }
    private String buildExtendedAttributes(Map<String, Object> source, String paymentMode) {
        String creditLimit = getRawValue(source.get("CreditLimit"));
        String deactiveDate = getRawValue(source.get("DeactiveDate"));
        String salesHierarchyCode = getRawValue(source.get("SalesHierarchyCode"));
        String preferredPaymentMode = paymentMode;

        StringBuilder extended = new StringBuilder();
        extended.append("CreditLimit=").append(creditLimit.isEmpty() ? "NA" : creditLimit);
        extended.append(" | DeactiveDate=").append(deactiveDate.isEmpty() ? "NA" : deactiveDate);
        extended.append(" | SalesHierarchyCode=").append(salesHierarchyCode.isEmpty() ? "NA" : salesHierarchyCode);
        extended.append(" | PreferredPaymentMode=").append(preferredPaymentMode.isEmpty() ? "NA" : preferredPaymentMode);

        return extended.toString();
    }
    // ----------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------

    private void validateFields(Map<String, Object> source) {
        String customerAccount = getRawValue(source.get("CustomerAccount"));
        if (customerAccount.isEmpty())
            throw new DataTransformationService.TransformationException(
                    "EntityValidation Failed: Field 'outletCode' cannot be empty ,CustomerAccount is missing");
        String customerName = getRawValue(source.get("CustomerName"));
        if (customerName.isEmpty())
            throw new DataTransformationService.TransformationException(
                    "EntityValidation Failed: Field 'outletName' cannot be empty CustomerName is missing");
    }

    private String getRawValue(Object value) {
        if (value == null) return "";
        if (value instanceof String) {
            String s = ((String) value).trim();
            return s.isEmpty() ? "" : s;
        }
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            if (node.isValueNode()) return node.asText("");
            return node.toString();
        }
        return value.toString().trim();
    }

    private boolean isValidMobile(String mobile) {
        return !mobile.isEmpty() && Pattern.matches("^[6-9]\\d{9}$", mobile);
    }

    private String concatWithDataAreaId(Map<String, Object> source, String key, String dataAreaId) {
        String value = getRawValue(source.get(key));
        return value.isEmpty() ? "" : value + "-" + dataAreaId;
    }

    private String sanitizeCoordinate(Object value, Pattern pattern) {
        String s = getRawValue(value).trim();
        if (s.isEmpty()) return "0";
        return pattern.matcher(s).matches() ? s : "0";
    }
}