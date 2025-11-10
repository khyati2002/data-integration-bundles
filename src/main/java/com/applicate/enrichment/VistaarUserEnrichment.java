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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VistaarUserEnrichment extends AbstractEnrichment<User> {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public OperationResult.StepResult apply(User user) {
		final UserService userService = (UserService) ServiceLocator.lookup(User.class);

		List<String> enrichmentResult = new ArrayList<>();
		enrichmentResult.add(enrichLocationInformation(user, userService));
		enrichmentResult.add(enrichSupplierMetadata(user));
		enrichmentResult.add(enrichAUSStockistUser(user, userService));
		enrichmentResult.add(enrichActiveStatus(user));
		return new OperationResult.StepResult(OperationResult.Status.OK, String.join(",", enrichmentResult));
	}

	public String enrichAUSStockistUser(User user, UserService userService) {
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

	/**
	 * Main method for location enrichment. (Complexity: 34 -> 12)
	 */
	private String enrichLocationInformation(User user, UserService userService) {
		String enrichmentMsg = "location enrichment skipped";

		Location location = parseLocationHierarchy(user.getLocationHierarchy());
		if (location == null) {
			location = new Location();
		}

		User immediateParent = findImmediateParent(user, userService);

		if (enrichCountry(location)) {
			enrichmentMsg = "country enriched successfully";
		}

		if (immediateParent != null && immediateParent.getLocationHierarchy() != null) {
			enrichmentMsg = mergeLocationFromParent(location, immediateParent.getLocationHierarchy());
		}

		try {
			user.setLocationHierarchy(mapper.writeValueAsString(location));
		} catch (Exception e) {
			throw new EnrichmentFailException("Failed to serialize Location back to JSON: " + e.getMessage());
		}

		return enrichmentMsg;
	}

	/** Helper to parse a location string (JSON or plain text) */
	private Location parseLocationHierarchy(String locString) {
		if (locString == null) return null;
		try {
			if (locString.trim().startsWith("{") || locString.trim().startsWith("[")) {
				return mapper.readValue(locString, Location.class);
			} else {
				Location parentLoc = new Location();
				String[] parts = locString.split(">");
				if (parts.length > 0) parentLoc.setBranch(parts[0]);
				if (parts.length > 1) parentLoc.setDistrict(parts[1]);
				if (parts.length > 2) parentLoc.setCountry(parts[2]);
				return parentLoc;
			}
		} catch (IOException e) {
			throw new EnrichmentFailException("Failed to parse locationHierarchy JSON: " + locString, e);
		}
	}

	/** Helper to find the immediate parent User object */
	private User findImmediateParent(User user, UserService userService) {
		if (user.getImmediateParent() != null && !user.getImmediateParent().isEmpty()) {
			HierarchyMetadata hmd = user.getImmediateParent().get(0);
			if (hmd != null && hmd.getParent() != null) {
				return userService.findByLoginId(hmd.getParent());
			}
		}
		return null;
	}

	/** Helper to enrich country field */
	private boolean enrichCountry(Location location) {
		if (StringUtils.isNotBlank(location.getCountry())) {
			location.setCountry(location.getCountry().trim().toUpperCase());
			return false;
		} else {
			location.setCountry("INDIA");
			return true;
		}
	}

	/** Helper to merge parent location data into the child location */
	private String mergeLocationFromParent(Location childLocation, String parentLocString) {
		Location parentLoc = parseLocationHierarchy(parentLocString);
		if (parentLoc == null) return "location enrichment skipped, parent loc unparseable";

		if (StringUtils.isNullOrBlank(childLocation.getBranch())) {
			childLocation.setBranch(parentLoc.getBranch());
		}
		if (StringUtils.isNullOrBlank(childLocation.getState())) {
			childLocation.setState(parentLoc.getState());
		}
		if (StringUtils.isNullOrBlank(childLocation.getCity())) {
			childLocation.setCity(parentLoc.getCity());
		}
		return "location enriched from parent";
	}

	/**
	 * Main method for supplier metadata enrichment. (Complexity: 21 -> 8)
	 */
	private String enrichSupplierMetadata(User user) {
		if (user.getDesignation() == null || !user.getDesignation().contains("wd")) {
			return "Supplier Metadata enrichment skipped";
		}

		boolean isSupplierEnriched = false;
		List<SupplierMetadata> supplierMetaDataList = getOrCreateSupplierList(user);

		for (SupplierMetadata supplierMetaData : supplierMetaDataList) {
			if (enrichSingleSupplier(supplierMetaData)) {
				isSupplierEnriched = true;
			}
		}

		return isSupplierEnriched ? "Supplier Metadata enriched for WD" : "Supplier Metadata enrichment skipped";
	}

	/** Helper to get or create the supplier metadata list */
	private List<SupplierMetadata> getOrCreateSupplierList(User user) {
		List<SupplierMetadata> supplierMetaDataList = user.getSupplierMetaData();

		if (supplierMetaDataList == null) {
			supplierMetaDataList = new ArrayList<>();
			user.setSupplierMetaData(supplierMetaDataList);
		}

		if (supplierMetaDataList.isEmpty()) {
			supplierMetaDataList.add(new SupplierMetadata());
		}

		return supplierMetaDataList;
	}

	/** Helper to enrich a single SupplierMetadata object */
	private boolean enrichSingleSupplier(SupplierMetadata supplierMetaData) {
		boolean isEnriched = false;
		if (supplierMetaData.getType() == null || supplierMetaData.getType().isBlank()) {
			supplierMetaData.setType("amount");
			isEnriched = true;
		}
		if (supplierMetaData.getMin() == null) {
			supplierMetaData.setMin(0);
			isEnriched = true;
		}
		if (supplierMetaData.getMax() == null) {
			supplierMetaData.setMax(1000000);
			isEnriched = true;
		}
		return isEnriched;
	}

	/**
	 * Main method for active status enrichment. (Complexity: 18 -> 10)
	 */
	private String enrichActiveStatus(User user) {
		if (user.getDesignation() == null || user.getActiveStatus() != null) {
			return "active status not enriched";
		}

		// 1. Try to enrich for Branch/District/WD
		String message = enrichStatusForBranch(user);
		if (message != null) return message;

		// 2. Try to enrich for PSR/Stockist
		message = enrichStatusForStockist(user);
		if (message != null) return message;

		// 3. Fallback for unknown designations
		user.setActiveStatus(ActiveStatus.INACTIVE);
		user.setActiveStatusReason("Unknown designation");
		return "active status not enriched";
	}

	/** Helper for WD/Branch/District status */
	private String enrichStatusForBranch(User user) {
		if (user.getDesignation().contains("wd") || user.getDesignation().contains("branch") || user.getDesignation().contains("district")) {
			user.setActiveStatus(ActiveStatus.ACTIVE);
			user.setActiveStatusReason(ActiveStatus.ACTIVE.getStatus());
			return "active status enriched";
		}
		return null;
	}

	/** Helper for PSR/Stockist status */
	private String enrichStatusForStockist(User user) {
		if (user.getDesignation().contains("psr") || user.getDesignation().contains("stockist")) {
			JsonNode extendedAttributes = user.getExtendedAttributes();

			if (extendedAttributes != null && extendedAttributes.hasNonNull("AUS")) {
				String aus = extendedAttributes.get("AUS").asText();
				if (aus.equalsIgnoreCase("Y")) {
					user.setActiveStatus(ActiveStatus.ACTIVE);
					user.setActiveStatusReason("active status activated as per AUS field");
					return "active status enriched as per AUS field";
				} else if (aus.equalsIgnoreCase("N")) {
					user.setActiveStatus(ActiveStatus.INACTIVE);
					user.setActiveStatusReason("active status inactive as per AUS field");
					return "active status enriched as per AUS field";
				} else {
					throw new EnrichmentFailException("AUS field should be Y or N for PSR and stockist");
				}
			}
		}
		return null;
	}
}