package com.applicate.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.exceptions.EnrichmentFailException;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.services.QueryService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;

import java.util.List;
import java.util.Map;

public class VistaarWDBranchPrvLocEnrichment extends AbstractEnrichment<User> {

    private final UserService userService = SpringContext.getBean(UserService.class);
    private static final String QUERY = "select loginid from ck_user ";
    private final QueryService queryService = SpringContext.getBean(QueryService.class);
    private static final String BRANCH = "branch";
    private static final String DISTRICT = "district";
    
    @Override
    public EnrichmentResult apply(User user) {
    	
    	if(user.getDesignation() != null && (user.getDesignation().contains(DISTRICT) || user.getDesignation().contains(BRANCH))) {
    		String prvLoc = "";
            if (user.getExtendedAttributes() != null && user.getExtendedAttributes().hasNonNull("prvLoc")) {
                prvLoc = user.getExtendedAttributes().get("prvLoc").textValue();
            }
            if(prvLoc == null || !prvLoc.equalsIgnoreCase("Y") ) {
            	return new EnrichmentResult(Status.OK , "prvLoc enrichment skipped");
            }else {
            	String location = "";
                	if(user.getDesignation().contains(DISTRICT)) {
                		location = user.getLocationHierarchy().getDistrict() + " > " + user.getLocationHierarchy().getCountry();
                	}else {
                		location = user.getLocationHierarchy().getBranch() + " > " + user.getLocationHierarchy().getDistrict() + " > " + user.getLocationHierarchy().getCountry();
                	}
                    if(StringUtils.isNullOrBlank(location)) {
                        return new EnrichmentResult(Status.ERROR, "The location hierarchy for the given user is null or empty");
                    }
                    String conditions = " where location_hierarchy='"+ location+"'";
                    boolean isEmpty = false;
                    try {
                        List<Map<String,Object>> result = queryService.execute(QUERY+conditions);
                        isEmpty = (result == null || result.isEmpty());
                        String assignedHierarchy= "";
                        if(!isEmpty) {
                            for(Map<String, Object> userLoginMap : result) {
                                String userId = userLoginMap.get("loginid").toString();
                                if(userId.equalsIgnoreCase(user.getLoginId())) {
                                    continue;
                                }
                                User userFromDB = userService.findByLoginId(userId);
                                assignedHierarchy+=userFromDB.getHierarchy()+",";
                            }
                            if(assignedHierarchy.endsWith(",")) {
                                assignedHierarchy=assignedHierarchy.substring(0,assignedHierarchy.length()-1);
                            }
                        }
                        if (StringUtils.isNullOrBlank(assignedHierarchy)) {
                            return new EnrichmentResult(Status.ERROR, "No data found to create the assigned hierarchies for ");
                        }
                        user.setAssignedHierarchy(assignedHierarchy);
                        return new EnrichmentResult(Status.OK, StringUtils.format("prvloc enrichment successful for user {} with assigned hierarchies {}", user,assignedHierarchy));
                    }catch (Exception e) {
                        throw new EnrichmentFailException(StringUtils.format("Unable to enrich prvLoc for user {} , due to exception {}", user,e));
                    }
            }
   	     
    	}else {
    		return new EnrichmentResult(Status.OK , "prvLoc enrichment skipped");
    	}
    }
}