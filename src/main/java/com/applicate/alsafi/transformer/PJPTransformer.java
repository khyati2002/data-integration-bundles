package com.applicate.alsafi.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PJPTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> input) {

        int frequency = getInteger(input, "Frequency", 7);
        String anchorDateStr = getString(input, "StartingWeekAnchorDate");
        ObjectNode extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
        Set<String> weekDayVisits = input.keySet().stream()
                .filter(key -> key.startsWith("Visit") && isTrue(input.get(key)))
                .collect(Collectors.toSet());
        extendedAttributes.put("visits", weekDayVisits.stream().collect(Collectors.joining(",")));

        Set<DayOfWeek> visitDays = weekDayVisits.stream()
                .map(k -> k.replace("Visit", "").toUpperCase())
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());

        List<String> visitDates = computeVisitDates(anchorDateStr, visitDays, frequency);

        List<Map<String,Object>> pjpMapList = new ArrayList<>();

        visitDates.forEach(date -> pjpMapList.add(getPjp(input, )));

        return pjpMapList;
    }


    private Map<String, Object> getPjp(Map<String, Object> input, String date){
        Map<String, Object> pjpMap = new HashMap<>();

        pjpMap.put("outletcode", getString(input, "CustomerId"));
        pjpMap.put("id", getString(input, "CustomerId")+date);

        String activeStatus = "inactive";
        if ("1".equals(getString(input, "Active")) || "true".equalsIgnoreCase(getString(input, "Active"))) {
            activeStatus = "active";
        }
        pjpMap.put("activeStatus", activeStatus);
        pjpMap.put("beat", getString(input, "RouteId"));
        pjpMap.put("loginid", getString(input, "RouteId"));
        pjpMap.put("pjpDate", date);
        pjpMap.put("month", LocalDate.now().getMonthValue());
        pjpMap.put("year", LocalDate.now().getYear());
    }
    private List<String> computeVisitDates(String anchorDateStr,
                                           Set<DayOfWeek> visitDays,
                                           int frequencyDays) {

        List<String> result = new ArrayList<>();
        if (anchorDateStr == null || visitDays.isEmpty()) return result;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDate anchor = LocalDate.parse(anchorDateStr, formatter);

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        for (DayOfWeek day : visitDays) {
            LocalDate firstOccurrence = findFirstMatchingDate(anchor, day);
            while (firstOccurrence.isBefore(monthStart)) {
                firstOccurrence = firstOccurrence.plusDays(frequencyDays);
            }
            LocalDate dt = firstOccurrence;
            while (!dt.isAfter(monthEnd)) {
                result.add(dt.toString());
                dt = dt.plusDays(frequencyDays);
            }
        }

        Collections.sort(result);
        return result;
    }

    private LocalDate findFirstMatchingDate(LocalDate anchor, DayOfWeek targetDay) {
        LocalDate date = anchor;
        while (date.getDayOfWeek() != targetDay) {
            date = date.plusDays(1);
        }
        return date;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString().trim() : null;
    }

    private int getInteger(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean isTrue(Object val) {
        if (val == null) return false;
        String s = val.toString().trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s);
    }
}
