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
	public static final java.lang.String ACTIVE_STATUS_NOT_ENRICHED = "active status not enriched";
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
		Location location = parseLocationHierarchy(user.getLocationHierarchy(), mapper);

		User immediateParent = getImmediateParentUser(user);
		location = enrichCountryIfMissing(location);

		if (immediateParent != null && immediateParent.getLocationHierarchy() != null) {
			enrichmentMsg = enrichFromParentLocation(mapper, location, immediateParent);
		}

		try {
			user.setLocationHierarchy(mapper.writeValueAsString(location));
		} catch (Exception e) {
			throw new EnrichmentFailException("Failed to serialize Location back to JSON: " + e.getMessage());
		}
		return enrichmentMsg;
	}

	private Location parseLocationHierarchy(String hierarchy, ObjectMapper mapper) {
		if (hierarchy == null) return new Location();
		try {
			return mapper.readValue(hierarchy, Location.class);
		} catch (Exception e) {
			throw new EnrichmentFailException("Failed to parse locationHierarchy JSON: " + e.getMessage());
		}
	}

	private User getImmediateParentUser(User user) {
		if (user.getImmediateParent() == null || user.getImmediateParent().isEmpty()) return null;
		HierarchyMetadata hmd = user.getImmediateParent().get(0);
		if (hmd == null || hmd.getParent() == null) return null;
		return userService.findByLoginId(hmd.getParent());
	}

	private Location enrichCountryIfMissing(Location location) {
		if (StringUtils.isNotBlank(location.getCountry())) {
			location.setCountry(location.getCountry().trim().toUpperCase());
		} else {
			location.setCountry("INDIA");
		}
		return location;
	}

	private String enrichFromParentLocation(ObjectMapper mapper, Location location, User immediateParent) {
		try {
			Location parentLoc = parseParentLocation(immediateParent.getLocationHierarchy(), mapper);
			copyMissingLocationFields(location, parentLoc);
			return "location enriched from parent";
		} catch (Exception e) {
			throw new EnrichmentFailException("Failed to parse parent locationHierarchy JSON: " + e.getMessage());
		}
	}

	private Location parseParentLocation(String parentLocHierarchy, ObjectMapper mapper) throws Exception {
		if (parentLocHierarchy.trim().startsWith("{") || parentLocHierarchy.trim().startsWith("[")) {
			return mapper.readValue(parentLocHierarchy, Location.class);
		}
		Location parentLoc = new Location();
		String[] parts = parentLocHierarchy.split(">");
		if (parts.length > 0) parentLoc.setBranch(parts[0]);
		if (parts.length > 1) parentLoc.setDistrict(parts[1]);
		if (parts.length > 2) parentLoc.setCountry(parts[2]);
		return parentLoc;
	}

	private void copyMissingLocationFields(Location target, Location source) {
		if (StringUtils.isNullOrBlank(target.getBranch())) target.setBranch(source.getBranch());
		if (StringUtils.isNullOrBlank(target.getState())) target.setState(source.getState());
		if (StringUtils.isNullOrBlank(target.getCity())) target.setCity(source.getCity());
	}

	private String enrichSupplierMetadata(User user) {
		if (user.getDesignation() == null || !user.getDesignation().contains("wd")) {
			return "Supplier Metadata enrichment skipped";
		}

		List<SupplierMetadata> supplierMetaDataList = initializeSupplierList(user);
		boolean enriched = enrichSupplierFields(supplierMetaDataList);
		return enriched ? "Supplier Metadata enriched for WD" : "Supplier Metadata enrichment skipped";
	}

	private List<SupplierMetadata> initializeSupplierList(User user) {
		List<SupplierMetadata> supplierMetaDataList = user.getSupplierMetaData();
		if (supplierMetaDataList == null || supplierMetaDataList.isEmpty()) {
			supplierMetaDataList = new ArrayList<>();
			supplierMetaDataList.add(new SupplierMetadata());
			user.setSupplierMetaData(supplierMetaDataList);
		}
		return supplierMetaDataList;
	}

	private boolean enrichSupplierFields(List<SupplierMetadata> supplierMetaDataList) {
		boolean enriched = false;
		for (SupplierMetadata meta : supplierMetaDataList) {
			if (meta.getType() == null || meta.getType().isBlank()) {
				meta.setType("amount");
				enriched = true;
			}
			if (meta.getMin() == null) {
				meta.setMin(0);
				enriched = true;
			}
			if (meta.getMax() == null) {
				meta.setMax(1000000);
				enriched = true;
			}
		}
		return enriched;
	}

	private String enrichActiveStatus(User user) {
		if (user.getDesignation() == null || user.getActiveStatus() != null) {
			return ACTIVE_STATUS_NOT_ENRICHED;
		}

		String designation = user.getDesignation();
		if (isAdminDesignation(designation)) {
			setActive(user, ActiveStatus.ACTIVE, ActiveStatus.ACTIVE.getStatus());
			return "active status enriched";
		}

		if (isSalesDesignation(designation)) {
			return enrichActiveStatusFromAUS(user);
		}

		setActive(user, ActiveStatus.INACTIVE, "Unknown designation");
		return ACTIVE_STATUS_NOT_ENRICHED;
	}

	private boolean isAdminDesignation(String designation) {
		return designation.contains("wd") || designation.contains("branch") || designation.contains("district");
	}

	private boolean isSalesDesignation(String designation) {
		return designation.contains("psr") || designation.contains("stockist");
	}

	private String enrichActiveStatusFromAUS(User user) {
		JsonNode extendedAttributes = user.getExtendedAttributes();
		if (extendedAttributes == null || !extendedAttributes.hasNonNull("AUS")) {
			return ACTIVE_STATUS_NOT_ENRICHED;
		}

		String aus = extendedAttributes.get("AUS").asText();
		switch (aus.toUpperCase()) {
			case "Y":
				setActive(user, ActiveStatus.ACTIVE, "active status activated as per AUS field");
				return "active status enriched as per AUS field";
			case "N":
				setActive(user, ActiveStatus.INACTIVE, "active status inactive as per AUS field");
				return "active status enriched as per AUS field";
			default:
				throw new EnrichmentFailException("AUS field should be Y or N for PSR and stockist");
		}
	}

	private void setActive(User user, ActiveStatus status, String reason) {
		user.setActiveStatus(status);
		user.setActiveStatusReason(reason);
	}
}
