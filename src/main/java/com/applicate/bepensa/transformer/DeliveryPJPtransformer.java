package com.applicate.bepensa.transformer;


import com.applicate.services.channelkart.services.CustomerAccountsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.CustomerAccount;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeliveryPJPtransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private final CustomerAccountsService accountsService = (CustomerAccountsService) ServiceLocator.lookup(CustomerAccount.class);

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();

        LocalDateTime currentDate = LocalDateTime.now(); // get current date
        int dayOfWeek = Integer.parseInt(inputMap.get("day").toString());
        LocalDateTime firstDesiredDayOfCurrentMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth()).with(TemporalAdjusters.nextOrSame(getDayOfWeek(dayOfWeek)));

        // Add dates for the current month
        responseList.addAll(getAllDesiredDaysInMonth(inputMap, firstDesiredDayOfCurrentMonth));

        // Add dates for the next month only if visit_type is 1
        if ("1".equals(inputMap.get("visit_type").toString())) {
            LocalDateTime firstDesiredDayOfNextMonth = currentDate.plusMonths(1).with(TemporalAdjusters.firstDayOfMonth()).with(TemporalAdjusters.nextOrSame(getDayOfWeek(dayOfWeek)));
            responseList.addAll(getAllDesiredDaysInMonth(inputMap, firstDesiredDayOfNextMonth));
        }

        if (responseList.isEmpty()) {
            throw new DataTransformationService.TransformationException("Day not available for the current month");
        }
        return responseList;
    }

    private DayOfWeek getDayOfWeek(int day) {
        switch (day) {
            case 1:
                return DayOfWeek.SUNDAY;
            case 2:
                return DayOfWeek.MONDAY;
            case 3:
                return DayOfWeek.TUESDAY;
            case 4:
                return DayOfWeek.WEDNESDAY;
            case 5:
                return DayOfWeek.THURSDAY;
            case 6:
                return DayOfWeek.FRIDAY;
            case 7:
                return DayOfWeek.SATURDAY;
            default:
                throw new DataTransformationService.TransformationException("Invalid day value: " + day);
        }
    }

    private List<Map<String, Object>> getAllDesiredDaysInMonth(Map<String, Object> inputMap, LocalDateTime firstDesiredDayOfMonth) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        LocalDateTime currentDate = firstDesiredDayOfMonth;

        while (currentDate.getMonth() == firstDesiredDayOfMonth.getMonth()) {
            Map<String, Object> responseMap = createResponseMap(inputMap, currentDate);
            responseList.add(responseMap);
            currentDate = currentDate.plusWeeks(1);
        }

        return responseList;
    }

    private Map<String, Object> createResponseMap(Map<String, Object> inputMap, LocalDateTime date) {
        Map<String, Object> responseMap = new HashMap<>();

        String beat = inputMap.get("idruta") != null ? inputMap.get("idruta").toString() : "";
        String loginId = inputMap.get("codemp") != null ? inputMap.get("codemp").toString() : "";
        String outletCode = inputMap.get("Customer_Code") != null ? inputMap.get("Customer_Code").toString() : "";
        String type = inputMap.get("visit_type") != null ? inputMap.get("visit_type").toString() : "";


        responseMap.put("beat", beat);
        responseMap.put("loginId", loginId);
        responseMap.put("outletcode", outletCode);
        responseMap.put("type", type);
        responseMap.put("supplierid", inputMap.get("branch_code") != null ? inputMap.get("branch_code").toString() : "");
        responseMap.put("month", date.getMonth().name());
        responseMap.put("year", String.valueOf(date.getYear()));
        responseMap.put("activeStatus", inputMap.get("activo") != null ? addStatus(inputMap.get("activo").toString()) : "");
        responseMap.put("pjpPlan", inputMap.get("day") != null ? inputMap.get("day").toString() : "");


        date = date.withHour(5).withMinute(30).withSecond(0).withNano(0);
        responseMap.put("pjpDate", date);


        String formattedDate = formatDateForId(date);
        String id = outletCode + "-" + loginId + "-" + formattedDate + "-" + type + "-" + beat;
        responseMap.put("id", id);

        return responseMap;
    }


    private String formatDateForId(LocalDateTime date) {
        String dayOfWeek = date.getDayOfWeek().name().substring(0, 3).toLowerCase(); // e.g., tue
        String month = date.getMonth().name().substring(0, 3).toLowerCase();         // e.g., may
        String dayOfMonth = String.format("%02d", date.getDayOfMonth());             // e.g., 13
        String time = String.format("%02d:%02d:%02d", date.getHour(), date.getMinute(), date.getSecond()); // 00:00:00
        int year = date.getYear();
        return dayOfWeek + "-" + month + "-" + dayOfMonth + "-" + time + "-utc-" + year;
    }

    public static String addStatus(String status) {
        return status.equals("true") ? "active" : "inactive";
    }
}
