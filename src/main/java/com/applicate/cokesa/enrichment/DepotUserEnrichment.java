package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

public class DepotUserEnrichment extends AbstractEnrichment<OutletDetails> {

    @Override
    public OperationResult.StepResult apply(OutletDetails outlet) {

        UserService userService=(UserService) ServiceLocator.lookup(User.class);
        User user = userService.findByLoginId(outlet.getLoginid());
        if(user!=null) {
            outlet.setUserName(user);
            user.setDesignation(Set.of("Depot"));
            user.setUserParents(List.of("admin@applicate.in"));
            user.setLoginId(outlet.getOutletcode());
            user.setUserAccountId(outlet.getOutletcode());
            user.setActiveStatus(outlet.getActiveStatus());
            user.setLocationHierarchy(outlet.getLocationHierarchy());
            user.setLocationHierarchy(outlet.getLocation());
            user.setMobile(outlet.getContactno());
            user.setAddress(outlet.getAddress());
            user.setName(StringUtils.isEmpty(outlet.getOutletName()) ? outlet.getOutletcode() : outlet.getOutletName());
            outlet.setUserName(user);
            userService.save(user);
        }
        return new OperationResult.StepResult(OperationResult.Status.OK);
    }
}
