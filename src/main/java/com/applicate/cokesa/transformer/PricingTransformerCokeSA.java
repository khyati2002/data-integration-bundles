package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.DateUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
        response.put("toDate", convertCustomStringToFormatted(requireNonNullValue(inputMap, "AM25_EFTDAT"), false));
        response.put("fromDate", convertCustomStringToFormatted(requireNonNullValue(inputMap, "AM25_EFRDAT"), true));
        Object val = requireNonNullValue(inputMap, "AM25_PRI");
        BigDecimal bd;

        if (val instanceof BigDecimal) {
            bd = (BigDecimal) val;
        } else if (val instanceof Number) {
            bd = BigDecimal.valueOf(((Number) val).doubleValue());
        } else {
            bd = new BigDecimal(val.toString());
        }

        response.put("casePtr", bd);
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

    public static String convertCustomStringToFormatted(String customDateStr, boolean isEndOfDay) {
        Date date = convertCustomStringToDate(customDateStr, isEndOfDay);

        Instant instant = date.toInstant();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'T'HH:mm:ss'Z'")
                .withZone(ZoneId.of("UTC"));

        return formatter.format(instant);
    }

}

