package com.applicate.hccb.validation;

import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;

import java.util.*;
import java.util.regex.Pattern;

@SuppressWarnings("all")
public class OutletValidatorITCL extends AbstractValidationRule<OutletDetails> {
    final String alphabetRegex = "(^[(A-Z a-z)]*$)";
    final String outletCodeRegex = "(^[(A-Za-z-0-9_ )]*$)";
    final String MobileNumberRegex = "(^[0-9]{10}$)";
    final String capitalCaseRegex = "(^[A-Z]*$)";
    String regexY_N = "^(Y|N)$";

    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {

        PropertyRegistry propertyRegistry = PropertyRegistry.getInstance();

        try {

            List<String> duplicateWDCheck = new ArrayList<String>();
            Set<String> ruleResult = new HashSet<>();

            if (cdm.getOutletcode() != null) {
                if (!Pattern.matches(outletCodeRegex, cdm.getOutletcode())) {
                    ruleResult.add("Given outletCode did not match the required validations.");
                }
            } else {
                ruleResult.add("OutletCode can not be null.");
            }

            if (cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().has("duplicateOutlet")) {
                if ((cdm.getExtendedAttributes().get("duplicateOutlet")) != null) {
                    if (cdm.getExtendedAttributes().get("duplicateOutlet").asText().equals("yes")) {
                        ruleResult.add("Outletcode can not be duplicate.");
                    }
                }
            }

            if (!propertyRegistry.getAsBoolean(PropertyDefinition.USE_SUPPLIER_FROM_OUTLET_METADATA)) {
                if (cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().has("giftVoucher")) {
                    if (!Pattern.matches(regexY_N, cdm.getExtendedAttributes().get("giftVoucher").asText())) {
                        ruleResult.add("Value given for giftVoucher can only be Y or N");
                    }
                } else {
                    ruleResult.add("Value given for giftVoucher can not be null");
                }

                if (cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().has("ITCProducts")) {
                    if (!Pattern.matches(regexY_N, cdm.getExtendedAttributes().get("ITCProducts").asText())) {
                        ruleResult.add("Value given for ITCProducts can only be Y or N");
                    }
                } else {
                    ruleResult.add("Value given for ITCProducts can not be null");
                }

                if (cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().has("autoRedemption")) {
                    if (!Pattern.matches(regexY_N, cdm.getExtendedAttributes().get("autoRedemption").asText())) {
                        ruleResult.add("Value given for autoRedemption can only be Y or N");
                    } else if (cdm.getOutletCategory() != null && "non loyalty".equals(cdm.getOutletCategory()) && "N".equals(cdm.getExtendedAttributes().get("autoRedemption").asText())) {
                        ruleResult.add("Value of autoRedemption for a non loyalty outlet can only be Y");
                    }
                } else {
                    ruleResult.add("Value given for autoRedemption can not be null");
                }

                if (cdm.getOutletCategory() != null) {
                    String outletCategory = cdm.getOutletCategory();
                    if (!(outletCategory.equals("loyalty") || outletCategory.equals("non loyalty"))) {
                        ruleResult.add("The value for loyalty flag should be either Loyalty or Non Loyalty.");
                    }
                } else {
                    ruleResult.add("The value for loyalty flag can not be null");
                }
            }

            if (StringUtils.hasNullOrEmptyValues(cdm.getOutletCategory())) {
                ruleResult.add("The value for loyalty type can not be null");
            } else {
                if (cdm.getOutletCategory().equals("loyalty")) {
                    String outletClass = cdm.getOutletClass();
                    if (outletClass == null) {
                        ruleResult.add("The value for loyalty type can not be null");
                    }
                }

                if (cdm.getOutletCategory().equals("non loyalty")) {
                    String outletClass = cdm.getOutletClass();
                    if (outletClass == null) {
                        ruleResult.add("The value for loyalty type can not be null");
                    }

                    if ((outletClass.equals("FC FOODS") || outletClass.equals("FC COMMON") || outletClass.equals("FC PCP"))) {
                        ruleResult.add("The value of loyalty type for non loyalty type outlet cannot be FC FOODS , FC COMMON, or FC PCP.");
                    }

                }

            }

            Location location = cdm.getLocation();
            if (location != null) {
                if (location.getBranch() != null) {
                    if (!Pattern.matches(capitalCaseRegex, location.getBranch())) {
                        ruleResult.add("Value given for branch should contain only alphabets with capital case.Current given value is not compatible");
                    }
                } else {
                    ruleResult.add("Branch can not be null");
                }

                if (location.getDistrict() != null) {
                    if (!Pattern.matches(capitalCaseRegex, location.getDistrict())) {
                        ruleResult.add("Value given for district should contain only alphabets with capital case.Current given value is not compatible");
                    }
                } else {
                    ruleResult.add("District can not be null");
                }
            } else {
                ruleResult.add("Location details can not be null");
            }

            if (cdm.getLocation().getDistrict() != null) {
                if (!Pattern.matches(capitalCaseRegex, cdm.getLocation().getDistrict())) {
                    ruleResult.add("Value given for district should contain only alphabets with capital case.Current given value is not compatible");
                }
            } else {
                ruleResult.add("District can not be null");
            }
            if (!propertyRegistry.getAsBoolean(PropertyDefinition.USE_SUPPLIER_FROM_OUTLET_METADATA)) {

                if (cdm.getExtendedAttributes() != null && cdm.getExtendedAttributes().has("supplierMapping")) {
                    if (NullUtils.isNotNull(cdm.getExtendedAttributes().get("supplierMapping"))) {
                        List<Map<String, String>> supplierList = JSONUtils.convert(cdm.getExtendedAttributes().get("supplierMapping"), new TypeReference<List<Map<String, String>>>() {
                        });

                        supplierList.forEach(supplierMapping -> {
                            if (supplierMapping.containsKey("WDDest")) {
                                if (NullUtils.isNull(supplierMapping.get("WDDest"))) {
                                    ruleResult.add("WDDest can not be null");
                                } else if (duplicateWDCheck.contains(supplierMapping.get("WDDest"))) {
                                    ruleResult.add("WDDest can not be duplicate");
                                }
                                duplicateWDCheck.add(supplierMapping.get("WDDest"));
                            } else {
                                ruleResult.add("WDDest can not be null");
                            }

                            if (supplierMapping.containsKey("CustID")) {
                                if (NullUtils.isNull(supplierMapping.get("CustID"))) {
                                    ruleResult.add("custID can not be null");
                                }
                            } else {
                                ruleResult.add("custID can not be null");
                            }

                            if (supplierMapping.containsKey("SIFYID")) {
                                if (NullUtils.isNull(supplierMapping.get("SIFYID"))) {
                                    ruleResult.add("SIFYID can not be null");
                                }
                            } else {
                                ruleResult.add("SIFYID can not be null");
                            }

                            if (supplierMapping.containsKey("UID")) {
                                if (NullUtils.isNull(supplierMapping.get("UID"))) {
                                    ruleResult.add("UID can not be null");
                                }
                            } else {
                                ruleResult.add("UID can not be null");
                            }

                            if (supplierMapping.containsKey("RCSId")) {
                                if (NullUtils.isNull(supplierMapping.get("RCSId"))) {
                                    ruleResult.add("RCSId can not be null");
                                }
                            } else {
                                ruleResult.add("RCSId can not be null");
                            }
                        });
                    } else {
                        ruleResult.add("Value given for Supplier Mapping can not be null");
                    }
                } else {
                    ruleResult.add("Value given for Supplier Mapping can not be null");
                }
            }

            if (ruleResult.size() > 0) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, org.apache.commons.lang3.StringUtils.join(ruleResult, ", "));
            } else {
                return OperationResult.StepResult.OK;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "unexpected server error ");
        }

    }

}