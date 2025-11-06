package com.applicate.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.exceptions.EnrichmentFailException;
import com.applicate.services.channelkart.services.QueryService;

import java.util.List;
import java.util.Map;

public class VistaarWDBranchPrvLocEnrichment extends AbstractEnrichment<User> {

    private final UserService userService = (UserService) ServiceLocator.lookup(User.class);
    private final QueryService queryService = new QueryService();
    private static final String BRANCH = "branch";
    private static final String DISTRICT = "district";
    private static final String QUERY = "select loginid from ck_user ";

    @Override
    public OperationResult.StepResult apply(User user) {

        if(user.getDesignation() != null && (user.getDesignation().contains(DISTRICT) || user.getDesignation().contains(BRANCH))) {
            String prvLoc = "";
            if (user.getExtendedAttributes() != null && user.getExtendedAttributes().hasNonNull("prvLoc")) {
                prvLoc = user.getExtendedAttributes().get("prvLoc").textValue();
            }
            if(prvLoc == null || !prvLoc.equalsIgnoreCase("Y") ) {
                return new OperationResult.StepResult(OperationResult.Status.OK , "prvLoc enrichment skipped");
            }else {
                String location = "";
                if(user.getDesignation().contains(DISTRICT)) {
                    location = user.getLocation().getDistrict() + " > " + user.getLocation().getCountry();
                }else {
                    location = user.getLocation().getBranch() + " > " + user.getLocation().getDistrict() + " > " + user.getLocation().getCountry();
                }
                if(StringUtils.isNullOrBlank(location)) {
                    return new OperationResult.StepResult(OperationResult.Status.ERROR, "The location hierarchy for the given user is null or empty");
                }

                String conditions = " where location_hierarchy = ?";
                boolean isEmpty = false;
                try {
                    List<Map<String,Object>> result = queryService.execute(QUERY + conditions, location);
                    isEmpty = (result == null || result.isEmpty());
                    String assignedHierarchy= "";
                    if(!isEmpty) {
                        for(Map<String, Object> userLoginMap : result) {
                            String userId = userLoginMap.get("loginid").toString();
                            if(userId.equalsIgnoreCase(user.getLoginid())) {
                                continue;
                            }
                            User userFromDB = userService.findByLoginId(userId);
                            assignedHierarchy += userFromDB.getHierarchy() + ",";
                        }
                        if(assignedHierarchy.endsWith(",")) {
                            assignedHierarchy=assignedHierarchy.substring(0,assignedHierarchy.length()-1);
                        }
                    }
                    if (StringUtils.isNullOrBlank(assignedHierarchy)) {
                        return new OperationResult.StepResult(OperationResult.Status.ERROR, "No data found to create the assigned hierarchies for ");
                    }
                    user.setAssignedHierarchy(assignedHierarchy);
                    return new OperationResult.StepResult(OperationResult.Status.OK, StringUtils.format("prvloc enrichment successful for user {} with assigned hierarchies {}", user,assignedHierarchy));
                }catch (Exception e) {
                    throw new EnrichmentFailException(StringUtils.format("Unable to enrich prvLoc for user {} , due to exception {}", user,e));
                }
            }

        }else {
            return new OperationResult.StepResult(OperationResult.Status.OK , "prvLoc enrichment skipped");
        }
    }
}