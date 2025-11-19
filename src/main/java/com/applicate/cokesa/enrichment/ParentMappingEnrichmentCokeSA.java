package com.applicate.cokesa.enrichment;


import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;

import java.util.*;


public class ParentMappingEnrichmentCokeSA extends AbstractEnrichment<User> {

    private GenericEntityService genericEntityService = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);

    @Override
    public OperationResult.StepResult apply(User user) {
        try {
            String userDesignation = user.getDesignation().toString();
            if(StringUtils.isEqual(userDesignation, "[salesrep]", true) || StringUtils.isEqual(userDesignation, "[vanseller]", true) || StringUtils.isEqual(userDesignation, "[driver]", true) || StringUtils.isEqual(userDesignation, "[merchandiser]", true)){
                List<String> routeCode = getRouteCodeWithDepot(user.getLoginid());
                if(NullUtils.isNull(routeCode) || routeCode.isEmpty() || routeCode.get(0).isEmpty()){
                    return new OperationResult.StepResult(OperationResult.Status.OK, "No route found for user");
                }
                List<String> supervisor = getSupervisor(routeCode.get(0));
                if(NullUtils.isNull(supervisor) || supervisor.isEmpty() || supervisor.get(0).isEmpty()){
                    return new OperationResult.StepResult(OperationResult.Status.OK, "No supervisor found for user");
                }
                user.setUserParents(List.of(supervisor.get(0)));
                return new OperationResult.StepResult(OperationResult.Status.OK, "Immediate parent successfully set to supervisor");
            }
            if(StringUtils.isEqual(userDesignation, "[depot]", true)){
                List<GenericEntity> allRfcUsers = genericEntityService.readModelsByName("rfc_user");
                Map<String, String> mapOfDepotToRfc = generateSupplierToRfcMap(allRfcUsers);
                user.setUserParents(List.of(mapOfDepotToRfc.getOrDefault(user.getLoginId(), "admin@applicate.in")));
                return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");
            }
            if (StringUtils.isEqual(userDesignation, "[rfc]", true)) {
                if (NullUtils.isNull(user.getLoginId()) || user.getLoginId().isEmpty()) {
                    return new OperationResult.StepResult(OperationResult.Status.OK,"Login ID missing for RFC user");
                }
                GenericEntity ge = createGenericEntity(user, user.getEmail() + "-rfc_user", "rfc_user");
                genericEntityService.batchSave(Collections.singletonList(ge));
                return new OperationResult.StepResult(OperationResult.Status.OK, "RFC User saved successfully");

            }
            return new OperationResult.StepResult(OperationResult.Status.OK,"Data Enriched Successfully");

        }
        catch (Exception e){
            return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage() + "Missing information for parent entity mapping");
        }

    }

    private Map<String, String> generateSupplierToRfcMap(List<GenericEntity> rfcUsers) {
        Map<String, String> supplierToRfcMap = new HashMap<>();
        if (NullUtils.isNull(rfcUsers)) {
            return supplierToRfcMap;
        }
        for(GenericEntity entity : rfcUsers) {
            if (NullUtils.isNull(entity)) {
                continue;
            }
            String key3 = entity.getKey3();
            String key1 = entity.getKey1();
            if (NullUtils.isNull(key3) || NullUtils.isNull(key1) || key3.isEmpty()) {
                continue;
            }
            String[] codes = key3.split(",");
            for (String code : codes) {
                String trimmedCode = code.trim();
                if (!trimmedCode.isEmpty()) {
                    supplierToRfcMap.put(trimmedCode, key1);
                }
            }
        }
        return supplierToRfcMap;
    }

    private GenericEntity createGenericEntity(User user, String id, String name)  {
        GenericEntity ge = new GenericEntity();
        ge.setId(id);
        ge.setName(name);
        String depotLocation = user.getExtendedAttributes().get("depotLocation").asText();
        String rfcCode = user.getExtendedAttributes().get("rfcCode").asText();
        ge.setKey3(depotLocation);
        ge.setKey2(rfcCode);
        ge.setKey1(user.getLoginId());
        return ge;
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
        List<Map<String, String>> dataByQuery = (List<Map<String, String>>) EntityUtils.getInstance().fetchDataByQuery(Map.class, query);

        dataByQuery.forEach(currId ->{
            String depotRouteCode = currId.get("depot_route");
            routeCode.add(depotRouteCode);
        });
        return routeCode;
    }
    private List<String> getSupervisor(String route){
        List<String> supervisor = new ArrayList<>();
        final String query = "SELECT loginid FROM ck_user WHERE JSON_CONTAINS(extended_attributes->'$.routeCode', JSON_QUOTE('" +route+"')) = 1;";
        List<Map<String, String>> dataByQuery = (List<Map<String, String>>) EntityUtils.getInstance().fetchDataByQuery(Map.class, query);

        dataByQuery.forEach(currId ->{
            String depotRouteCode = currId.get("loginid");
            supervisor.add(depotRouteCode);
        });
        return supervisor;
    }
}
