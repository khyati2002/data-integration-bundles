package com.applicate.cokesa.task;


import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.taskexecutors.AbstractTaskExecutor;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SupplierOutletCreationTask extends AbstractTaskExecutor {
    private static final String SUPPLIER = "supplier";
    private final JdbcTemplate jdbcTemplate = SpringContext.getBean(JdbcTemplate.class);
    private OutletDetailsService getOutletDetailsService(){
        return SpringContext.getBean(OutletDetailsService.class);
    }

    private MetaDataService getMetaDataService(){return SpringContext.getBean(MetaDataService.class);}
    private UserService getUserService(){
        return SpringContext.getBean(UserService.class);
    }
    public void runTask(Task task) {
        List<String> distinctSupplier = getSuppliersDepot();
        List<User> users = getUserService().findByLoginIdIn(distinctSupplier);
        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getLoginId, Function.identity()));
        distinctSupplier.forEach(supplier -> getOutletDetailsService().save(getOutletDetailsService().refresh(createOutletSupplier(supplier, userMap))));
        setDBVisitInExtended();
    }

    private OutletDetails createOutletSupplier(String supplier, Map<String, User> userMap){
        OutletDetails outlet = new OutletDetails();
        outlet.setOutletCode(supplier);
        outlet.setOutletName("DEPOTNAME_"+ supplier);
        outlet.setContactno("00000000000");
        outlet.setLocationHierarchy(getUserLocation());
        outlet.setActiveStatus(ActiveStatus.ACTIVE);
        outlet.setImmediateParent(new ArrayList<>());
        outlet.setExtendedAttributes(createExtended());
        outlet.setOutletType(SUPPLIER);
        outlet.setUserName(userMap.get(supplier));
        outlet.setMapped(false);
        return outlet;
    }

    private JsonNode createExtended(){
        Map<String, Object> extended = new HashMap<>();
        extended.put("dbVisit", true);
        extended.put("type", "depot");
        return JSONUtils.toJsonNode(extended);
    }

    private Location getUserLocation(){
        Location location = new Location();
        location.setCountry("KSA");
        return location;
    }
    private List<String> getSuppliersDepot(){
        List<String> suppliers = new ArrayList<>();
        final String query = "SELECT DISTINCT supplier from ck_route_info LIMIT 1000;";
        List<Map<String, String>> dataByQuery = (List<Map<String, String>>) EntityUtils.get().findDataByQuery(Map.class, query, true);

        dataByQuery.forEach(currId ->{
            String loginId = currId.get(SUPPLIER);
            suppliers.add(loginId);
        });
        return suppliers;
    }

    private void setDBVisitInExtended(){
        MetaData dbRoutesToDeactivate = getMetaDataService().fetchByValue("RouteInfo", "dbVisitRoutes");
        JsonNode routes = dbRoutesToDeactivate.getDomainValues().get(0).get("routesToBlock");
        for(JsonNode route : routes){
            String supplier = route.asText().substring(0,2);
            String routeOfUser = route.asText().substring(2);
            String updateQuery = "UPDATE ck_route_info SET extended_attributes = JSON_SET(IFNULL(extended_attributes, '{}'),'$.dbVisit', false) WHERE route_code = '" + routeOfUser + "' AND supplier = '"+supplier+"';";
            jdbcTemplate.execute(updateQuery);
        }
    }

    @Override
    public String getTaskType() {
        return "SupplierOutletCreationTask";
    }

}
