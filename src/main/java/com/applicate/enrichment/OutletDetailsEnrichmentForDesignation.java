
package com.applicate.enrichment;

import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;


public class OutletDetailsEnrichmentForDesignation extends AbstractEnrichment<OutletDetails>{

	final UserService userService = (UserService) ServiceLocator.lookup(User.class);

	private static final String ISVERIFIED="isVerified";


	@Override
	public OperationResult.StepResult apply(OutletDetails cdm) {

		try {
			boolean defaultValue = false;

			String userName= SecurityContextUtils.getPrincipal();
			User user = userService.findByLoginId(userName);
			Set<String> userDesignation=user.getDesignation();
			Object[] des=userDesignation.toArray();
			ObjectNode extendedAttributes = (ObjectNode) cdm.getExtendedAttributes();
			if(!cdm.getExtendedAttributes().has("designation")) {
				extendedAttributes.put("designation",des[0].toString());
			}
			if(!cdm.getExtendedAttributes().has(ISVERIFIED) ) {
				extendedAttributes.put(ISVERIFIED, defaultValue);
			}
			cdm.setExtendedAttributes(extendedAttributes);
			return new OperationResult.StepResult(OperationResult.Status.OK, "Designation enriched successfully");

		}catch(Exception e){
			return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getLocalizedMessage());
		}
	}

}