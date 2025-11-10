package com.applicate.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VistaarStockistTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

	public static final String ACTIVE_STATUS = "ActiveStatus";
	public static final String STOCKIST_NAME = "stockistName";
	public static final String AUS = "AUS";
	public static final String KYC = "KYC";
	public static final String BEAT_ID = "BeatID";
	public static final String BEAT = "Beat";
	public static final String STOCKIST_BAND = "StockistBand";
	private static final String PSRCRMID = "PSRCRMID";
	private static final String DISTRICT = "District";
	private static final String BRANCH = "Branch";
	private static final String COLOUR = "COLOUR";
	private static final String WD_DEST = "WDDest";
	private static final String SUPPLIER_MAPPING = "supplierMapping";
	private static final String IMMEDIATE_PARENT = "immediateParent";
	private static final String STOCKIST = "STOCKIST";
	private static final String UID = "UID";

	public Map<String, Object> transform(Map<String, Object> input) {
		Map<String, Object> outlet = new LinkedHashMap<>();

		String stockistName = (String) input.get(STOCKIST_NAME);
		outlet.put("contactName", stockistName);
		outlet.put("channel", STOCKIST);
		outlet.put("outletType", STOCKIST);
		outlet.put("outletName", stockistName);
		outlet.put("outletCategory", STOCKIST);
		outlet.put("activeStatus", input.get(ACTIVE_STATUS));
		outlet.put("beatName", input.get(BEAT_ID));
		outlet.put("beat", input.get(BEAT));

		List<Map<String, Object>> outletImmediateParentObj = getOutletImmediateParentObj(input);
		outlet.put(IMMEDIATE_PARENT, outletImmediateParentObj);

		Map<String, Object> userName = getUserObj(input);
		outlet.put("userName", userName);

		Map<String, Object> extAttr = new LinkedHashMap<>();
		extAttr.put("source_key", "integration");
		extAttr.put(AUS, input.get(AUS));
		extAttr.put(KYC, input.get(KYC));
		extAttr.put(UID, input.get(UID));
		extAttr.put(COLOUR, input.get(COLOUR));
		extAttr.put("supplier", input.get(WD_DEST));
		extAttr.put(PSRCRMID, input.get(PSRCRMID));
		extAttr.put("stockistBadge", input.get(STOCKIST_BAND));
		extAttr.put(SUPPLIER_MAPPING, input.get(SUPPLIER_MAPPING));
		outlet.put("extendedAttributes", extAttr);

		Map<String, Object> location = getLocationHierarchyObj(input);
		outlet.put("location", location);

		outlet.put("outletCode", input.get(UID));
		return outlet;
	}

	private Map<String, Object> getUserObj(Map<String, Object> input) {
		Map<String, Object> userName = new LinkedHashMap<>();
		userName.put("loginId", input.get(UID));
		userName.put("userAccountId", input.get(UID));
		userName.put("mobile", null);
		userName.put("activeStatus", input.get(ACTIVE_STATUS));
		userName.put("name", input.get(STOCKIST_NAME));

		// Set designation
		userName.put("designation", Collections.singletonList(STOCKIST));

		// Set extendedAttributes
		Map<String, Object> userExtAttr = new LinkedHashMap<>();
		userExtAttr.put("source_key", "integration");
		userExtAttr.put(AUS, input.get(AUS));
		userExtAttr.put(KYC, input.get(KYC));
		userExtAttr.put(UID, input.get(UID));
		userExtAttr.put(BRANCH, input.get(BRANCH));
		userExtAttr.put(COLOUR, input.get(COLOUR));
		userExtAttr.put("supplier", input.get(WD_DEST));
		userExtAttr.put(DISTRICT, input.get(DISTRICT));
		userExtAttr.put(PSRCRMID, input.get(PSRCRMID));
		userName.put("extendedAttributes", userExtAttr);

		// Set immediateParent
		List<Map<String, Object>> immediateParentObj = getUserImmediateParentObj(input);
		userName.put(IMMEDIATE_PARENT, immediateParentObj);

		// Set locationHierarchy
		Map<String, Object> locationHierarchy = getLocationHierarchyObj(input);
		userName.put("locationHierarchy", locationHierarchy);

		return userName;
	}

	private Map<String, Object> getLocationHierarchyObj(Map<String, Object> input) {
		Map<String, Object> locationHierarchy = new LinkedHashMap<>();
		locationHierarchy.put("country", "India");
		locationHierarchy.put("branch", input.get(BRANCH));
		locationHierarchy.put("district", input.get(DISTRICT));
		return locationHierarchy;
	}

	private List<Map<String, Object>> getUserImmediateParentObj(Map<String, Object> input) {
		String psrcrmid = (String) input.get(PSRCRMID);
		List<Map<String, Object>> userHierarchy = new ArrayList<>();
		if (psrcrmid.equalsIgnoreCase("NA")) {
			ArrayList<Map<String, Object>> supplierMapping = (ArrayList<Map<String, Object>>) input.get(SUPPLIER_MAPPING);
			for (Map<String, Object> s : supplierMapping) {
				Map<String, Object> m = new HashMap<>();
				m.put(IMMEDIATE_PARENT, s.get(WD_DEST));
				userHierarchy.add(m);
			}
		} else {
			Map<String, Object> m = new HashMap<>();
			m.put(IMMEDIATE_PARENT, psrcrmid);
			userHierarchy.add(m);
		}
		return userHierarchy;
	}

	private List<Map<String, Object>> getOutletImmediateParentObj(Map<String, Object> input) {
		List<Map<String, Object>> outletHierarchy = new ArrayList<>();
		String psrcrmid = (String) input.get(PSRCRMID);
		ArrayList<Map<String, Object>> supplierMapping = (ArrayList<Map<String, Object>>) input.get(SUPPLIER_MAPPING);
		for (Map<String, Object> s : supplierMapping) {
			Map<String, Object> m = new HashMap<>();
			String supplier = s.get(WD_DEST).toString();
			String uid = s.get(UID).toString();
			String hierarchy;
			if (!psrcrmid.equalsIgnoreCase("NA")) {
				hierarchy = uid + " > " + psrcrmid + " > " + supplier;
			} else {
				hierarchy = uid + " > " + supplier;
			}
			m.put("hierarchy", hierarchy);
			m.put(IMMEDIATE_PARENT, uid);
			outletHierarchy.add(m);
		}
		return outletHierarchy;
	}

}
