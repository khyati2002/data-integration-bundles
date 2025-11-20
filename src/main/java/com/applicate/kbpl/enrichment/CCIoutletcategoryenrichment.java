package com.applicate.kbpl.enrichment;

import com.applicate.services.channelkart.models.*;
import com.applicate.services.channelkart.models.enums.RoleName;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.services.LocationService;
import com.applicate.services.channelkart.services.RoleService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.AuthRole;
import com.salescode.dim.jooq.impl.*;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CCIoutletcategoryenrichment extends AbstractEnrichment<OutletDetails> {
    public static final String OUTLETCATEGORY = "Outletcategory";
    public static final String NA = "NA";
    public static final String HIERARCHY_CODE_MAPPING = "HierarchyCodeMapping";
    GenericEntityService genericEntityService = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);
    RoleService roleService = (RoleService) ServiceLocator.lookup(AuthRole.class);

    @Override
    public EnrichmentResult apply(OutletDetails cdm) {
        Map<String, Map<String, String>> outletCategoryMap = genericEntityService.readModelsByName(OUTLETCATEGORY).stream()
                .filter(entity -> StringUtils.isNotEmpty(entity.getKey1()) && StringUtils.isNotEmpty(entity.getKey2()) && StringUtils.isNotEmpty(entity.getKey3()))
                .collect(Collectors.groupingBy(
                        GenericEntity::getKey1,
                        Collectors.toMap(GenericEntity::getKey2, GenericEntity::getKey3)
                ));

        if (NullUtils.isNotNull(cdm.getSource()) && cdm.getSource().equals("ENRICH")) {
            Map<String, String> outleHierarchyMap = genericEntityService.readModelsByName(HIERARCHY_CODE_MAPPING).stream()
                    .collect(Collectors.toMap(GenericEntity::getKey1, GenericEntity::getKey3, (existing, replacement) -> existing));

            if(!ObjectUtils.isEmpty(cdm.getOutletType())) {
                if(cdm.getOutletType().equalsIgnoreCase("7       26")) {
                    Set<String> designations = Set.of("retailer", "supplier");
                    createRetailUser(cdm, designations);
                }
                else if(cdm.getOutletType().equalsIgnoreCase("Wholesale")){
                    createRetailUser(cdm, Set.of("wholesaler"));
                }
                else {
                    createRetailUser(cdm, Set.of("retailer"));
                }
                cdm.setOutletType(enrichField(cdm.getOutletType(), outleHierarchyMap));
            } else {
                cdm.setOutletType(NA);
            }
            cdm.setMarketId(enrichField(cdm.getMarketId(),outletCategoryMap.get("2")));
            cdm.setMarketName(enrichField(cdm.getMarketName(), outletCategoryMap.get("3")));
            cdm.setChannel(enrichField(cdm.getChannel(), outletCategoryMap.get("8")));
            cdm.setSubChannel(enrichField(cdm.getSubChannel(), outletCategoryMap.get("9")));
            cdm.setOutletClass(enrichField(cdm.getOutletClass(), outletCategoryMap.get("3")));
            cdm.setDistributionChannel(enrichField(cdm.getDistributionChannel(), outletCategoryMap.get("8")));

            return new OperationResult.StepResult(OperationResult.Status.OK, "Data Enriched");
        }
        else if(NullUtils.isNotNull(cdm.getSource())) {
            if(!ObjectUtils.isEmpty(cdm.getOutletType())) {
                switch (cdm.getSource()) {
                    case "KGPL":
                        if (cdm.getOutletType().equalsIgnoreCase("Wholesale-KGPL")) {
                            createRetailUser(cdm, Set.of("wholesaler"));
                        }
                        else {
                            createRetailUser(cdm, Set.of("retailer"));
                        }
                        break;
                    case "WAVE":
                        if (cdm.getOutletType().equalsIgnoreCase("Wholesale-WAVE")) {
                            createRetailUser(cdm, Set.of("wholesaler"));
                        }
                        else {
                            createRetailUser(cdm, Set.of("retailer"));
                        }
                        break;
                    default:
                        if (cdm.getOutletType().equalsIgnoreCase("Wholesale")) {
                            createRetailUser(cdm, Set.of("wholesaler"));
                        }
                        else {
                            createRetailUser(cdm, Set.of("retailer"));
                        }
                        break;
                }
            }
            else {
                cdm.setOutletType(NA);
            }
        }

        cdm.setOutletType(enrichField(cdm.getOutletType(), outletCategoryMap.get("1")));

        //cdm.setLocationHierarchy(enrichLocation(locationObj));
        //cdm.setLocation(enrichLocation(cdm.getLocation()));

        LocationService locationService = (LocationService) ServiceLocator.lookup(Location.class);
        Location locationObj = locationService.findByLocationHierarchy(cdm.getLocationHierarchy());
        Location enrichedLocation = enrichLocation(locationObj);
        cdm.setLocation(enrichedLocation);
        String[] locationColumns = locationService.getLocationColumns();
        String hierarchyStr = locationService.formHierarchyUsingColumns(enrichedLocation, locationColumns, " > ");
        cdm.setLocationHierarchy(hierarchyStr);

        cdm.setMarketId(enrichField(cdm.getMarketId(), outletCategoryMap.get("2")));
        cdm.setMarketName(enrichField(cdm.getMarketName(), outletCategoryMap.get("3")));
        cdm.setChannel(enrichField(cdm.getChannel(), outletCategoryMap.get("4")));
        cdm.setSubChannel(enrichField(cdm.getSubChannel(), outletCategoryMap.get("5")));
        cdm.setOutletClass(enrichField(cdm.getOutletClass(), outletCategoryMap.get("7")));
        cdm.setDistributionChannel(enrichField(cdm.getDistributionChannel(), outletCategoryMap.get("8")));

        return new OperationResult.StepResult(OperationResult.Status.OK, "Data Enriched");
    }


    private String enrichField(String fieldValue, Map<String, String> outletMap) {
        if (!ObjectUtils.isEmpty(fieldValue)) {
            if(outletMap.containsKey(fieldValue)) return outletMap.get(fieldValue);
            return fieldValue;
        }
        return NA;
    }

    private Location enrichLocation(Location location) {
        String areacode = location.getAreacode();
        if (!ObjectUtils.isEmpty(areacode)) {
            List<GenericEntity> cc2map = genericEntityService.findByNameAndKey1AndKey2(OUTLETCATEGORY, "2", areacode);
            if (!cc2map.isEmpty()) {
                String code2 = cc2map.get(0).getKey3();
                location.setAreacode(code2);
            }
        }
        return location;
    }

    private void createRetailUser(OutletDetails outlet, Set<String> designations) {
        if (outlet.getUserName() == null) {
            User user = new User();
            user.setActiveStatus(outlet.getActiveStatus());
            user.setLoginId(outlet.getOutletcode());
            user.setUserAccountId(outlet.getOutletcode());
            user.setSource(outlet.getSource());
            user.setLocationHierarchy(outlet.getLocationHierarchy());
            user.setMobile(outlet.getContactno());
            user.setName(org.apache.commons.lang3.StringUtils.isEmpty(outlet.getOutletName()) ? outlet.getOutletcode() : outlet.getOutletName());
            user.setImmediateParent(replicateRetailerOutletParent(outlet.getImmediateParent()));
            user.setDesignation(designations);
            List<AuthRole> roles = roleService.getRoleAsList(RoleName.ROLE_USER.name());
            user.setRoles(roles);
            outlet.setUserName(user);
            outlet.setImmediateParent(new ArrayList<>(1));
        } else {
            User user = outlet.getUserName();
            user.setDesignation(designations);
            user.setSource(outlet.getSource());
            outlet.setUserName(user);
        }
    }

    private List<HierarchyMetadata> replicateRetailerOutletParent(List<HierarchyMetadata> hms) {
        if (NullUtils.isNull(hms)) {
            return new ArrayList<>(1);
        }
        List<HierarchyMetadata> parentList = new ArrayList<>(hms.size());
        hms.forEach(hm -> parentList.add(EntityUtils.deepClone(hm)));
        return parentList;
    }
}
