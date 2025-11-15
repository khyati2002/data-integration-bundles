package com.applicate.kbpl.validation;

import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.User;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class SalesManRouteValidation extends AbstractValidationRule<User> {

    public static final String ROUTE_TYPE = "routetype";
    GenericEntityService genericEntityService;

    public SalesManRouteValidation() {
        this.genericEntityService = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);
    }

    @Override
    public OperationResult.StepResult apply(User user) {

        List<String> errors = new ArrayList<>();
        if (user.getDesignation().contains("mgr")) {
            final Set<String> routeTypeList = getRouteSet(user);
            if (!routeTypeList.isEmpty()) {
                if ("enrich".equalsIgnoreCase(user.getSource())) {
                    // Enrich users can have either 4 or 11
                    if (!routeTypeList.contains("4") && !routeTypeList.contains("11")) {
                        errors.add("Neither RouteType 4 nor 11 found for User : " + user.getLoginid());
                    }
                } else {
                    // Non-enrich users must have RouteType 4
                    if (!routeTypeList.contains("4")) {
                        errors.add("RouteType 4 not found for User : " + user.getLoginid());
                    }
                }
            } else {
                errors.add("No route details found for User :" + user.getLoginid());
            }
        }

        if (!errors.isEmpty()) {
            String errorString = StringUtils.format("Error saving SalesMan details for User : {}, Reason [{}] ", user.getLoginid(), org.apache.commons.lang3.StringUtils.join(errors, ","));
            return new OperationResult.StepResult(OperationResult.Status.ERROR, errorString);

        }
        return OperationResult.StepResult.OK;
    }

    @NotNull
    private Set<String> getRouteSet(User user) {

        try {
            return genericEntityService.readModelsByName("RouteDetails").stream()
                    .filter(route -> user.getLoginid().equalsIgnoreCase(route.getLoginId())
                            && route.getKey2().equalsIgnoreCase(user.getImmediateParent().get(0).getParent())
                            && route.getPayload().has(ROUTE_TYPE))
                    .map(route -> route.getPayload().get(ROUTE_TYPE).asText()).collect(Collectors.toSet());
        } catch (Exception e) {
            return Set.of();
        }
    }
}

