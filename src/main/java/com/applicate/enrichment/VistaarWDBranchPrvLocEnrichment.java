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

    private static final String BRANCH = "branch";
    private static final String DISTRICT = "district";
    private static final String QUERY = "select loginid from ck_user ";

    @Override
    public OperationResult.StepResult apply(User user) {

        final UserService userService = (UserService) ServiceLocator.lookup(User.class);
        final QueryService queryService = new QueryService();

        // 1. Check if enrichment should be skipped
        if (!isApplicableForEnrichment(user)) {
            return new OperationResult.StepResult(OperationResult.Status.OK , "prvLoc enrichment skipped");
        }

        // 2. Get the location string
        String location = getLocationHierarchyString(user);
        if (StringUtils.isNullOrBlank(location)) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "The location hierarchy for the given user is null or empty");
        }

        // 3. Perform the main logic
        try {
            return findAndSetAssignedHierarchy(user, location, userService, queryService);
        } catch (Exception e) {
            throw new EnrichmentFailException(StringUtils.format("Unable to enrich prvLoc for user {} , due to exception {}", user,e));
        }
    }

    /**
     * Checks if the user has the correct designation and "prvLoc" flag.
     */
    private boolean isApplicableForEnrichment(User user) {
        if (user.getDesignation() == null ||
                (!user.getDesignation().contains(DISTRICT) && !user.getDesignation().contains(BRANCH))) {
            return false;
        }

        String prvLoc = "";
        if (user.getExtendedAttributes() != null && user.getExtendedAttributes().hasNonNull("prvLoc")) {
            prvLoc = user.getExtendedAttributes().get("prvLoc").textValue();
        }

        // Only return true if the prvLoc flag is explicitly "Y"
        return "Y".equalsIgnoreCase(prvLoc);
    }

    /**
     * Builds the location_hierarchy string based on the user's designation.
     */
    private String getLocationHierarchyString(User user) {
        if (user.getDesignation().contains(DISTRICT)) {
            return user.getLocation().getDistrict() + " > " + user.getLocation().getCountry();
        } else {
            return user.getLocation().getBranch() + " > " + user.getLocation().getDistrict() + " > " + user.getLocation().getCountry();
        }
    }

    /**
     * Executes the query and builds the assigned hierarchy.
     */
    private OperationResult.StepResult findAndSetAssignedHierarchy(User user, String location, UserService userService, QueryService queryService) {

        String conditions = " where location_hierarchy = ?";
        List<Map<String,Object>> result = queryService.execute(QUERY + conditions, location);

        if (result == null || result.isEmpty()) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "No data found to create the assigned hierarchies for ");
        }

        String assignedHierarchy = buildAssignedHierarchyString(result, user.getLoginid(), userService);

        if (StringUtils.isNullOrBlank(assignedHierarchy)) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "No other users found to create assigned hierarchies");
        }

        user.setAssignedHierarchy(assignedHierarchy);
        return new OperationResult.StepResult(OperationResult.Status.OK, StringUtils.format("prvloc enrichment successful for user {} with assigned hierarchies {}", user,assignedHierarchy));
    }

    /**
     * Loops through DB results to build the hierarchy string.
     */
    private String buildAssignedHierarchyString(List<Map<String, Object>> result, String currentUserLoginId, UserService userService) {
        StringBuilder assignedHierarchyBuilder = new StringBuilder();

        for (Map<String, Object> userLoginMap : result) {
            String userId = userLoginMap.get("loginid").toString();

            if (userId.equalsIgnoreCase(currentUserLoginId)) {
                continue;
            }

            User userFromDB = userService.findByLoginId(userId);
            if (userFromDB != null && userFromDB.getHierarchy() != null) {
                assignedHierarchyBuilder.append(userFromDB.getHierarchy()).append(",");
            }
        }
        
        if (assignedHierarchyBuilder.length() > 0) {
            assignedHierarchyBuilder.setLength(assignedHierarchyBuilder.length() - 1);
        }

        return assignedHierarchyBuilder.toString();
    }
}