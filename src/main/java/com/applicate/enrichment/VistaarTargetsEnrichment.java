package com.applicate.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.TargetResults;
import com.salescode.dim.jooq.impl.Targets;
import com.salescode.dim.jooq.impl.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class VistaarTargetsEnrichment extends AbstractEnrichment<Targets> {

    private static final Logger logger = LoggerFactory.getLogger(VistaarTargetsEnrichment.class);

    @Override
    public OperationResult.StepResult apply(Targets cdm) {

        final UserService userService = (UserService) ServiceLocator.lookup(User.class);

        List<TargetResults> results = cdm.getTargetResults();
        if(results == null || results.isEmpty()){
            logger.error("Unable to Enrich Data! Target Result Object null or empty");
            return new OperationResult.StepResult(OperationResult.Status.WARNING, "Target Result Object null or empty");
        }

        if (cdm.getExtendedAttributes() == null || !cdm.getExtendedAttributes().hasNonNull("loginId")) {
            logger.error("Unable to Enrich Data! loginId not found in Extended Attributes");
            return new OperationResult.StepResult(OperationResult.Status.WARNING, "loginId not found in Extended Attributes");
        }

        String loginId = cdm.getExtendedAttributes().get("loginId").asText();
        if(StringUtils.isEmpty(loginId)){
            logger.error("Unable to Enrich Data! Extended Attributes loginId is empty");
            return new OperationResult.StepResult(OperationResult.Status.WARNING, "Extended Attributes loginId is empty");
        }

        User userDetails = userService.findByLoginId(loginId);
        if(userDetails == null){
            logger.error("Unable to Enrich Data! LoginId provided not present in db: {}", loginId);
            return new OperationResult.StepResult(OperationResult.Status.WARNING, "LoginId provided not present in db");
        }

        TargetResults targetResult = results.get(0);

        String locationHierarchy = null;
        if (userDetails.getLocation() != null) {
            locationHierarchy = userDetails.getLocation().getLocationHierarchy();
        }

        if(StringUtils.isEmpty(userDetails.getHierarchy()) || StringUtils.isEmpty(locationHierarchy)){
            logger.error("Unable to Enrich Data! Either Hierarchy or LocationHierarchy of the user present in db. User: {}", loginId);
            return new OperationResult.StepResult(OperationResult.Status.WARNING, "User Hierarchy or LocationHierarchy is missing");
        }

        targetResult.setHierarchy(userDetails.getHierarchy());
        targetResult.setLocationHierarchy(locationHierarchy);
        targetResult.setLoginId(loginId);

        return OperationResult.StepResult.OK;
    }
}