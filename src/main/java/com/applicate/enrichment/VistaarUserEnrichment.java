package com.applicate.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;

import com.applicate.services.channelkart.exceptions.EnrichmentFailException;
import com.applicate.services.channelkart.models.HierarchyMetaData;
import com.applicate.services.channelkart.models.Location;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.jooq.impl.SupplierMetaData;
// import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.applicate.services.channelkart.models.SupplierMetaData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public class VistaarUserEnrichment extends AbstractEnrichment<User> {
    private final UserService userService = (UserService) ServiceLocator.lookup(UserService.class);
    @Override
    public OperationResult.StepResult apply(User user) {
        List<String> enrichmentResult = new ArrayList<>();
        enrichmentResult.add(enrichLocationInformation(user));
        enrichmentResult.add(enrichSupplierMetadata(user));
        enrichmentResult.add(enrichAUSStockistUser(user));
        enrichmentResult.add(enrichActiveStatus(user));
        return new OperationResult.StepResult(OperationResult.Status.OK, String.join(",",enrichmentResult));
    }

    public String enrichAUSStockistUser(User user){
        String enrichmentMsg="AUS enrichment skipped";

        if(user.getDesignation().contains("stockist")){
            JsonNode curExtendedAttributes=user.getExtendedAttributes();
            User userInDb=userService.findByLoginId(user.getLoginId());

            if(curExtendedAttributes!=null && curExtendedAttributes.has("AUS")){
                JsonNode curAUS=curExtendedAttributes.get("AUS");
                ObjectNode newExtendedAttributes=(ObjectNode) curExtendedAttributes;

                if(curAUS.isNull() || StringUtils.isNullOrBlank(curAUS.asText())){
                    if(userInDb!=null){
                        JsonNode ausInDb=userInDb.getExtendedAttributes().get("AUS");
                        newExtendedAttributes.set("AUS",ausInDb);
                    }else{
                        newExtendedAttributes.put("AUS","N");
                    }
                    user.setExtendedAttributes(newExtendedAttributes);
                    enrichmentMsg="AUS enriched successfully";
                }

            }
        }
        return enrichmentMsg;
    }

    private String enrichLocationInformation(User user) {
        String enrichmentMsg="location enrichment skipped";
        Location location = user.getLocationHierarchy();
        User immediateParent = null;
        if(user.getImmediateParent() != null && user.getImmediateParent().size() > 0) {
            HierarchyMetaData hmimmeParent = user.getImmediateParent().get(0);
            if(hmimmeParent != null && hmimmeParent.getImmediateParent() != null){
                immediateParent = userService.findByLoginId(hmimmeParent.getImmediateParent());
            }
        }
        if(location == null) {
            location = new Location();
            user.setLocationHierarchy(location);
        }
        if(StringUtils.isNotBlank(location.getCountry())) {
            location.setCountry(location.getCountry().trim().toUpperCase());
        }
        else {
            location.setCountry("INDIA");
            enrichmentMsg="country enriched successfully";
        }

        if(immediateParent != null && immediateParent.getLocationHierarchy() != null) {
            if(StringUtils.isNullOrBlank(location.getBranch())) {
                location.setBranch(immediateParent.getLocationHierarchy().getBranch());
            }
            if(StringUtils.isNullOrBlank(location.getState())) {
                location.setState(immediateParent.getLocationHierarchy().getState());
            }
            if(StringUtils.isNullOrBlank(location.getCity())) {
                location.setCity(immediateParent.getLocationHierarchy().getCity());
            }
        }
        return enrichmentMsg;
    }

    private String enrichSupplierMetadata(User user) {
        boolean isSupplierEnriched= false;
        if(user.getDesignation() != null && user.getDesignation().contains("wd")) {
            List<SupplierMetaData> supplierMetaDataList = user.getSupplierMetaData();
            if(supplierMetaDataList == null || supplierMetaDataList.isEmpty()) {
                SupplierMetaData supplierMetaData = new SupplierMetaData();
                if(supplierMetaDataList == null) {
                    supplierMetaDataList = new ArrayList<>();
                    user.setSupplierMetaData(supplierMetaDataList);
                }
                supplierMetaDataList.add(supplierMetaData);
            }
            for(SupplierMetaData supplierMetaData : supplierMetaDataList) {
                if(supplierMetaData.getType() == null || supplierMetaData.getType().isBlank()) {
                    supplierMetaData.setType("amount");
                    isSupplierEnriched = true;
                }
                if(supplierMetaData.getMin() == null) {
                    supplierMetaData.setMin(0);
                    isSupplierEnriched = true;
                }
                if(supplierMetaData.getMax() == null) {
                    supplierMetaData.setMax(1000000);
                    isSupplierEnriched = true;
                }
            }
        }

        if(isSupplierEnriched) {
            return "Supplier Metadata enriched for WD";
        }
        return "Supplier Metadata entichment skipped";
    }

    private String enrichActiveStatus(User user) {
        if(user.getDesignation() != null) {
            if(user.getActiveStatus() == null) {
                if (user.getDesignation().contains("wd") || user.getDesignation().contains("branch") || user.getDesignation().contains("district") ) {
                    user.setActiveStatus(ActiveStatus.ACTIVE);
                    user.setActiveStatusReason(ActiveStatus.ACTIVE.getStatus());
                    return "active status enriched";
                }
                else if ((user.getDesignation().contains("psr")) || (user.getDesignation().contains("stockist"))) {
                    JsonNode extendedAttributes = user.getExtendedAttributes();
                    if (extendedAttributes != null && extendedAttributes.hasNonNull("AUS")) {
                        String AUS = extendedAttributes.get("AUS").asText();
                        if (AUS.equalsIgnoreCase("Y")) {
                            user.setActiveStatus(ActiveStatus.ACTIVE);
                            user.setActiveStatusReason("active status activated as per AUS field");
                            return "active status enriched as per AUS field";
                        } else if (AUS.equalsIgnoreCase("N")) {
                            user.setActiveStatus(ActiveStatus.INACTIVE);
                            user.setActiveStatusReason("active status inactive as per AUS field");
                            return "active status enriched as per AUS field";
                        } else {
                            throw new EnrichmentFailException("AUS field should be Y or N for PSR and stockist");
                        }
                    }
                }
                else {
                    user.setActiveStatus(ActiveStatus.INACTIVE);
                    user.setActiveStatusReason("Unknown designation");
                }
            }
        }
        return "active status not enriched";
    }

}
