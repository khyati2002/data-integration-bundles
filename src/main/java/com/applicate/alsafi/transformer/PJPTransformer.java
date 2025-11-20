package com.applicate.alsafi.transformer;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

public class PJPTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final String FREQUENCY = "frequency";

    @Override
    public Map<String, Object> transform(Map<String, Object> input) {
        Map<String, Object> pjpMap = new HashMap<>();

        pjpMap.put("outletcode",getString(input, "CustomerId"));

        String activeStatus = "inactive";
        if ("1".equals(getString(input, "Active")) || "true".equalsIgnoreCase(getString(input, "Active"))) {
            activeStatus = "active";
        }
        pjpMap.put("activeStatus", activeStatus);
        pjpMap.put("beat", getString(input, "RouteId"));
        pjpMap.put("loginid", getString(input, "RouteId"));

        int frequency = getInteger(input, "Frequency", 7);
        ArrayNode dayAndFrequency = JSONUtils.getObjectMapper().createArrayNode();
        Map<String, Object> extendedAttributes = new HashMap<>();
        extendedAttributes.put("sequence", getString(input, "Sequence"));
        extendedAttributes.put("startingWeekAnchorDate", getString(input, "StartingWeekAnchorDate"));
        extendedAttributes.put(FREQUENCY, frequency);
        extendedAttributes.put("scheduleTypeId", getString(input, "ScheduleTypeId"));

        Set<String> weekDayVisits = input.keySet().stream()
                .filter(key -> key.startsWith("Visit") && isTrue(input.get(key)))
                .collect(Collectors.toSet());

        Set<String> weekDayNameSet = weekDayVisits.stream()
                .map(key -> key.replace("Visit", "").toLowerCase())
                .collect(Collectors.toSet());

        extendedAttributes.put("dayToVisit", String.join(",", weekDayVisits));
        pjpMap.put("extendedAttributes", extendedAttributes);
        try {
            String dFString = JSONUtils.getObjectMapper().writeValueAsString(dayAndFrequency);
            pjpMap.put("dayAndFrequency", dFString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        populateDayAndFrequency(weekDayNameSet, dayAndFrequency);
        pjpMap.put("month", LocalDate.now().getMonth().toString());
        pjpMap.put("year", LocalDate.now().getYear());

        return pjpMap;
    }

    private void populateDayAndFrequency(Set<String> weekDayNameSet, ArrayNode dayAndFrequencyList) {
        weekDayNameSet.forEach(weekDay -> {
            int week = 1;
            while (week <= 6) {
                ObjectNode dayFrequency = JSONUtils.getObjectMapper().createObjectNode();
                dayFrequency.put("day", weekDay);
                dayFrequency.put(FREQUENCY, week);
                dayAndFrequencyList.add(dayFrequency);
                week = week + 1;
            }
        });
    }

    private void populateDayAndFrequency14(Set<String> weekDayNameSet, List<Map<String, Object>> dayAndFrequencyList,
                                           String startAnchorDate, int frequency) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate currDate = LocalDate.now();

        weekDayNameSet.forEach(dayOfWeek -> {
            LocalDate firstDayOfMonth = currDate.withDayOfMonth(1);
            LocalDate anchorDate = LocalDate.parse(startAnchorDate, formatter);

            while (!dayOfWeek.equalsIgnoreCase(anchorDate.getDayOfWeek().toString())) {
                anchorDate = anchorDate.plusDays(1);
            }

            long period = ChronoUnit.DAYS.between(anchorDate, firstDayOfMonth);
            long daysToAdd = period % frequency;
            if (daysToAdd != 0) {
                daysToAdd = frequency - daysToAdd;
            }

            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int weekNumberOfFirstDay = firstDayOfMonth.get(weekFields.weekOfWeekBasedYear());
            LocalDate firstVisit = firstDayOfMonth.plusDays(daysToAdd);
            int weekNumberForFirstVisit = firstVisit.get(weekFields.weekOfWeekBasedYear());
            int weekNumber = weekNumberForFirstVisit - weekNumberOfFirstDay + 1;

            while (weekNumber <= 6) {
                Map<String, Object> dayFrequency = new HashMap<>();
                dayFrequency.put("day", dayOfWeek);
                dayFrequency.put(FREQUENCY, weekNumber);
                dayAndFrequencyList.add(dayFrequency);
                weekNumber = weekNumber + (frequency / 7);
            }
        });
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
        String strVal = val.toString().trim();
        return "1".equals(strVal) || "true".equalsIgnoreCase(strVal);
    }
}
