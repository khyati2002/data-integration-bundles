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
        System.out.println("========== TRANSFORM START ==========");
        System.out.println("Source JsonNode type: " + (source == null ? "null" : source.getClass().getName()));

        try {
            validateFields(source);
            System.out.println("DEBUG: Validation passed");

            HashMap<String, Object> result = new HashMap<>();

            String dataAreaId = getStringValue(source, "dataAreaId");
            System.out.println("DEBUG: dataAreaId = " + dataAreaId);

            // Build locationHierarchy
            String locationHierarchy = buildLocationHierarchy(source, dataAreaId);
            System.out.println("DEBUG: locationHierarchy = " + locationHierarchy + " [" + locationHierarchy.getClass().getName() + "]");
            result.put("locationHierarchy", locationHierarchy);

            String channel = concatWithDataAreaId(source, "Channel", dataAreaId);
            System.out.println("DEBUG: channel = " + channel + " [" + channel.getClass().getName() + "]");
            result.put("channel", channel);

            String displayAddress = getStringValue(source, "City") + "," + getStringValue(source, "StateName");
            System.out.println("DEBUG: displayAddress = " + displayAddress + " [" + displayAddress.getClass().getName() + "]");
            result.put("displayAddress", displayAddress);

            System.out.println("DEBUG: Setting source = " + dataAreaId);
            result.put("source", dataAreaId);

            String segment = concatWithDataAreaId(source, "SegmentId", dataAreaId);
            System.out.println("DEBUG: segment = " + segment + " [" + segment.getClass().getName() + "]");
            result.put("segment", segment);

            String outletAttr5 = concatWithDataAreaId(source, "ShipToCode", dataAreaId);
            System.out.println("DEBUG: outletAttr5 = " + outletAttr5 + " [" + outletAttr5.getClass().getName() + "]");
            result.put("outletAttr5", outletAttr5);

            String subChannel = concatWithDataAreaId(source, "SubChannel", dataAreaId);
            System.out.println("DEBUG: subChannel = " + subChannel + " [" + subChannel.getClass().getName() + "]");
            result.put("subChannel", subChannel);

            String outletClass = concatWithDataAreaId(source, "VPO", dataAreaId);
            System.out.println("DEBUG: outletClass = " + outletClass + " [" + outletClass.getClass().getName() + "]");
            result.put("outletClass", outletClass);

            String subsegment = getStringValue(source, "SubsegmentId");
            String segmentValue = getStringValue(source, "SegmentId");
            System.out.println("DEBUG: subsegment = " + subsegment + ", segmentValue = " + segmentValue);

            if (!subsegment.isEmpty() && !segmentValue.isEmpty()) {
                String outletDivision = subsegment + "-" + segmentValue + "-" + dataAreaId;
                System.out.println("DEBUG: outletDivision = " + outletDivision + " [" + outletDivision.getClass().getName() + "]");
                result.put("outletDivision", outletDivision);
            } else {
                System.out.println("DEBUG: outletDivision = (empty)");
                result.put("outletDivision", "");
            }

            String outletCode = concatWithDataAreaId(source, "CustomerAccount", dataAreaId);
            System.out.println("DEBUG: outletCode = " + outletCode + " [" + outletCode.getClass().getName() + "]");
            result.put("outletCode", outletCode);

            String outletCategory = concatWithDataAreaId(source, "CustomerCategory", dataAreaId);
            System.out.println("DEBUG: outletCategory = " + outletCategory + " [" + outletCategory.getClass().getName() + "]");
            result.put("outletCategory", outletCategory);

            String discountGroup = getStringValue(source, "LineDiscountCode");
            String discountGroupValue = discountGroup.isEmpty() ? "NA" : discountGroup + "-" + dataAreaId;
            System.out.println("DEBUG: discountGroup = " + discountGroupValue + " [" + discountGroupValue.getClass().getName() + "]");
            result.put("discountGroup", discountGroupValue);

            String priceListId = concatWithDataAreaId(source, "DiscountPriceGroupId", dataAreaId);
            System.out.println("DEBUG: priceListId = " + priceListId + " [" + priceListId.getClass().getName() + "]");
            result.put("priceListId", priceListId);

            String vpo = getStringValue(source, "VPO");
            System.out.println("DEBUG: vpo = " + vpo + " [" + vpo.getClass().getName() + "]");
            result.put("vpo", vpo);

            String email = getStringValue(source, "PrimaryContactEmail");
            System.out.println("DEBUG: email = " + email + " [" + email.getClass().getName() + "]");
            result.put("email", email);

            String gstNo = getStringValue(source, "GSTIN");
            System.out.println("DEBUG: gstNo = " + gstNo + " [" + gstNo.getClass().getName() + "]");
            result.put("gstNo", gstNo);

            String outletAttr1 = concatWithDataAreaId(source, "SiteId", dataAreaId);
            System.out.println("DEBUG: outletAttr1 = " + outletAttr1 + " [" + outletAttr1.getClass().getName() + "]");
            result.put("outletAttr1", outletAttr1);

            String address = getStringValue(source, "Address");
            System.out.println("DEBUG: address = " + address + " [" + address.getClass().getName() + "]");
            result.put("address", address);

            String outletAttr2 = getStringValue(source, "SWIFTNo");
            System.out.println("DEBUG: outletAttr2 = " + outletAttr2 + " [" + outletAttr2.getClass().getName() + "]");
            result.put("outletAttr2", outletAttr2);

            String outletAttr3 = getStringValue(source, "BankName");
            System.out.println("DEBUG: outletAttr3 = " + outletAttr3 + " [" + outletAttr3.getClass().getName() + "]");
            result.put("outletAttr3", outletAttr3);

            String deactive = getStringValue(source, "Deactive");
            String custGroup = getStringValue(source, "CustGroup");
            System.out.println("DEBUG: deactive = " + deactive + ", custGroup = " + custGroup);

            String activeStatus = "Yes".equalsIgnoreCase(deactive) ? "inactive" : "active";

            if ((("KBPL".equalsIgnoreCase(dataAreaId) || "KGPL".equalsIgnoreCase(dataAreaId))
                    && "COLEMPTY".equalsIgnoreCase(custGroup))
                    || ("WBPL".equalsIgnoreCase(dataAreaId)
                    && Arrays.asList("COLEMPTY", "COL-DMP", "COL-DST", "COL-RTL")
                    .stream().anyMatch(cg -> cg.equalsIgnoreCase(custGroup)))) {
                activeStatus = "inactive";
            }
            System.out.println("DEBUG: activeStatus = " + activeStatus + " [" + activeStatus.getClass().getName() + "]");
            result.put("activeStatus", activeStatus);

            String latitude = sanitizeCoordinate(source, "Latitude", LAT_PATTERN);
            System.out.println("DEBUG: latitude = " + latitude + " [" + latitude.getClass().getName() + "]");
            result.put("latitude", latitude);

            String longitude = sanitizeCoordinate(source, "Longitude", LON_PATTERN);
            System.out.println("DEBUG: longitude = " + longitude + " [" + longitude.getClass().getName() + "]");
            result.put("longitude", longitude);

            String contactNo = getStringValue(source, "PrimaryContactPhone");
            System.out.println("DEBUG: contactNo = " + contactNo);
            if (isValidMobile(contactNo)) {
                System.out.println("DEBUG: Setting contactno = " + contactNo + " [" + contactNo.getClass().getName() + "]");
                result.put("contactno", contactNo);
            }

            String outletType = getStringValue(source, "PartyType");
            System.out.println("DEBUG: outletType = " + outletType + " [" + outletType.getClass().getName() + "]");
            result.put("outletType", outletType);

            String outletAttr4 = getStringValue(source, "");
            System.out.println("DEBUG: outletAttr4 = " + outletAttr4 + " [" + outletAttr4.getClass().getName() + "]");
            result.put("outletAttr4", outletAttr4);

            String outletName = getStringValue(source, "CustomerName");
            System.out.println("DEBUG: outletName = " + outletName + " [" + outletName.getClass().getName() + "]");
            result.put("outletName", outletName);

            String paymentMode = getStringValue(source, "PaymentTerms");
            System.out.println("DEBUG: paymentMode = " + paymentMode + " [" + paymentMode.getClass().getName() + "]");
            result.put("paymentMode", paymentMode);

            String outletAttr6 = getStringValue(source, "InvoiceAccount");
            System.out.println("DEBUG: outletAttr6 = " + outletAttr6 + " [" + outletAttr6.getClass().getName() + "]");
            result.put("outletAttr6", outletAttr6);

            String calculateWithholdingTax = getStringValue(source, "CalculateWithholdingTax");
            String tcsEligibility = "Yes".equalsIgnoreCase(calculateWithholdingTax) ? "true" : "false";
            System.out.println("DEBUG: tcsEligibility = " + tcsEligibility + " [" + tcsEligibility.getClass().getName() + "]");
            result.put("tcsEligibility", tcsEligibility);

            String extendedAttributes = buildExtendedAttributes(source, paymentMode);
            System.out.println("DEBUG: extendedAttributes = " + extendedAttributes + " [" + extendedAttributes.getClass().getName() + "]");
            result.put("extendedAttributes", extendedAttributes);

            System.out.println("\n========== FINAL RESULT MAP ==========");
            result.forEach((key, value) -> {
                System.out.println(key + " = " + value + " [" + (value == null ? "null" : value.getClass().getName()) + "]");
            });
            System.out.println("========== TRANSFORM END ==========\n");

            return result;

        } catch (Exception e) {
            System.err.println("ERROR in transform: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void validateFields(JsonNode source) {
        System.out.println("DEBUG: Validating fields...");
        String customerAccount = getStringValue(source, "CustomerAccount");
        System.out.println("DEBUG: customerAccount for validation = " + customerAccount);
        if (customerAccount.isEmpty()) {
            throw new DataTransformationService.TransformationException("EntityValidation Failed: Field 'outletCode' cannot be empty ,CustomerAccount is missing");
        }
        String customerName = getStringValue(source, "CustomerName");
        System.out.println("DEBUG: customerName for validation = " + customerName);
        if (customerName.isEmpty()) {
            throw new DataTransformationService.TransformationException("EntityValidation Failed: Field 'outletName' cannot be empty CustomerName is missing");
        }
    }

    /**
     * Extract string directly from JsonNode - NO Map conversion, NO convertValue calls
     */
    private String getStringValue(JsonNode source, String key) {
        try {
            System.out.println("DEBUG: getStringValue called for key: " + key);
            if (source == null) {
                System.out.println("DEBUG: source is null for key: " + key);
                return "";
            }
            if (!source.has(key)) {
                System.out.println("DEBUG: source does not have key: " + key);
                return "";
            }
            JsonNode node = source.get(key);
            if (node == null || node.isNull() || node.isMissingNode()) {
                System.out.println("DEBUG: node is null/missing for key: " + key);
                return "";
            }
            String value = node.asText().trim();
            String result = value.isEmpty() || "null".equals(value) ? "" : value;
            System.out.println("DEBUG: getStringValue(" + key + ") = '" + result + "'");
            return result;
        } catch (Exception e) {
            System.err.println("ERROR in getStringValue for key '" + key + "': " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private boolean isValidMobile(String mobile) {
        boolean valid = !mobile.isEmpty() && Pattern.matches("^[6-9]\\d{9}$", mobile);
        System.out.println("DEBUG: isValidMobile(" + mobile + ") = " + valid);
        return valid;
    }

    /**
     * Concatenates value with dataAreaId - using direct JsonNode access
     */
    private String concatWithDataAreaId(JsonNode source, String key, String dataAreaId) {
        try {
            System.out.println("DEBUG: concatWithDataAreaId called for key: " + key);
            String value = getStringValue(source, key);
            String result = value.isEmpty() ? "" : value + "-" + dataAreaId;
            System.out.println("DEBUG: concatWithDataAreaId(" + key + ") = '" + result + "'");
            return result;
        } catch (Exception e) {
            System.err.println("ERROR in concatWithDataAreaId for key '" + key + "': " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Sanitizes coordinate values - using direct JsonNode access
     */
    private String sanitizeCoordinate(JsonNode source, String key, Pattern pattern) {
        try {
            System.out.println("DEBUG: sanitizeCoordinate called for key: " + key);
            String s = getStringValue(source, key);
            if (s.isEmpty()) {
                System.out.println("DEBUG: sanitizeCoordinate(" + key + ") = '0' (empty)");
                return "0";
            }
            String result = pattern.matcher(s).matches() ? s : "0";
            System.out.println("DEBUG: sanitizeCoordinate(" + key + ") = '" + result + "'");
            return result;
        } catch (Exception e) {
            System.err.println("ERROR in sanitizeCoordinate for key '" + key + "': " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Builds locationHierarchy as a hierarchical string
     * Format: "Area > City > Pincode > State > Country"
     */
    private String buildLocationHierarchy(JsonNode source, String dataAreaId) {
        try {
            System.out.println("DEBUG: buildLocationHierarchy called");
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

            String result = hierarchy.toString();
            System.out.println("DEBUG: buildLocationHierarchy result = '" + result + "'");
            return result;
        } catch (Exception e) {
            System.err.println("ERROR in buildLocationHierarchy: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Builds extendedAttributes as a formatted string
     * Format: "CreditLimit: value | DeactiveDate: value | salesHierarchyCode: value | preferredPaymentMode: value"
     */
    private String buildExtendedAttributes(JsonNode source, String paymentMode) {
        try {
            System.out.println("DEBUG: buildExtendedAttributes called");
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

            String result = attributes.length() > 0 ? attributes.toString() : "";
            System.out.println("DEBUG: buildExtendedAttributes result = '" + result + "'");
            return result;
        } catch (Exception e) {
            System.err.println("ERROR in buildExtendedAttributes: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
