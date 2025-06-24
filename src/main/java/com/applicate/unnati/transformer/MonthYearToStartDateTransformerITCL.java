package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.utils.CollectionUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.TransformerInfo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.jooq.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MonthYearToStartDateTransformerITCL extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
	private static final String MONTH_TAG = "Month";
	private static final String YEAR_TAG = "Year";
	private static final String DIGIT_REGEX_MONTH_YEAR = "^[0-9]*$";
	private static final String DIGIT_REGEX_TAG = "^[0-9.]*$";
	private static final String OPENING_POINT_TAG = "openingPoints";
	private static final String CLOSING_POINT_TAG = "closingPoints";
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public Map<String, Object> transform(Map<String, Object> jsonobj) {
		TransformerInfo transformerInfo = this.getTransformerInfo();
		JSON codeNode = transformerInfo.getCode();
		try {
			Object spec = JsonUtils.jsonToObject(String.valueOf(codeNode));
			Chainr chainr = Chainr.fromSpec(spec);
			Object transformedOutput = chainr.transform(jsonobj);
			Map<String, Object> result = JSONUtils.getObjectMapper().readValue(
					JsonUtils.toPrettyJsonString(transformedOutput), new TypeReference<HashMap<String, Object>>() {
					});
			if (!CollectionUtils.isEmptyOrNull(result)) {
				result = transformData(result);
				String openingPoints = result.get(OPENING_POINT_TAG) != null ? result.get(OPENING_POINT_TAG) + "" : "";
				String closingPoints = result.get(CLOSING_POINT_TAG) != null ? result.get(CLOSING_POINT_TAG) + "" : "";

				String startDate = result.get("startDate").toString();
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date parse = dateFormat.parse(startDate);
				String startDateInMilliSec = Long.toString(parse.getTime());
				String programNumber = result.get("feature") + ":" + startDateInMilliSec + ":"
						+ result.get("loginId");
				result.put("programNumber", programNumber);
				return result;

			}
		} catch (Exception ex) {
			logger.error("MonthYearToStartDateTransformerITCL Exception", ex);
		}
		return null;
	}

	private Map<String, Object> transformData(Map<String, Object> result) throws ParseException {
		String month = result.get(MONTH_TAG) != null ? result.get(MONTH_TAG) + "" : "";
		String year = result.get(YEAR_TAG) != null ? result.get(YEAR_TAG) + "" : "";
		result.remove(MONTH_TAG);
		result.remove(YEAR_TAG);
		if (!month.matches(DIGIT_REGEX_MONTH_YEAR) || !year.matches(DIGIT_REGEX_MONTH_YEAR)) {
			month = "14";
			year = "20000";
		}
		Calendar defaultDate = Calendar.getInstance();
		defaultDate.setTime(new Date());

		Calendar currDate = Calendar.getInstance();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

		if (month.equals("") || Integer.parseInt(month) > 12) {
			month = (defaultDate.get(Calendar.MONTH) + 1) + "";
		}

		if (year.equals("") || year.length() > 4) {
			year = defaultDate.get(Calendar.YEAR) + "";
		}

		int monthVal = Integer.parseInt(month);
		String monthStr = monthVal + "";
		if (monthVal < 10) {
			monthStr = "0" + monthVal;
		}
		String monthYear = year + "-" + monthStr;
		String dateString = monthYear + "-01 00:00:00";

		result.put("startDate", dateString);

		currDate.setTime(format.parse(dateString));
		int endDate = currDate.getActualMaximum(Calendar.DATE);
		result.put("endDate", monthYear + "-" + endDate + " 23:59:59");

		return result;
	}
}
