package com.applicate.enrichment;


import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VistaarOutletEnrichment extends AbstractEnrichment<OutletDetails> {

	public static final String SUPPLIER = "supplier";
	private static final String DESIGNATION_STOCKIST = "STOCKIST";


	@Override
	public OperationResult.StepResult apply(OutletDetails outletDetails) {
		UserService userService = (UserService) ServiceLocator.lookup(User.class);
		String enrichmentMsg = "";
		List<HierarchyMetadata> hierarchyMetaDataList = outletDetails.getImmediateParent();
		User userinDb = null;

		HierarchyMetadata parent0 = null;
		if (hierarchyMetaDataList != null && !hierarchyMetaDataList.isEmpty()) {
			parent0 = hierarchyMetaDataList.get(0);
			String immediateParent = parent0.getImmediateParent();
			userinDb = userService.findByLoginId(immediateParent);
		}
		if (userinDb != null) {
			Location location = userinDb.getLocation();
			Location outletLocation = outletDetails.getLocation();
			if (outletLocation == null) {
				outletLocation = new Location();
				outletDetails.setLocation(outletLocation);
			}
			if (StringUtils.isNullOrBlank(outletLocation.getCountry())) {
				outletLocation.setCountry(location.getCountry());
			}
			if (StringUtils.isNullOrBlank(outletLocation.getDistrict())) {
				outletLocation.setBranch(location.getDistrict());
			}
			if (StringUtils.isNullOrBlank(outletLocation.getBranch())) {
				outletLocation.setState(location.getBranch());
			}
			enrichmentMsg += "Enriched location for the outlet. ";

			if (!outletDetails.getOutletType().equalsIgnoreCase(DESIGNATION_STOCKIST)
					&& !outletDetails.getChannel().equalsIgnoreCase(DESIGNATION_STOCKIST)) {
				ObjectNode extendedAttributes = (ObjectNode) outletDetails.getExtendedAttributes();
				JsonNode supplierNode = userinDb.getExtendedAttributes().get(SUPPLIER);
				extendedAttributes.putPOJO(SUPPLIER, userinDb.getExtendedAttributes().get(SUPPLIER));
				outletDetails.setExtendedAttributes(extendedAttributes);
				enrichmentMsg += "Enriched supplier for retails outlet. ";
				enrichmentImmediateParent(outletDetails, supplierNode);
			}

		}
		return new OperationResult.StepResult(OperationResult.Status.OK, enrichmentMsg);
	}

	/**
	 * for not stockist outlets, filters hierarchy metadata based on suppliers
	 * @param outletDetails
	 * @param supplierNode
	 */
	private void enrichmentImmediateParent(OutletDetails outletDetails, JsonNode supplierNode) {
		// Collect all supplier strings into a set
		Set<String> supplierSet = new HashSet<>();
		for (JsonNode supplier : supplierNode) {
			supplierSet.add(supplier.asText());
		}

		// Synchronize on the immediateParent list to ensure thread safety while modifying it
		synchronized (outletDetails.getImmediateParent()) {
			outletDetails.getImmediateParent().removeIf(hierarchyMetaData -> {
				// Validate hierarchyMetaData and its immediate parent
				String hierarchyStr = hierarchyMetaData.getHierarchy();
				if(hierarchyStr== null) return false;
				// Check if the hierarchy string matches any supplier criteria
				for (String s : supplierSet) {
					if (hierarchyStr.contains("> " + s + " >")) {
						// Found a match; keep this element
						return false;
					}
				}
				// No match found; remove this element
				return true;
			});
		}
	}

}



