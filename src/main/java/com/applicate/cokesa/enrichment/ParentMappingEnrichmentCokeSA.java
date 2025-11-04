package com.applicate.cokesa.enrichment;


import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.etl.OperationResult.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ParentMappingEnrichmentCokeSA extends AbstractEnrichment<User> {



    @Override
    public StepResult apply(User user) {

        try {
            String userDesignation = user.getDesignation().toString();
            if(StringUtils.isEqual(userDesignation, "[salesrep]", true) || StringUtils.isEqual(userDesignation, "[vanseller]", true) || StringUtils.isEqual(userDesignation, "[driver]", true) || StringUtils.isEqual(userDesignation, "[merchandiser]", true)){
                List<String> routeCode = getRouteCodeWithDepot(user.getLoginId());
                if(NullUtils.isNull(routeCode) || routeCode.isEmpty() || routeCode.get(0).isEmpty()){
                    return new StepResult(Status.OK, "No route found for user");
                }
                List<String> supervisor = getSupervisor(routeCode.get(0));
                if(NullUtils.isNull(supervisor) || supervisor.isEmpty() || supervisor.get(0).isEmpty()){
                    return new StepResult(Status.OK, "No supervisor found for user");
                }
                user.setImmediateParent(getImmediateParent(supervisor.get(0)));
            }

            return new StepResult(Status.OK, "Immediate parent successfully set to supervisor");
        }
        catch (Exception e){
            return new StepResult(Status.ERROR, e.getMessage() + "Missing information for parent entity mapping");
        }

    }

    private List<HierarchyMetadata> getImmediateParent(String supervisor){
        List<HierarchyMetadata> hierarchyMetaDataList = new ArrayList<>();
        HierarchyMetadata hierarchyMetaData = new HierarchyMetadata();
        hierarchyMetaData.setImmediateParent(supervisor);
        hierarchyMetaDataList.add(hierarchyMetaData);
        return hierarchyMetaDataList;
    }

    private List<String> getRouteCodeWithDepot(String loginId){
        List<String> routeCode = new ArrayList<>();
        final String query = "SELECT CONCAT(COALESCE(key6,''), COALESCE(key2,'')) AS depot_route FROM ck_generic_object WHERE name = 'OM16_Route' and key3 = '"+ loginId +"';";
        List<Map<String, String>> dataByQuery = (List<Map<String, String>>) EntityUtils.getInstance().findDataByQuery(Map.class, query, true);

        dataByQuery.forEach(currId ->{
            String depotRouteCode = currId.get("depot_route");
            routeCode.add(depotRouteCode);
        });
        return routeCode;
    }
    private List<String> getSupervisor(String route){
        List<String> supervisor = new ArrayList<>();
        final String query = "SELECT loginid FROM ck_user WHERE JSON_CONTAINS(extended_attributes->'$.routeCode', JSON_QUOTE('" +route+"')) = 1;";
        List<Map<String, String>> dataByQuery = (List<Map<String, String>>) EntityUtils.getInstance().findDataByQuery(Map.class, query, true);

        dataByQuery.forEach(currId ->{
            String depotRouteCode = currId.get("loginid");
            supervisor.add(depotRouteCode);
        });
        return supervisor;
    }
}
