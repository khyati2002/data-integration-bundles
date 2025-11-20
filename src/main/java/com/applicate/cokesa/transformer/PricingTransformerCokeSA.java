package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.DateUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

public class PricingTransformerCokeSA
        extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createResponse(inputMap));
        return responseList;
    }

    private Map<String, Object> createResponse(Map <String, Object> inputMap){
        Map<String, Object> response = new HashMap<>();
        response.put("priceList", requireNonNullValue(inputMap,"AM25_PRILST"));
        response.put("batchCode", requireNonNullValue(inputMap, "AM25_ARTNUM").replaceAll("\\.0$", ""));
        response.put("skuCode", requireNonNullValue(inputMap, "AM25_ARTNUM").replaceAll("\\.0$", ""));
        response.put("toDate", convertCustomStringToLocalDateTime(requireNonNullValue(inputMap, "AM25_EFTDAT"), false));
        response.put("fromDate", convertCustomStringToLocalDateTime(requireNonNullValue(inputMap, "AM25_EFRDAT"), true));
        response.put("casePtr", requireNonNullValue(inputMap,"AM25_PRI"));
        return response;
    }

    private String requireNonNullValue(Map<String, Object> inputMap, String key) {
        Object value = inputMap.get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            throw new NullPointerException("Mandatory field missing: " + key);
        }
        return value.toString();
    }

    public static Date convertCustomStringToDate(String customDateStr, boolean isEndOfDay){
        if (NullUtils.isNull(customDateStr) || customDateStr.length() != 7) {
            throw new IllegalArgumentException("Invalid input format. Expected 7 characters.");
        }
        int centuryPart = Integer.parseInt(customDateStr.substring(0, 3));
        int year = 1900 + centuryPart;
        String month = customDateStr.substring(3, 5);
        String day = customDateStr.substring(5, 7);
        String time = isEndOfDay ? "23:59:59" : "00:00:00";
        String fullDateStr = year + "-" + month + "-" + day + " " + time;
        return DateUtils.parse(fullDateStr);
    }

    public static LocalDateTime convertCustomStringToLocalDateTime(String customDateStr, boolean isEndOfDay) {
        Date date = convertCustomStringToDate(customDateStr, isEndOfDay);

        if (date == null) {
            return null;   // or LocalDateTime.MAX → your choice
        }

        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

}

