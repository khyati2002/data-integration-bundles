package com.applicate.enrichment;

import com.applicate.services.channelkart.exceptions.EnrichmentFailException;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.salescode.dim.jooq.impl.HierarchyMetadata;

import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class VistaarUserEnrichment extends AbstractEnrichment<User> {
	//UserService userService = (UserService) ServiceLocator.lookup(User.class);
	private transient UserService userService;

	@Override
	public OperationResult.StepResult apply(User user) {

		if (this.userService == null) {
			this.userService = (UserService) ServiceLocator.lookup(User.class);
		}

		List<String> enrichmentResult = new ArrayList<>();
		enrichmentResult.add(enrichLocationInformation(user));
		enrichmentResult.add(enrichSupplierMetadata(user));
		enrichmentResult.add(enrichAUSStockistUser(user));
		enrichmentResult.add(enrichActiveStatus(user));
		return new OperationResult.StepResult(OperationResult.Status.OK, String.join(",", enrichmentResult));
	}

	public String enrichAUSStockistUser(User user) {
		String enrichmentMsg = "AUS enrichment skipped";

		if (user.getDesignation().contains("stockist")) {
			JsonNode curExtendedAttributes = user.getExtendedAttributes();
			User userInDb = userService.findByLoginId(user.getLoginId());

			if (curExtendedAttributes != null && curExtendedAttributes.has("AUS")) {
				JsonNode curAUS = curExtendedAttributes.get("AUS");
				ObjectNode newExtendedAttributes = (ObjectNode) curExtendedAttributes;

				if (curAUS.isNull() || StringUtils.isNullOrBlank(curAUS.asText())) {
					if (userInDb != null) {
						JsonNode ausInDb = userInDb.getExtendedAttributes().get("AUS");
						newExtendedAttributes.set("AUS", ausInDb);
					} else {
						newExtendedAttributes.put("AUS", "N");
					}
					user.setExtendedAttributes(newExtendedAttributes);
					enrichmentMsg = "AUS enriched successfully";
				}

			}
		}
		return enrichmentMsg;
	}

	private String enrichLocationInformation(User user) {
		ObjectMapper mapper = new ObjectMapper();
		String enrichmentMsg = "location enrichment skipped";
		Location location = null;
		try {
			if (user.getLocationHierarchy() != null) {
				location = mapper.readValue(user.getLocationHierarchy(), Location.class);
			}
		} catch (Exception e) {
			throw new EnrichmentFailException("Failed to parse locationHierarchy JSON: " + e.getMessage());
		}

		if (location == null) {
			location = new Location();
		}
		User immediateParent = null;
		if (user.getImmediateParent() != null && !user.getImmediateParent().isEmpty()) {
			HierarchyMetadata hmd = user.getImmediateParent().get(0);
			if (hmd != null && hmd.getParent() != null) {
				immediateParent = userService.findByLoginId(hmd.getParent());
			}
		}

		if (StringUtils.isNotBlank(location.getCountry())) {
			location.setCountry(location.getCountry().trim().toUpperCase());
		} else {
			location.setCountry("INDIA");
			enrichmentMsg = "country enriched successfully";
		}

		if (immediateParent != null && immediateParent.getLocationHierarchy() != null) {
			try {
				String parentLocHierarchy = immediateParent.getLocationHierarchy();
				Location parentLoc;

				if (parentLocHierarchy.trim().startsWith("{") || parentLocHierarchy.trim().startsWith("[")) {
					parentLoc = mapper.readValue(parentLocHierarchy, Location.class);
				} else {
					parentLoc = new Location();
					String[] parts = parentLocHierarchy.split(">");
					if (parts.length > 0) parentLoc.setBranch(parts[0]);
					if (parts.length > 1) parentLoc.setDistrict(parts[1]);
					if (parts.length > 2) parentLoc.setCountry(parts[2]);
				}
				if (StringUtils.isNullOrBlank(location.getBranch())) {
					location.setBranch(parentLoc.getBranch());
				}
				if (StringUtils.isNullOrBlank(location.getState())) {
					location.setState(parentLoc.getState());
				}
				if (StringUtils.isNullOrBlank(location.getCity())) {
					location.setCity(parentLoc.getCity());
				}
				enrichmentMsg = "location enriched from parent";
			} catch (Exception e) {
				throw new EnrichmentFailException("Failed to parse parent locationHierarchy JSON: " + e.getMessage());
			}
		}

		try {
			user.setLocationHierarchy(mapper.writeValueAsString(location));
		} catch (Exception e) {
			throw new EnrichmentFailException("Failed to serialize Location back to JSON: " + e.getMessage());
		}

		return enrichmentMsg;
	}

	private String enrichSupplierMetadata(User user) {
		boolean isSupplierEnriched = false;
		if (user.getDesignation() != null && user.getDesignation().contains("wd")) {
			List<SupplierMetadata> supplierMetaDataList = user.getSupplierMetaData();
			if (supplierMetaDataList == null || supplierMetaDataList.isEmpty()) {
				SupplierMetadata supplierMetaData = new SupplierMetadata();
				if (supplierMetaDataList == null) {
					supplierMetaDataList = new ArrayList<>();
					user.setSupplierMetaData(supplierMetaDataList);
				}
				supplierMetaDataList.add(supplierMetaData);
			}
			for (SupplierMetadata supplierMetaData : supplierMetaDataList) {
				if (supplierMetaData.getType() == null || supplierMetaData.getType().isBlank()) {
					supplierMetaData.setType("amount");
					isSupplierEnriched = true;
				}
				if (supplierMetaData.getMin() == null) {
					supplierMetaData.setMin(0);
					isSupplierEnriched = true;
				}
				if (supplierMetaData.getMax() == null) {
					supplierMetaData.setMax(1000000);
					isSupplierEnriched = true;
				}
			}
		}

		if (isSupplierEnriched) {
			return "Supplier Metadata enriched for WD";
		}
		return "Supplier Metadata enrichment skipped";
	}

	private String enrichActiveStatus(User user) {
		if (user.getDesignation() != null && user.getActiveStatus() == null) {
				if (user.getDesignation().contains("wd") || user.getDesignation().contains("branch") || user.getDesignation().contains("district")) {
					user.setActiveStatus(ActiveStatus.ACTIVE);
					user.setActiveStatusReason(ActiveStatus.ACTIVE.getStatus());
					return "active status enriched";
				} else if ((user.getDesignation().contains("psr")) || (user.getDesignation().contains("stockist"))) {
					JsonNode extendedAttributes = user.getExtendedAttributes();
					if (extendedAttributes != null && extendedAttributes.hasNonNull("AUS")) {
						String AUS = extendedAttributes.get("AUS").asText();
						if (AUS.equalsIgnoreCase("Y")) {
							user.setActiveStatus(ActiveStatus.ACTIVE);
							user.setActiveStatusReason("active status activated as per AUS field");
							return "active status enriched as per AUS field";
						} else if (AUS.equalsIgnoreCase("N")) {
							user.setActiveStatus(ActiveStatus.INACTIVE);
							user.setActiveStatusReason("active status inactive as per AUS field");
							return "active status enriched as per AUS field";
						} else {
							throw new EnrichmentFailException("AUS field should be Y or N for PSR and stockist");
						}
					}
				} else {
					user.setActiveStatus(ActiveStatus.INACTIVE);
					user.setActiveStatusReason("Unknown designation");
				}
			}

		return "active status not enriched";
	}

}
