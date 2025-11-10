package com.applicate.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.User;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.applicate.services.channelkart.services.UserService;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

public class OutletRCSIDEnrichment extends AbstractEnrichment<OutletDetails> {

	private UserService userService = (UserService) ServiceLocator.lookup(User.class);
	private static final Logger logger = LoggerFactory.getLogger(OutletRCSIDEnrichment.class);


	@Override
	public OperationResult.StepResult apply(OutletDetails outlet) {

		List<HierarchyMetadata> parent = outlet.getImmediateParent();
		ObjectNode extendedAttributes = (ObjectNode) outlet.getExtendedAttributes();
		if (parent != null && !parent.isEmpty()) {
			logger.info("<<<<<<<< Executing UID update for immediate parent Stockist :  STARTED ");
			String immediateParent = parent.get(0).getImmediateParent();
			// immediateParent of the outlet
			User user = userService.findByLoginId(immediateParent);
			Set<String> userDesignation = user.getDesignation();
			Object[] des = userDesignation.toArray();
			if (des[0].toString().equalsIgnoreCase("STOCKIST")) {
				extendedAttributes.put("UID", user.getLoginId());
				extendedAttributes.put("RCSID", user.getLoginId());
				outlet.setExtendedAttributes(extendedAttributes);
				logger.info("<<<<<<<< Executing UID update for immediate parent Stockist :  FINISHED ");
				return new OperationResult.StepResult(OperationResult.Status.OK, "outlet enriched successfull");

			}
		}
		return new OperationResult.StepResult(OperationResult.Status.ERROR, "outlet does not have an immediate parent for outletCode " + outlet.getOutletcode());

	}
}