package com.applicate.cokesa.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.applicate.services.channelkart.utils.NullUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JourneyPlanTransformerCokeSa extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createResponse(inputMap));
        return responseList;
    }
    private Map<String, Object> createResponse(Map<String, Object> inputMap) {
        Map<String, Object> response = new HashMap<>();
        response.put("key1", inputMap.get("OM16_CALTYP").toString()); // sales route type
        response.put("key2", inputMap.get("OM16_CALRTE").toString()); // sales route code
        String depo = inputMap.get("OM16_CALLOC").toString();       // depo location
        String salesmanCode = inputMap.get("OM16_PRNNUM").toString(); // salesman code

        String finalSalesmanCode = depo + salesmanCode;
        response.put("key3", finalSalesmanCode);
        response.put("key4", convertCustomStringToDate(inputMap.get("OM16_EFRDAT").toString(), false)); // from date
        response.put("key5", convertCustomStringToDate(inputMap.get("OM16_EFTDAT").toString(), true)); // to date (end of day)
        response.put("key6", inputMap.get("OM16_CALLOC").toString()); // depo location
        response.put("name", "OM16_Route"); // identifier
        return response;
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
        return year + "-" + month + "-" + day + " " + time; // yyyy-MM-dd HH:mm:ss
    }





}
