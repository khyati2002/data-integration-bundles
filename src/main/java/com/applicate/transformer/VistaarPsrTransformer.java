package com.applicate.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.*;

/**
 * Transformer for PSR user data from Vistaar integration.
 * Converts flat PSR records into a structured user representation.
 */
public class VistaarPsrTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

	private static final String PSR_NAME = "PSRName";
	private static final String PSR_CRM_ID = "PSRCRMID";
	private static final String USERNAME = "userName";
	private static final String BRANCH = "Branch";
	private static final String DISTRICT = "District";
	private static final String AUS = "AUS";
	private static final String DS_TYPE = "DSType";
	private static final String SOURCE_KEY = "source_key";
	private static final String IMMEDIATE_PARENT = "immediateParent";
	private static final String ACTIVE = "active";
	private static final String COUNTRY = "India";

	@Override
	public Map<String, Object> transform(Map<String, Object> input) {
		Map<String, Object> output = new LinkedHashMap<>();

		output.put("loginId", input.get(PSR_CRM_ID));
		output.put("userAccountId", input.get(PSR_CRM_ID));
		output.put("mobile", input.get(USERNAME));
		output.put("activeStatus", ACTIVE);
		output.put("name", safeLower((String) input.get(PSR_NAME)));
		output.put("designation", getDesignationObj());

		List<Map<String, Object>> immediateParentObj = getImmediateParentObj(input);
		output.put(IMMEDIATE_PARENT, immediateParentObj);

		Map<String, Object> locationHierarchy = getLocationHierarchyObj(input);
		output.put("locationHierarchy", locationHierarchy);

		Map<String, Object> extendedAttributes = getExtendedAttributesObj(input);
		output.put("extendedAttributes", extendedAttributes);

		return output;
	}

	/**
	 * Builds the locationHierarchy node.
	 */
	private Map<String, Object> getLocationHierarchyObj(Map<String, Object> input) {
		Map<String, Object> location = new LinkedHashMap<>();
		location.put("country", COUNTRY);
		location.put("branch", input.get(BRANCH));
		location.put("district", input.get(DISTRICT));
		return location;
	}

	/**
	 * Builds the immediateParent object.
	 */
	private List<Map<String, Object>> getImmediateParentObj(Map<String, Object> input) {
		List<Map<String, Object>> parents = new ArrayList<>();
		Map<String, Object> parentObj = new LinkedHashMap<>();
		parentObj.put(IMMEDIATE_PARENT, input.get(IMMEDIATE_PARENT));
		parents.add(parentObj);
		return parents;
	}

	/**
	 * Returns the PSR designation info.
	 */
	private List<String> getDesignationObj() {
		return Collections.singletonList("PSR");
	}

	/**
	 * Builds the extendedAttributes node.
	 */
	private Map<String, Object> getExtendedAttributesObj(Map<String, Object> input) {
		Map<String, Object> extAttr = new LinkedHashMap<>();
		extAttr.put(SOURCE_KEY, input.get(SOURCE_KEY));
		extAttr.put(DS_TYPE, input.get(DS_TYPE));
		extAttr.put(PSR_CRM_ID, input.get(PSR_CRM_ID));
		extAttr.put(AUS, input.get(AUS));
		extAttr.put(BRANCH, input.get(BRANCH));
		extAttr.put(DISTRICT, input.get(DISTRICT));
		return extAttr;
	}

	private String safeLower(String val) {
		return val == null ? null : val.toLowerCase(Locale.ROOT);
	}
}
