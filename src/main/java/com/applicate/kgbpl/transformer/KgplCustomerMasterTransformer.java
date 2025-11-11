package com.applicate.kgbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.regex.Pattern;

public class KgplCustomerMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    // --- Strict coordinate patterns ---
    private static final Pattern LAT_PATTERN =
            Pattern.compile("^[+-]?(?:90(?:\\.0+)?|(?:[0-8]?\\d)(?:\\.\\d+)?)$");
    private static final Pattern LON_PATTERN =
            Pattern.compile("^[+-]?(?:180(?:\\.0+)?|(?:1[0-7]\\d|\\d?\\d)(?:\\.\\d+)?)$");

    @Override
    public Map<String, Object> transform(Map<String, Object> source) {
        validateFields(source);

        Map<String, Object> result = new HashMap<>();
        String dataAreaId = getRawValue(source.get("dataAreaId"));

        // Location hierarchy
        result.put("locationHierarchy", setClientLocationHierarchy(source, dataAreaId));

        // Basic details
        result.put("channel", concatWithDataAreaId(source, "Channel", dataAreaId));
        result.put("displayAddress", setDisplayAddress(source));
        result.put("source", dataAreaId);
        result.put("segment", concatWithDataAreaId(source, "SegmentId", dataAreaId));
        result.put("outletAttr5", concatWithDataAreaId(source, "ShipToCode", dataAreaId));
        result.put("subChannel", concatWithDataAreaId(source, "SubChannel", dataAreaId));
        result.put("outletClass", concatWithDataAreaId(source, "VPO", dataAreaId));
        result.put("outletDivision", buildOutletDivision(source, dataAreaId));

        // Identifiers
        result.put("outletCode", concatWithDataAreaId(source, "CustomerAccount", dataAreaId));
        result.put("outletCategory", concatWithDataAreaId(source, "CustomerCategory", dataAreaId));

        // Discount group logic
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

        // Active status logic
        result.put("activeStatus", determineActiveStatus(source, dataAreaId));

        // Coordinates
        result.put("latitude", sanitizeCoordinate(source.get("Latitude"), LAT_PATTERN));
        result.put("longitude", sanitizeCoordinate(source.get("Longitude"), LON_PATTERN));

        // Contact
        String contactNo = getValidContact(source.get("PrimaryContactPhone"));
        if (!contactNo.isEmpty()) {
            result.put("contactno", contactNo);
        }

        // Outlet details
        result.put("outletType", getRawValue(source.get("PartyType")));
        result.put("outletAttr4", getRawValue(source.get(""))); // left as-is per original code
        result.put("outletName", getRawValue(source.get("CustomerName")));

        // Payment & TCS details
        String paymentMode = getRawValue(source.get("PaymentTerms"));
        result.put("paymentMode", paymentMode);
        result.put("outletAttr6", getRawValue(source.get("InvoiceAccount")));

        String calculateWithholdingTax = getRawValue(source.get("CalculateWithholdingTax"));
        result.put("tcsEligibility", "Yes".equalsIgnoreCase(calculateWithholdingTax) ? "true" : "false");

        // Extended attributes
        result.put("extendedAttributes", buildExtendedAttributes(source, paymentMode));

        return result;
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private JsonNode setClientLocationHierarchy(Map<String, Object> source, String dataAreaId) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode locationHierarchy = mapper.createObjectNode();

        String area = concatWithDataAreaId(source, "AreaCode", dataAreaId);
        String city = getRawValue(source.get("City"));
        String pincode = getRawValue(source.get("ZipCode"));
        String state = getRawValue(source.get("StateName"));
        String country = "India";

        locationHierarchy.put("area", area);
        locationHierarchy.put("city", city);
        locationHierarchy.put("pincode", pincode);
        locationHierarchy.put("state", state);
        locationHierarchy.put("country", country);
        locationHierarchy.put("areacode", area);

        return JSONUtils.getObjectMapper().convertValue(locationHierarchy, JsonNode.class);
    }

    private String setDisplayAddress(Map<String, Object> source) {
        String city = getRawValue(source.get("City"));
        String state = getRawValue(source.get("StateName"));
        return (city.isEmpty() && state.isEmpty()) ? "" : city + "," + state;
    }

    private String buildOutletDivision(Map<String, Object> source, String dataAreaId) {
        String subsegment = getRawValue(source.get("SubsegmentId"));
        String segment = getRawValue(source.get("SegmentId"));
        if (!subsegment.isEmpty() && !segment.isEmpty()) {
            return subsegment + "-" + segment + "-" + dataAreaId;
        }
        return "";
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

    private JsonNode buildExtendedAttributes(Map<String, Object> source, String paymentMode) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode extended = mapper.createObjectNode();

        extended.put("CreditLimit", getRawValue(source.get("CreditLimit")));
        extended.put("DeactiveDate", getRawValue(source.get("DeactiveDate")));
        extended.put("salesHierarchyCode", getRawValue(source.get("SalesHierarchyCode")));
        extended.put("preferredPaymentMode", paymentMode);

        return JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class);
    }

    // -------------------------------------------------------------------------
    // Utility methods (unchanged core logic)
    // -------------------------------------------------------------------------

    private void validateFields(Map<String, Object> source) {
        String customerAccount = getRawValue(source.get("CustomerAccount"));
        if (customerAccount.isEmpty()) {
            throw new DataTransformationService.TransformationException(
                    "EntityValidation Failed: Field 'outletCode' cannot be empty ,CustomerAccount is missing");
        }
        String customerName = getRawValue(source.get("CustomerName"));
        if (customerName.isEmpty()) {
            throw new DataTransformationService.TransformationException(
                    "EntityValidation Failed: Field 'outletName' cannot be empty CustomerName is missing");
        }
    }

    private String getRawValue(Object value) {
        return (value != null && !ObjectUtils.isEmpty(value.toString().trim())) ? value.toString() : "";
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