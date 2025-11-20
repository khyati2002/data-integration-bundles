package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class DepotUserEnrichment extends AbstractEnrichment<OutletDetails> {

    @Override
    public OperationResult.StepResult apply(OutletDetails outlet) {

        String authToken = SecurityContextUtils.getPrincipal();

        if ("integration_user".equals(authToken)) {

            UserService userService = (UserService) ServiceLocator.lookup(User.class);

            User user = new User();

            if (outlet.getUserName() == null) {
                user = userService.findByLoginId(outlet.getLoginid());

                if (user == null) {
                    user = new User();
                    user.setLoginId(outlet.getOutletcode());
                    user.setUserAccountId(outlet.getOutletcode());
                }

                outlet.setUserName(user);

            } else {
                user = outlet.getUserName();
            }

            user.setDesignation(Set.of("Depot"));
            user.setUserParents(new ArrayList<>(Arrays.asList("admin@applicate.in")));
            user.setActiveStatus(outlet.getActiveStatus());
            user.setLocationHierarchy(outlet.getLocationHierarchy());
            user.setLocationHierarchy(outlet.getLocation());
            user.setMobile(outlet.getContactno());
            user.setAddress(outlet.getAddress());
            user.setName(
                    StringUtils.isEmpty(outlet.getOutletName())
                            ? outlet.getOutletcode()
                            : outlet.getOutletName()
            );

            userService.save(user);
        }

        return new OperationResult.StepResult(OperationResult.Status.OK);
    }
}
