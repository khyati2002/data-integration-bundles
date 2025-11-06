package com.applicate.transformer;

import java.util.*;
import java.util.stream.Collectors;

import com.applicate.services.channelkart.services.*;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.utils.TimerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * transformer for outlet details to add whole location object in response
 */
public class VistaarOutletLocationTransformer extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {

	private static final String LOCATION = "location";
	public static final String SUPPLIER = "supplier";

	private final UserService userService = ServiceLocator.lookup(UserService.class);
	private final SupplierInfoService supplierInfoService = ServiceLocator.lookup(SupplierInfoService.class);
	private final LocationService locationService = ServiceLocator.lookup(LocationService.class);

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@SuppressWarnings("unchecked")
	@Override
	public Object transform(Map<String, Object> s) {
		if (s == null) {
			return null;
		}

		List<Map<String, Object>> outletList = (List<Map<String, Object>>) s.get("features");
		if (outletList == null || outletList.isEmpty()) {
			return s;
		}

		// collect unique hierarchies from outlets (guard nulls)
		Set<String> hierarchies = outletList.stream()
				.filter(outlet -> outlet != null && outlet.containsKey(LOCATION) && outlet.get(LOCATION) != null)
				.map(outlet -> outlet.get(LOCATION).toString())
				.collect(Collectors.toSet());

		// fetch locations in batch
		List<Location> locations = TimerUtils.withTime("Time taken to findByLocationHierarchy",
				() -> locationService.findByLocationHierarchy(hierarchies));

		Map<String, Location> collect = locations == null
				? Collections.emptyMap()
				: locations.stream().collect(Collectors.toMap(Location::getLocationHierarchy, location -> location));

		// cache for loginId -> supplierName
		Map<String, String> suppliersCache = new HashMap<>();

		outletList.forEach(outlet -> {
			// set Location object (fallback to empty Location)
			Object locKey = outlet.get(LOCATION);
			String locHierarchy = locKey == null ? null : locKey.toString();
			Location loc = collect.getOrDefault(locHierarchy, new Location());
			outlet.put(LOCATION, loc);

			// attach SupplierInfo - timed
			outlet.put("SupplierInfo", TimerUtils.withTime("Time taken to getSupplierInfo",
					() -> getSupplierInfo(outlet, suppliersCache)));
		});

		s.put("features", outletList);
		return s;
	}

	private List<String> getSupplierFromOutletCode(String outletCode) {
		if (outletCode == null) return Collections.emptyList();
		return supplierInfoService.findSuppliersUsingOutletCode(outletCode);
	}

	private List<Map<String, String>> getSupplierInfo(Map<String, Object> outlet, Map<String, String> allSuppliersList) {
		Set<String> currentSuppliersSet = extractSupplierSet(outlet);
		return getSupplierMap(currentSuppliersSet, allSuppliersList);
	}

	private List<Map<String, String>> getSupplierMap(Set<String> currentSuppliersSet, Map<String, String> allSuppliersList) {
		if (currentSuppliersSet == null || currentSuppliersSet.isEmpty()) {
			return Collections.emptyList();
		}

		// find missing loginIds from cache
		Set<String> missing = currentSuppliersSet.stream()
				.filter(id -> !allSuppliersList.containsKey(id))
				.collect(Collectors.toSet());

		if (!missing.isEmpty()) {
			// batch fetch users
			List<User> suppliersList = userService.findByLoginIdIn(new ArrayList<>(missing));
			if (suppliersList != null) {
				suppliersList.forEach(u -> {
					if (u != null && u.getLoginId() != null) {
						allSuppliersList.put(u.getLoginId(), u.getName());
					}
				});
			}
		}

		return currentSuppliersSet.stream().map(entry -> {
					Map<String, String> map = new HashMap<>();
					map.put("supplierCode", entry);
					map.put("supplierName", allSuppliersList.get(entry)); // may be null if not found
					return map;
				})
				.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private Set<String> extractSupplierSet(Map<String, Object> outlet) {
		Set<String> suppliersSet = new HashSet<>();
		if (outlet == null) return suppliersSet;

		Object oc = outlet.get("outletCode");
		String outletCode = oc == null ? null : oc.toString();

		// top-level supplier key (expected as Collection<String>)
		if (outlet.containsKey(SUPPLIER)) {
			Object supObj = outlet.get(SUPPLIER);
			if (supObj instanceof Collection) {
				try {
					suppliersSet.addAll((Collection<String>) supObj);
				} catch (ClassCastException ignored) {}
			}
		}

		// extendedAttributes is expected as a Map
		Object ext = outlet.get("extendedAttributes");
		if (ext instanceof Map) {
			Map<String, Object> extendedAttributes = (Map<String, Object>) ext;

			if (extendedAttributes.containsKey(SUPPLIER)) {
				Object sup = extendedAttributes.get(SUPPLIER);
				if (sup instanceof Collection) {
					try {
						suppliersSet.addAll((Collection<String>) sup);
					} catch (ClassCastException ignored) {}
				} else if (sup != null) {
					suppliersSet.add(sup.toString());
				}
			}

			if (extendedAttributes.containsKey("parent-0-1-id")) {
				suppliersSet.add(String.valueOf(extendedAttributes.get("parent-0-1-id")));
			}
			if (extendedAttributes.containsKey("parent-1-1-id")) {
				suppliersSet.add(String.valueOf(extendedAttributes.get("parent-1-1-id")));
			}
		}

		// fallback: fetch using outletCode via SupplierInfoService
		if (suppliersSet.isEmpty() && outletCode != null) {
			List<String> suppliers = TimerUtils.withTime("Time taken to getSupplierFromOutletCode",
					() -> getSupplierFromOutletCode(outletCode));
			if (suppliers != null) suppliersSet.addAll(suppliers);
		}

		if (suppliersSet.isEmpty()) {
			log.debug("No Supplier found for outlet code {} ", outletCode);
		} else {
			log.debug("Supplier for outlet code {} are {}", outletCode, suppliersSet);
		}
		return suppliersSet;
	}
}
