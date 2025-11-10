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

	private String enrichLocationInformation(User user) {
		ObjectMapper mapper = new ObjectMapper();
		Location location = parseLocationHierarchy(user.getLocationHierarchy(), mapper);
		String enrichmentMsg = enrichCountry(location);

		User immediateParent = getImmediateParentUser(user);
		if (immediateParent != null) {
			enrichmentMsg = tryEnrichFromParent(location, immediateParent, mapper, enrichmentMsg);
		}

		serializeLocation(user, location, mapper);
		return enrichmentMsg;
	}

	private String tryEnrichFromParent(Location location, User parent, ObjectMapper mapper, String msg) {
		if (parent.getLocationHierarchy() == null) return msg;
		try {
			Location parentLoc = parseParentLocation(parent.getLocationHierarchy(), mapper);
			return fillMissingLocationFields(location, parentLoc) ? "location enriched from parent" : msg;
		} catch (Exception e) {
			throw new EnrichmentFailException("Failed to parse parent locationHierarchy JSON: " + e.getMessage());
		}
	}

	private String enrichSupplierMetadata(User user) {
		if (!isWDUser(user)) return "Supplier Metadata enrichment skipped";

		List<SupplierMetadata> metadataList = ensureSupplierMetadataList(user);
		boolean enriched = enrichSupplierMetadataFields(metadataList);
		return enriched ? "Supplier Metadata enriched for WD" : "Supplier Metadata enrichment skipped";
	}

	private boolean isWDUser(User user) {
		return user.getDesignation() != null && user.getDesignation().contains("wd");
	}

	private List<SupplierMetadata> ensureSupplierMetadataList(User user) {
		List<SupplierMetadata> list = user.getSupplierMetaData();
		if (list == null || list.isEmpty()) {
			list = new ArrayList<>();
			list.add(new SupplierMetadata());
			user.setSupplierMetaData(list);
		}
		return list;
	}

	private boolean enrichSupplierMetadataFields(List<SupplierMetadata> list) {
		boolean enriched = false;
		for (SupplierMetadata meta : list) {
			if (setIfBlank(meta::getType, meta::setType, "amount")) enriched = true;
			if (setIfNull(meta::getMin, meta::setMin, 0)) enriched = true;
			if (setIfNull(meta::getMax, meta::setMax, 1_000_000)) enriched = true;
		}
		return enriched;
	}

	private boolean setIfBlank(Supplier<String> getter, Consumer<String> setter, String value) {
		if (StringUtils.isNullOrBlank(getter.get())) {
			setter.accept(value);
			return true;
		}
		return false;
	}

	private <T> boolean setIfNull(Supplier<T> getter, Consumer<T> setter, T value) {
		if (getter.get() == null) {
			setter.accept(value);
			return true;
		}
		return false;
	}

	private String enrichActiveStatus(User user) {
		if (user.getDesignation() == null || user.getActiveStatus() != null)
			return ACTIVE_STATUS_NOT_ENRICHED;

		if (isOrgLevelUser(user)) return activateUser(user);
		if (isSalesUser(user)) return handleSalesUserActiveStatus(user);

		deactivateUnknownDesignation(user);
		return ACTIVE_STATUS_NOT_ENRICHED;
	}

	private boolean isOrgLevelUser(User user) {
		String d = user.getDesignation();
		return d.contains("wd") || d.contains("branch") || d.contains("district");
	}

	private boolean isSalesUser(User user) {
		String d = user.getDesignation();
		return d.contains("psr") || d.contains("stockist");
	}

	private String activateUser(User user) {
		user.setActiveStatus(ActiveStatus.ACTIVE);
		user.setActiveStatusReason(ActiveStatus.ACTIVE.getStatus());
		return "active status enriched";
	}

	private String handleSalesUserActiveStatus(User user) {
		JsonNode attrs = user.getExtendedAttributes();
		if (attrs == null || !attrs.hasNonNull("AUS")) return ACTIVE_STATUS_NOT_ENRICHED;

		String aus = attrs.get("AUS").asText();
		switch (aus.toUpperCase()) {
			case "Y":
				user.setActiveStatus(ActiveStatus.ACTIVE);
				user.setActiveStatusReason("active status activated as per AUS field");
				return "active status enriched as per AUS field";
			case "N":
				user.setActiveStatus(ActiveStatus.INACTIVE);
				user.setActiveStatusReason("active status inactive as per AUS field");
				return "active status enriched as per AUS field";
			default:
				throw new EnrichmentFailException("AUS field should be Y or N for PSR and stockist");
		}
	}

	private void deactivateUnknownDesignation(User user) {
		user.setActiveStatus(ActiveStatus.INACTIVE);
		user.setActiveStatusReason("Unknown designation");
	}

	private Location parseLocationHierarchy(String locationHierarchy, ObjectMapper mapper) {
		if (locationHierarchy == null) return new Location();
		try {
			return mapper.readValue(locationHierarchy, Location.class);
		} catch (Exception e) {
			throw new EnrichmentFailException("Failed to parse locationHierarchy JSON: " + e.getMessage());
		}
	}

	private User getImmediateParentUser(User user) {
		if (user.getImmediateParent() == null || user.getImmediateParent().isEmpty()) return null;
		HierarchyMetadata hmd = user.getImmediateParent().get(0);
		return (hmd != null && hmd.getParent() != null) ? userService.findByLoginId(hmd.getParent()) : null;
	}

	private String enrichCountry(Location location) {
		if (StringUtils.isNotBlank(location.getCountry())) {
			location.setCountry(location.getCountry().trim().toUpperCase());
			return "location enrichment skipped";
		}
		location.setCountry("INDIA");
		return "country enriched successfully";
	}

	private Location parseParentLocation(String parentLocHierarchy, ObjectMapper mapper) throws Exception {
		String trimmed = parentLocHierarchy.trim();
		if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
			return mapper.readValue(parentLocHierarchy, Location.class);
		}
		Location parentLoc = new Location();
		String[] parts = parentLocHierarchy.split(">");
		if (parts.length > 0) parentLoc.setBranch(parts[0]);
		if (parts.length > 1) parentLoc.setDistrict(parts[1]);
		if (parts.length > 2) parentLoc.setCountry(parts[2]);
		return parentLoc;
	}

	private boolean fillMissingLocationFields(Location location, Location parentLoc) {
		boolean enriched = false;
		if (StringUtils.isNullOrBlank(location.getBranch()) && StringUtils.isNotBlank(parentLoc.getBranch())) {
			location.setBranch(parentLoc.getBranch());
			enriched = true;
		}
		if (StringUtils.isNullOrBlank(location.getState()) && StringUtils.isNotBlank(parentLoc.getState())) {
			location.setState(parentLoc.getState());
			enriched = true;
		}
		if (StringUtils.isNullOrBlank(location.getCity()) && StringUtils.isNotBlank(parentLoc.getCity())) {
			location.setCity(parentLoc.getCity());
			enriched = true;
		}
		return enriched;
	}

	private void serializeLocation(User user, Location location, ObjectMapper mapper) {
		try {
			user.setLocationHierarchy(mapper.writeValueAsString(location));
		} catch (Exception e) {
			throw new EnrichmentFailException("Failed to serialize Location back to JSON: " + e.getMessage());
		}
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
}
