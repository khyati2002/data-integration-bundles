package com.applicate.dabur.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.*;

public class OutletTransformerSplit extends AbstractTransformer<Map<String, Object>, OutletDetails> {

    @Override
    public OutletDetails transform(Map<String, Object> input) {

        OutletDetails outletDetails = new OutletDetails();

        // locationHierarchy stored as JSON map
        Map<String, Object> locationHierarchy = new LinkedHashMap<>();
        locationHierarchy.put("country", "IN");
        locationHierarchy.put("town", input.get("town"));
        locationHierarchy.put("zone", input.get("Zone"));
        locationHierarchy.put("stateCode", input.get("StateCode"));
        locationHierarchy.put("state", input.get("StateName"));
        locationHierarchy.put("townCode", input.get("townCode"));
//        outletDetails.setLocationHierarchy(locationHierarchy);

        outletDetails.setAddress((String) input.get("Addr"));
        outletDetails.setGstNo((String) input.get("GSTIINNO"));
        outletDetails.setLatitude((BigDecimal) input.get("Latitude"));
        outletDetails.setLongitude((BigDecimal) input.get("Longitude"));
        outletDetails.setContactno((String) input.get("MobileNo"));
        outletDetails.setOutletCode((String) input.get("RtrCompCode"));
        outletDetails.setOutletName((String) input.get("RetailerName"));
        outletDetails.setActiveStatus((ActiveStatus) input.get("activeStatus"));
        outletDetails.setOutletClass((String) input.get("Classcode"));
        outletDetails.setOutletType((String) input.get("GroupCode"));
        outletDetails.setBeat((String) input.get("RouteCode"));
        outletDetails.setBeatName((String) input.get("RouteName"));
        outletDetails.setChannel((String) input.get("ChannelCode"));

        Map<String, Object> extendedAttributes = new LinkedHashMap<>();
        extendedAttributes.put("supplier", input.get("DistCode"));

        // Split comma-separated SSMCompCode and concat with DistCode
        String distCode = (String) input.get("DistCode");
        String ssmCompCode = (String) input.get("SSMCompCode");

        List<String> ssmList = new ArrayList<>();
        if (ssmCompCode != null && !ssmCompCode.isEmpty()) {
            for (String ssm : ssmCompCode.split(",")) {
                String trimmed = ssm.trim();
                if (!trimmed.isEmpty()) {
                    ssmList.add(trimmed + "-" + distCode);
                }
            }
        }

        extendedAttributes.put("salesRep", String.join(",", ssmList));
        extendedAttributes.put("stateCode", input.get("StateCode"));
        outletDetails.setExtendedAttributes((JsonNode) extendedAttributes);

        HierarchyMetadata hierarchy = new HierarchyMetadata();
        // Immediate Parent hierarchy
        if (distCode != null && !distCode.isEmpty()) {
            String parentHierarchy = distCode;
            hierarchy.setImmediateParent(parentHierarchy);
            outletDetails.setImmediateParent((List)hierarchy);
        }

        return outletDetails;
    }

    @Override
    public EtlType getSourceType() {
        return super.getSourceType();
    }
}
