package com.applicate.lbpl.validation;



import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import org.apache.commons.lang3.StringUtils;
import com.salescode.dim.jooq.impl.GenericEntity;



import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RouteTypeValidation extends AbstractValidationRule<User> {

    GenericEntityService genericEntityService = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);

    private static final String ROUTE_TYPE = "routetype";

    @Override
    public OperationResult.StepResult apply(User user) {

        List<String> errors = new ArrayList<>();

        if (user.getDesignation().contains("mgr")) {

            // Skip validation in case of inactive salesreps
            if(user.getActiveStatus().equals("inactive")){
                return OperationResult.StepResult.OK;
            }

            final Set<String> routeTypeList = getRouteSet(user);

            if (!routeTypeList.isEmpty()) {
                if (!routeTypeList.contains("4")) {
                    errors.add("RouteType 4 not found for User : " + user.getLoginid());
                }
            } else {
                errors.add("No route details found for User :" + user.getLoginid());
            }
        }

        if(!errors.isEmpty()) {
            String errorstr= com.applicate.services.channelkart.utils.StringUtils.format("Some values for User: {} voilating validations. Reason : {}", user.getLoginid(), StringUtils.join(errors, ","));
            return new OperationResult.StepResult(OperationResult.Status.ERROR,errorstr);
        }
        return  OperationResult.StepResult.OK;
    }

    private Set<String> getRouteSet(User user) {
        try {
            return genericEntityService.readModelsByName("RouteDetails").stream()
                    .filter(route -> user.getLoginid().equalsIgnoreCase(route.getLoginId())
                            && route.getKey2().equalsIgnoreCase(user.getImmediateParent().get(0).getParent())
                            && route.getPayload().has(ROUTE_TYPE))
                    .map(route -> route.getPayload().get(ROUTE_TYPE).asText()).collect(Collectors.toSet());
        }catch (Exception e) {
            return Set.of();
        }
    }

}
