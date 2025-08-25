package com.applicate.unnati.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.collections.MapUtils.getString;


public class OutletDetailTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
	private static final Logger logger = LoggerFactory.getLogger(com.applicate.unnati.transformer.OutletDetailTransformer.class);
	private static final String IMMEDIATEPARENT = "immediateParent";
	private static final String ACTIVE = "active";
	private static final String COUNTRY = "India";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String BRANCH = "branch";
	private static final String DISTRICT = "district";

	@Override
	public Map<String, Object> transform(Map<String, Object> responseEnvelope) {
		Map<String, Object> output = new LinkedHashMap<>();
		Map<String, Object> userName = new LinkedHashMap<>();
		Map<String, Object> extendedAttributes = new LinkedHashMap<>();
		String uid = getString(responseEnvelope, "uid");
		output.put("outletCode", uid);
		userName.put("loginId", uid);
		userName.put("userAccountId", uid);
		String type = getString(responseEnvelope, "type");
		output.put("outletCategory", type);
		extendedAttributes.put("loyaltyFlag", type);
		userName.put("extendedAttributes", extendedAttributes);
		String custName = getString(responseEnvelope, "custname");
		output.put("outletName", custName);
		String ownerName = getString(responseEnvelope, "ownername");
		output.put("contactName", ownerName);
		userName.put("name", ownerName);
		String outletLatStr = getString(responseEnvelope, "outletlat");
		if (outletLatStr != null && !outletLatStr.isEmpty()) {
			output.put("latitude", Double.parseDouble(outletLatStr));
		}
		String outletLongStr = getString(responseEnvelope, "outletlong");
		if (outletLongStr != null && !outletLongStr.isEmpty()) {
			output.put("longitude", Double.parseDouble(outletLongStr));
		}
		output.put("outletType", getString(responseEnvelope, "outlettype"));
		output.put("channel", getString(responseEnvelope, "channeltype"));
		output.put("outletClass", getString(responseEnvelope, "loyaltytype"));
		output.put("userName", userName);
		Map<String, Object> location = new LinkedHashMap<>();
		Map<String, Object> locationHierarchy = new LinkedHashMap<>();
		location.put("country", COUNTRY);
		location.put(BRANCH, getString(responseEnvelope, BRANCH));
		location.put(DISTRICT, getString(responseEnvelope, DISTRICT));
		locationHierarchy.put("country", COUNTRY);
		locationHierarchy.put(BRANCH, getString(responseEnvelope, BRANCH));
		locationHierarchy.put(DISTRICT, getString(responseEnvelope, DISTRICT));
		userName.put("locationHierarchy", locationHierarchy);
		output.put("location", location);
		output.put("activeStatus", ACTIVE);
		output.put("activeStatusReason", ACTIVE);
		userName.put("activeStatus", ACTIVE);
		userName.put("activeStatusReason", ACTIVE);
		userName.put("designation", List.of("retailer"));
		userName.put("contactType", "retailer");
		List<Map<String, Object>> supplierMapping = null;
		Object rawSupplierMapping = responseEnvelope.get("suppliermapping");
		try {
			String mappingStr = (String) rawSupplierMapping;
			supplierMapping = OBJECT_MAPPER.readValue(mappingStr, new TypeReference<List<Map<String, Object>>>() {
			});
		} catch (Exception e) {
			logger.error("Failed to parse supplierMapping string", e);
		}
		if (supplierMapping != null && !supplierMapping.isEmpty()) {
			List<Map<String, Object>> userNameParents = new ArrayList<>();
			List<Map<String, Object>> hierarchyParents = new ArrayList<>();
			for (Map<String, Object> supplier : supplierMapping) {
				String wdDest = getString(supplier, "WDDest");
				Map<String, Object> parentMap = new HashMap<>();
				parentMap.put(IMMEDIATEPARENT, wdDest);
				userNameParents.add(parentMap);
				String uid2 = getString(responseEnvelope, "uid");
				Map<String, Object> hierarchyMap = new HashMap<>();
				hierarchyMap.put("hierarchy", uid2 + " > " + wdDest);
				hierarchyParents.add(hierarchyMap);
			}

			userName.put(IMMEDIATEPARENT, userNameParents);
			output.put(IMMEDIATEPARENT, hierarchyParents);
		}
		return output;
	}
}

