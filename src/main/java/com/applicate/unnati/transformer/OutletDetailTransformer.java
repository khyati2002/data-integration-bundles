package com.applicate.unnati.transformer;

import com.mysql.cj.util.StringUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.collections.MapUtils.getString;


public class OutletDetailTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
	private static final Logger logger = LoggerFactory.getLogger(com.applicate.unnati.transformer.OutletDetailTransformer.class);
	private static final String IMMEDIATEPARENT = "immediateParent";
	private static final String ACTIVE = "active";

	@Override
	public Map<String, Object> transform(Map<String, Object> responseEnvelope) {

		logger.info("=== Starting OutletDetailTransformer ===");
		logger.info("Raw responseEnvelope: {}", responseEnvelope);

		if (responseEnvelope == null || responseEnvelope.isEmpty()) {
			logger.warn("Response envelope is null or empty!");
			return new LinkedHashMap<>();
		}

		// Step 1: Log raw fields (matching case from your example)
		logger.info("uid: {}", responseEnvelope.get("uid"));
		logger.info("type: {}", responseEnvelope.get("type"));
		logger.info("custname: {}", responseEnvelope.get("custname"));
		logger.info("ownername: {}", responseEnvelope.get("ownername"));
		logger.info("outletlat: {}", responseEnvelope.get("outletlat"));
		logger.info("outletlong: {}", responseEnvelope.get("outletlong"));
		logger.info("outlettype: {}", responseEnvelope.get("outlettype"));
		logger.info("channeltype: {}", responseEnvelope.get("channeltype"));
		logger.info("loyaltytype: {}", responseEnvelope.get("loyaltytype"));
		logger.info("branch: {}", responseEnvelope.get("branch"));
		logger.info("district: {}", responseEnvelope.get("district"));
		logger.info("suppliermapping: {}", responseEnvelope.get("suppliermapping"));

		Map<String, Object> output = new LinkedHashMap<>();

		Map<String, Object> userName = new LinkedHashMap<>();
		Map<String, Object> extendedAttributes = new LinkedHashMap<>();

		String uid = getString(responseEnvelope, "UID");
		logger.info("Parsed uid: {}", uid);
		output.put("outletCode", uid);
		logger.info("Set outletCode: {}", output.get("outletCode"));
		userName.put("loginId", uid);
		userName.put("userAccountId", uid);
		logger.info("Set user loginId and userAccountId: {}", userName);


		String type = getString(responseEnvelope, "TYPE");
		logger.info("Parsed type: {}", type);
		output.put("outletCategory", type);
		extendedAttributes.put("loyaltyFlag", type);
		userName.put("extendedAttributes", extendedAttributes);
		logger.info("Set outletCategory and extendedAttributes: {}", extendedAttributes);

		String custName = getString(responseEnvelope, "CUSTName");
		logger.info("Parsed custname: {}", custName);
		output.put("outletName", custName);

		String ownerName = getString(responseEnvelope, "OwnerName");
		logger.info("Parsed ownername: {}", ownerName);
		output.put("contactName", ownerName);
		userName.put("name", ownerName);

		String outletLatStr = getString(responseEnvelope, "OutletLat");
		logger.info("Parsed outletlat: {}", outletLatStr);
		if (!StringUtils.isNullOrEmpty(outletLatStr)) {
			output.put("latitude", Double.parseDouble(outletLatStr));
			logger.info("Set latitude: {}", output.get("latitude"));
		}
		String outletLongStr = getString(responseEnvelope, "OutletLong");
		logger.info("Parsed outletlong: {}", outletLongStr);
		if (!StringUtils.isNullOrEmpty(outletLongStr)) {
			output.put("longitude", Double.parseDouble(outletLongStr));
			logger.info("Set longitude: {}", output.get("longitude"));
		}

		output.put("outletType", getString(responseEnvelope, "OutletType"));
		output.put("channel", getString(responseEnvelope, "ChannelType"));
		output.put("outletClass", getString(responseEnvelope, "LoyaltyType"));
		logger.info("Set outletType, channel, outletClass: {}, {}, {}", output.get("outletType"), output.get("channel"), output.get("outletClass"));

		output.put("userName", userName);

		Map<String, Object> location = new LinkedHashMap<>();
		Map<String, Object> locationHierarchy = new LinkedHashMap<>();

		location.put("country", "India");
		location.put("branch", getString(responseEnvelope, "Branch"));
		location.put("district", getString(responseEnvelope, "DISTRICT"));

		locationHierarchy.put("country", "India");
		locationHierarchy.put("branch", getString(responseEnvelope, "Branch"));
		locationHierarchy.put("district", getString(responseEnvelope, "DISTRICT"));


		userName.put("locationHierarchy", locationHierarchy);
		output.put("location", location);
		logger.info("Set location and locationHierarchy: {}, {}", location, locationHierarchy);


		output.put("activeStatus", ACTIVE);
		output.put("activeStatusReason", ACTIVE);

		userName.put("activeStatus", ACTIVE);
		userName.put("activeStatusReason", ACTIVE);
		userName.put("designation", List.of("retailer"));
		userName.put("contactType", "retailer");
		logger.info("Set active status fields for output and userName");


		List<Map<String, Object>> supplierMapping = null;
		Object rawSupplierMapping = responseEnvelope.get("supplierMapping");
		logger.info("Raw supplierMapping: {}", rawSupplierMapping);


		try {
			String mappingStr = (String) rawSupplierMapping;
			ObjectMapper mapper = new ObjectMapper();
			supplierMapping = mapper.readValue(mappingStr, new TypeReference<List<Map<String, Object>>>() {
			});
			logger.info("Parsed supplierMapping: {}", supplierMapping);
		} catch (Exception e) {
			logger.error("Failed to parse supplierMapping string", e);
		}

		if (supplierMapping != null && !supplierMapping.isEmpty()) {
			List<Map<String, Object>> userNameParents = supplierMapping.stream().map(supplier -> {
				String wdDest = getString(supplier, "WDDest");
				Map<String, Object> map = new HashMap<>();
				map.put(IMMEDIATEPARENT, wdDest);
				return map;
			}).collect(Collectors.toList());
			logger.info("userNameParents: {}", userNameParents);

			List<Map<String, Object>> hierarchyParents = supplierMapping.stream().map(supplier -> {
				String uid2 = getString(responseEnvelope, "UID");
				String wdDest = getString(supplier, "WDDest");
				Map<String, Object> map = new HashMap<>();
				map.put("hierarchy", uid2 + " > " + wdDest);
				return map;
			}).collect(Collectors.toList());
			logger.info("hierarchyParents: {}", hierarchyParents);

			userName.put(IMMEDIATEPARENT, userNameParents);
			output.put(IMMEDIATEPARENT, hierarchyParents);

		}
		logger.info("Final output: {}", output);
		logger.info("OutletName: {}", output.get("outletName"));
		logger.info("Channel: {}", output.get("channel"));
		logger.info("OutletCode: {}", output.get("outletCode"));
		logger.info("LoyaltyType (outletClass): {}", output.get("outletClass"));
		logger.info("District: {}", ((Map<String, Object>) output.get("location")).get("district"));
		logger.info("Branch: {}", ((Map<String, Object>) output.get("location")).get("branch"));
		return output;
	}
}

