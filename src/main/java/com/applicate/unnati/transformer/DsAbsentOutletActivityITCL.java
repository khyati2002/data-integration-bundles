package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.transformers.impl.JoltTransformer;
import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class DsAbsentOutletActivityITCL extends JoltTransformer {

	private static String getString(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : "";
	}

	private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
		if (value != null) {
			target.put(key, value);
		}
	}

	@Override
	public Object transform(Map<String, Object> input) {
		Map<String, Object> result = (Map<String, Object>) super.transform(input);

		if (input.get("UID") != null) {
			result.put("outletCode", input.get("UID"));
		}

		String uid = getString(input, "UID");
		String wd = getString(input, "WDDEST");
		String ds = getString(input, "DSID");

		String currentDate = LocalDate.now(ZoneId.of(DateToClientTimeZoneStringConverter.getTimeZone())).toString();
		String referenceNumber = String.join(":", uid, wd, ds, currentDate);
		result.put("referenceNumber", referenceNumber);
		result.put("loginId", wd);
		result.put("activity", "attendance");
		Map<String, Object> extendedAttributes = new HashMap<>();
		putIfNotNull(extendedAttributes, "SIFYID", input.get("SIFYID"));
		putIfNotNull(extendedAttributes, "RCSID", input.get("RCSID"));
		putIfNotNull(extendedAttributes, "WDNAME", input.get("WDNAME"));
		putIfNotNull(extendedAttributes, "DSID", input.get("DSID"));
		putIfNotNull(extendedAttributes, "DSNAME", input.get("DSNAME"));
		putIfNotNull(extendedAttributes, "DSType", input.get("DSType"));
		result.put("extendedAttributes", extendedAttributes);

		return result;
	}
}
