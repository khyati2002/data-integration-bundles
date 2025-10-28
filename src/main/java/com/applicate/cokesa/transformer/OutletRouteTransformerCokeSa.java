package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutletRouteTransformerCokeSa extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        return createMonthlyResponses(inputMap); // generate multiple pjp records for current month
    }

    private List<Map<String, Object>> createMonthlyResponses(Map<String, Object> inputMap) {
        List<Map<String, Object>> result = new ArrayList<>();

        // Extract inputs
        String firstVisitDateStr = convertCustomStringToDate(inputMap.get("OM06_CALDAT").toString(), false);
        int frequency = Integer.parseInt(inputMap.get("OM06_CALCYC").toString());
        int weekday = Integer.parseInt(inputMap.get("OM06_WEKDAY").toString());

        LocalDate firstVisitDate = LocalDate.parse(firstVisitDateStr.substring(0, 10), DATE_FORMATTER);
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        // Move to first relevant PJP date in current month
        LocalDate date = firstVisitDate;
        while (date.isBefore(startOfMonth)) {
            date = date.plusDays(frequency);
        }

        // Align to target weekday
        DayOfWeek desiredDay = DayOfWeek.of(weekday == 7 ? 7 : weekday);
        while (date.getDayOfWeek() != desiredDay) {
            date = date.plusDays(1);
        }

        // Generate all visits for this month
        while (!date.isAfter(endOfMonth)) {
            String pjpDate = date.format(DATE_FORMATTER) + " 00:00:00";
            Map<String, Object> record = createResponse(inputMap, firstVisitDateStr, frequency, pjpDate);
            result.add(record);
            date = date.plusDays(frequency);
        }

        return result;
    }

    private Map<String, Object> createResponse(Map<String, Object> inputMap, String firstVisitDate, int frequency, String pjpDate) {
        Map<String, Object> response = new HashMap<>();

        response.put("outletCode", sanitizeOutletCode(inputMap.get("OM06_OUTNUM")));
        response.put("beat", inputMap.get("OM06_CALRTE").toString());
        response.put("sequence", inputMap.get("OM06_CALSEQ").toString());
        response.put("pjpDate", pjpDate);

        // Extract year and month from pjpDate
        String year = pjpDate.substring(0, 4);
        String month = pjpDate.substring(5, 7);
        response.put("year", year);
        response.put("month", month);

        response.put("extendedAttributes", createExtended(inputMap, firstVisitDate, frequency));

        // Optional field (empty array)
        ArrayNode emptyArray = mapper.createArrayNode();
        response.put("dayAndFrequency", emptyArray);

        return response;
    }

    private JsonNode createExtended(Map<String, Object> inputMap, String firstVisitDate, int frequency) {
        Map<String, Object> extended = new HashMap<>();
        extended.put("routeType", inputMap.get("OM06_CALTYP"));
        extended.put("depo", inputMap.get("OM06_CALLOC"));
        extended.put("firstVisitDate", firstVisitDate);
        extended.put("frequency", frequency);
        return JSONUtils.toJsonNode(extended);
    }

    public static String convertCustomStringToDate(String customDateStr, boolean isEndOfDay) {
        if (NullUtils.isNull(customDateStr) || customDateStr.length() != 7) {
            throw new IllegalArgumentException("Invalid input format. Expected 7 characters.");
        }
        int centuryPart = Integer.parseInt(customDateStr.substring(0, 3));
        int year = 1900 + centuryPart;
        String month = customDateStr.substring(3, 5);
        String day = customDateStr.substring(5, 7);
        String time = isEndOfDay ? "23:59:59" : "00:00:00";
        return year + "-" + month + "-" + day + " " + time;
    }

    private String sanitizeOutletCode(Object outletCodeObj) {
        if (outletCodeObj instanceof Double) {
            // Convert to long first to remove decimal
            return String.valueOf(((Double) outletCodeObj).longValue());
        } else if (outletCodeObj instanceof Integer || outletCodeObj instanceof Long) {
            return String.valueOf(outletCodeObj);
        } else {
            return outletCodeObj.toString().replaceAll("\\.0$", ""); // fallback cleanup
        }
    }

}
