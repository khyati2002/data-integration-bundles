package com.applicate.validation;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

public class VistaarOutletValidation extends AbstractValidationRule<OutletDetails> {
    public static final String STOCKIST = "stockist";
    final OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
    static final String CAPITAL_CASE_REGEX = "(^[A-Z\\s]*$)";
    static final String CAPITAL_CASE_BRANCH_REGEX = "(^[A-Z]*$)";

    @Override
    public OperationResult.StepResult apply(OutletDetails outletDetails) {
        List<String> ruleResult = new ArrayList<>();
        RegexValidation regexValidation = new RegexValidation();
        boolean isAlpha = isAlphaOutlet(outletDetails);

        basicOutletChecks(outletDetails, ruleResult);
        validateLocationHierarchy(outletDetails, regexValidation, ruleResult);
        validateUserFields(outletDetails, isAlpha, ruleResult);

        if (outletDetails.getImmediateParent() == null || outletDetails.getImmediateParent().isEmpty()) {
            ruleResult.add("immediate parent can not be null or empty");
        } else {
            validateParentAndRelations(outletDetails, ruleResult);
        }

        if (isAlpha) validateSecondaryOutletVerifiedStatus(outletDetails, ruleResult);

        return ruleResult.isEmpty()
                ? OperationResult.StepResult.OK
                : new OperationResult.StepResult(OperationResult.Status.ERROR, String.join(",", ruleResult));
    }

    private void basicOutletChecks(OutletDetails outletDetails, List<String> ruleResult) {
        if (outletDetails.getOutletcode() == null)
            ruleResult.add("outletcode not present in the database");

        if ((outletDetails.getActiveStatus() != ActiveStatus.ACTIVE)
                && (outletDetails.getActiveStatus() != ActiveStatus.INACTIVE))
            ruleResult.add("Invalid status for active field");

        if (outletDetails.getImmediateParent() == null || outletDetails.getImmediateParent().isEmpty())
            ruleResult.add("UID can not be null or empty");

        if (outletDetails.getLocationHierarchyModel() == null)
            ruleResult.add("location hierarchy cannot be null");
    }

    private void validateLocationHierarchy(OutletDetails outletDetails, RegexValidation regexValidation, List<String> ruleResult) {
        if (outletDetails.getLocationHierarchy() == null) return;

        if (outletDetails.getLocationHierarchyModel().getBranch() != null) {
            if (!StringUtils.isNotBlank(outletDetails.getLocation().getBranch())
                    || !regexValidation.match(CAPITAL_CASE_BRANCH_REGEX, outletDetails.getLocationHierarchyModel().getBranch())) {
                ruleResult.add("Value given for branch should contain only alphabets with capital case.Current given value is not compatible");
            }
        } else ruleResult.add("Branch can not be null");

        if (outletDetails.getLocationHierarchyModel().getDistrict() != null) {
            if (!StringUtils.isNotBlank(outletDetails.getLocation().getDistrict())
                    || !regexValidation.match(CAPITAL_CASE_REGEX, outletDetails.getLocationHierarchyModel().getDistrict())) {
                ruleResult.add("Value given for District should contain only alphabets with capital case.Current given value is not compatible");
            }
        } else ruleResult.add("District can not be null");
    }

    private void validateUserFields(OutletDetails outletDetails, boolean isAlpha, List<String> ruleResult) {
        if (outletDetails.getChannel().equals(STOCKIST) && outletDetails.getOutletType().equals(STOCKIST)
                && outletDetails.getUserName() == null)
            ruleResult.add("Loginid for the outlet cannot be null or empty");

        if (StringUtils.isNullOrBlank(outletDetails.getContactName()))
            ruleResult.add("contactName cannot be blank or null");

        if (isAlpha && StringUtils.isNullOrBlank(outletDetails.getContactno())
                && !outletDetails.getOutletType().equalsIgnoreCase("Stockist")
                && outletDetails.getActiveStatus() == ActiveStatus.ACTIVE)
            ruleResult.add("contactno cannot be blank or null");

        if (StringUtils.isNullOrBlank(outletDetails.getOutletName()))
            ruleResult.add("outletName cannot be blank or null");

        if (StringUtils.isNullOrBlank(outletDetails.getOutletType()))
            ruleResult.add("outletType cannot be blank or null and should only contain alphabets");

        if (StringUtils.isNullOrBlank(outletDetails.getChannel()))
            ruleResult.add("channel cannot be blank or null");
    }

    private void validateParentAndRelations(OutletDetails outletDetails, List<String> ruleResult) {
        UserService userService = (UserService) ServiceLocator.lookup(User.class);
        String parentId = outletDetails.getImmediateParent().get(0).getImmediateParent();
        if (parentId == null) {
            ruleResult.add("immediate parent can not be null.");
            return;
        }

        User dbparent = userService.findByLoginId(parentId);
        validateUIDInDB(outletDetails, dbparent, ruleResult);
        validateNonStockistOutletParent(outletDetails, dbparent, ruleResult);
        validateOutletLocation(outletDetails, dbparent, ruleResult);
        validateSupplierMappingForStockist(outletDetails, ruleResult);
    }

    // === Below: Existing helper methods unchanged ===

    private void validateSecondaryOutletVerifiedStatus(OutletDetails outletDetails, List<String> ruleResult) {
        if (outletDetails.getOutletType().equalsIgnoreCase(STOCKIST)) return;
        if (outletDetails.getActiveStatus() == ActiveStatus.ACTIVE) {
            JsonNode extendedAttributes = outletDetails.getExtendedAttributes();
            boolean isVerified = false;
            if (extendedAttributes != null && extendedAttributes.has("isVerified")) {
                JsonNode verifiedNode = extendedAttributes.get("isVerified");
                if (verifiedNode.isBoolean()) isVerified = verifiedNode.asBoolean();
                else if (verifiedNode.isTextual()) isVerified = Boolean.parseBoolean(verifiedNode.asText().trim());
            }
            if ((outletDetails.getSource() == null || !outletDetails.getSource().equalsIgnoreCase("MDM UPLAOD")) && !isVerified)
                ruleResult.add("Active outlets should be verified!");
        }
    }

    private void validateNonStockistOutletParent(OutletDetails outletDetails, User dbParent, List<String> ruleResult) {
        if (!outletDetails.getOutletType().equalsIgnoreCase(STOCKIST)
                && dbParent != null && !dbParent.getDesignation().contains(STOCKIST))
            ruleResult.add("Outlet has invalid immediate parent, parent of non stockist outlet should be a stockist");
    }

    private void validateOutletLocation(OutletDetails outletDetails, User dbparent, List<String> ruleResult) {
        UserService userService = (UserService) ServiceLocator.lookup(User.class);
        Location outletLocation = outletDetails.getLocationHierarchyModel();
        if (outletDetails.getOutletType().equalsIgnoreCase(STOCKIST)) {
            String psrCrmId = outletDetails.getExtendedAttributes().get("PSRCRMID").asText();
            if (!psrCrmId.equalsIgnoreCase("NA")) {
                User psrUser = userService.findByLoginId(psrCrmId);
                matchBranchAndDistrict(outletLocation, psrUser.getLocation(), ruleResult);
            } else {
                ArrayNode supplierList = (ArrayNode) outletDetails.getExtendedAttributes().get("supplier");
                for (JsonNode supplier : supplierList) {
                    User supplierUser = userService.findByLoginId(supplier.asText());
                    matchBranchAndDistrict(outletLocation, supplierUser.getLocation(), ruleResult);
                }
            }
        } else if (dbparent != null) {
            matchBranchAndDistrict(outletLocation, dbparent.getLocation(), ruleResult);
        }
    }

    private void matchBranchAndDistrict(Location outletLocation, Location location, List<String> ruleResult) {
        if (location != null) {
            if (location.getBranch() != null && !location.getBranch().equals(outletLocation.getBranch()))
                ruleResult.add("Branch value is not matching with parent's branch: " + location.getBranch() + ". Please verify the input data.");
            if (location.getDistrict() != null && !location.getDistrict().equals(outletLocation.getDistrict()))
                ruleResult.add("District value is not matching with parent's district: " + location.getDistrict() + " .Please verify the input data.");
        }
    }

    private void validateSupplierMappingForStockist(OutletDetails outletDetails, List<String> ruleResult) {
        if (outletDetails.getOutletType().equalsIgnoreCase(STOCKIST)) {
            JsonNode extendedAttributes = outletDetails.getExtendedAttributes();
            if (extendedAttributes != null && !extendedAttributes.isEmpty()) {
                ArrayNode supplierMapping = (ArrayNode) extendedAttributes.get("supplierMapping");
                if (supplierMapping == null || supplierMapping.isEmpty()) return;

                for (JsonNode supplier : supplierMapping) {
                    validateField(supplier, "CustID", outletDetails.getOutletcode(), ruleResult);
                    validateField(supplier, "SIFYID", outletDetails.getOutletcode(), ruleResult);
                    validateField(supplier, "WDDest", outletDetails.getOutletcode(), ruleResult);
                    validateField(supplier, "RCSID", outletDetails.getOutletcode(), ruleResult);
                    validateField(supplier, "WDName", outletDetails.getOutletcode(), ruleResult);
                }
            } else ruleResult.add("Extended attributes missing for stockist: " + outletDetails.getOutletcode());
        }
    }

    private void validateField(JsonNode supplier, String fieldName, String outletCode, List<String> ruleResult) {
        if (!supplier.has(fieldName) || supplier.get(fieldName).asText().trim().isEmpty())
            ruleResult.add("Missing or empty " + fieldName + " in supplier mapping for stockist: " + outletCode);
    }

    private void validateUIDInDB(OutletDetails outletDetails, User dbParent, List<String> ruleResult) {
        boolean isNonStockist = !outletDetails.getChannel().equalsIgnoreCase(STOCKIST);
        if (isNonStockist && dbParent == null)
            ruleResult.add("The UID is invalid");
    }

    private boolean isAlphaOutlet(OutletDetails outlet) {
        if (outlet == null) return false;
        try {
            JsonNode extendedAttributes = outlet.getExtendedAttributes();
            if (extendedAttributes != null && extendedAttributes.has("isAlpha")) {
                JsonNode alphaNode = extendedAttributes.get("isAlpha");
                if (alphaNode.isBoolean()) return alphaNode.asBoolean();
                if (alphaNode.isTextual()) return "true".equals(alphaNode.asText());
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }
}
