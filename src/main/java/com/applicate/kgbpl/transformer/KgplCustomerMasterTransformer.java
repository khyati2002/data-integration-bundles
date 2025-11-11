package com.applicate.kgbpl.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class KgplCustomerMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    // --- NEW: strict coordinate patterns (-90..90 for lat, -180..180 for lon) ---
    private static final Pattern LAT_PATTERN =
            Pattern.compile("^[+-]?(?:90(?:\\.0+)?|(?:[0-8]?\\d)(?:\\.\\d+)?)$");
    private static final Pattern LON_PATTERN =
            Pattern.compile("^[+-]?(?:180(?:\\.0+)?|(?:1[0-7]\\d|\\d?\\d)(?:\\.\\d+)?)$");

    @Override
    public Map<String, Object> transform(Map<String, Object> source) {
        validateFields(source);

        HashMap<String, Object> result = new HashMap<>();

        String dataAreaId = getRawValue(source.get("dataAreaId"));

        ObjectNode locationHierarchy = new ObjectMapper().createObjectNode();
        locationHierarchy.put("area", concatWithDataAreaId(source, "AreaCode", dataAreaId));
        locationHierarchy.put("city", getRawValue(source.get("City")));
        locationHierarchy.put("pincode", getRawValue(source.get("ZipCode")));
        locationHierarchy.put("state", getRawValue(source.get("StateName")));
        locationHierarchy.put("country", "India");
        locationHierarchy.put("areacode", concatWithDataAreaId(source, "AreaCode", dataAreaId));
        result.put("locationHierarchy", locationHierarchy);

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
        // Default active status
        String activeStatus = "Yes".equalsIgnoreCase(deactive) ? "inactive" : "active";

        // Additional business rules (case-insensitive)
        if ((("KBPL".equalsIgnoreCase(dataAreaId) || "KGPL".equalsIgnoreCase(dataAreaId))
                && "COLEMPTY".equalsIgnoreCase(custGroup))
                || ("WBPL".equalsIgnoreCase(dataAreaId)
                && Arrays.asList("COLEMPTY", "COL-DMP", "COL-DST", "COL-RTL")
                .stream().anyMatch(cg -> cg.equalsIgnoreCase(custGroup)))) {
            activeStatus = "inactive";
        }

        result.put("activeStatus", activeStatus);

        // --- UPDATED: sanitize coordinates using regex; default to "0" when invalid/blank ---
        result.put("latitude", sanitizeCoordinate(source.get("Latitude"), LAT_PATTERN));
        result.put("longitude", sanitizeCoordinate(source.get("Longitude"), LON_PATTERN));

        String contactNo = getRawValue(source.get("PrimaryContactPhone"));
        if (isValidMobile(contactNo)) {
            result.put("contactno", contactNo);
        }

        result.put("outletType", getRawValue(source.get("PartyType")));
        result.put("outletAttr4", getRawValue(source.get(""))); // kept as-is per original code
        result.put("outletName", getRawValue(source.get("CustomerName")));

        String paymentMode = getRawValue(source.get("PaymentTerms"));
        result.put("paymentMode", paymentMode);
        result.put("outletAttr6", getRawValue(source.get("InvoiceAccount")));

        String calculateWithholdingTax = getRawValue(source.get("CalculateWithholdingTax"));
        result.put("tcsEligibility", "Yes".equalsIgnoreCase(calculateWithholdingTax) ? "true" : "false");

        ObjectNode extended = new ObjectMapper().createObjectNode();
        extended.put("CreditLimit", getRawValue(source.get("CreditLimit")));
        extended.put("DeactiveDate", getRawValue(source.get("DeactiveDate")));
        extended.put("salesHierarchyCode", getRawValue(source.get("SalesHierarchyCode")));
        extended.put("preferredPaymentMode", paymentMode);
        result.put("extendedAttributes", extended);

        return result;
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

    private String getRawValue(Object value) {
        return (value != null && !ObjectUtils.isEmpty(value.toString().trim())) ? value.toString() : "";
    }

    private boolean isValidMobile(String mobile) {
        // Accepts 10-digit Indian numbers starting with 6-9
        return !mobile.isEmpty() && Pattern.matches("^[6-9]\\d{9}$", mobile);
    }

    private String concatWithDataAreaId(Map<String, Object> source, String key, String dataAreaId) {
        String value = getRawValue(source.get(key));
        return value.isEmpty() ? "" : value + "-" + dataAreaId;
    }

    // --- NEW: helper to sanitize coordinates using the patterns above ---
    private String sanitizeCoordinate(Object value, Pattern pattern) {
        String s = getRawValue(value).trim();
        if (s.isEmpty()) return "0";
        return pattern.matcher(s).matches() ? s : "0";
    }
}