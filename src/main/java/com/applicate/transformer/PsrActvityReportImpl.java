package com.applicate.transformer;

import com.applicate.services.channelkart.exceptions.CustomRuntimeException;
import com.applicate.services.channelkart.querys.QueryResultDTO;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class PsrActvityReportImpl extends AbstractTransformer<List<QueryResultDTO<Object>>, Map<String, Object>> {

    public static final String STOCKIST_VISITED = "Stockist_Visited";
    public static final String SEC_OUTLET_ORDERED = "Sec_Outlet_Ordered";
    public static final String QUALIFIED_VISIT = "Qualified_Visit";

    @Override
    public Object transform(List<QueryResultDTO<Object>> queryResultDTOList) {
        List<Object> respData =  queryResultDTOList.get(0).getData();
        List<Map<String, Object>> finalResult = new ArrayList<>();

        LocalDate currentDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1); // Current date minus 1 day
        LocalDate startDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusMonths(2).withDayOfMonth(1);
        // Create a map of date as key and default values for stockist visited and sec outlet ordered
        Map<String, Map<String, String>> dateDataMap = new HashMap<>();

        // Populate the dateDataMap with all dates between fromDate and (currentDate -1)
        for (LocalDate date = startDate; !date.isAfter(currentDate); date = date.plusDays(1)) {
            Map<String, String> defaultData = new HashMap<>();
            defaultData.put(STOCKIST_VISITED, "0");
            defaultData.put(SEC_OUTLET_ORDERED, "0");
            defaultData.put(QUALIFIED_VISIT, "0");
            dateDataMap.put(date.toString(), defaultData);
        }

        // Convert Date format from query resp
        SimpleDateFormat sourceDateFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat targetDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (Object o : respData) {
            Map<String,Object> map=(Map<String,Object>) o;
            Object value = map.get("Date");
            Date date;
            try {
                date = sourceDateFormat.parse((String) value);
            } catch (ParseException e) {
                throw new CustomRuntimeException(e);
            }
            String reformattedDate = targetDateFormat.format(date);
            map.put("Date", reformattedDate);
        }

        // Process the query results and update the dateDataMap
        for (Object o : respData) {
            Map<String,Object> data =(Map<String,Object>) o;
            String date = data.get("Date").toString();
            String stockistVisited = data.get(STOCKIST_VISITED).toString();
            String secOutletOrdered = data.get(SEC_OUTLET_ORDERED).toString();

            if (dateDataMap.containsKey(date)) {
                Map<String, String> currentData = dateDataMap.get(date);
                currentData.put(STOCKIST_VISITED, stockistVisited);
                currentData.put(SEC_OUTLET_ORDERED, secOutletOrdered);

                // Calculate Qualified_Visit: 1 if secOutletOrdered >= 10 or stockistVisited >= 1, else 0
                String qualifiedVisit = (Integer.parseInt(secOutletOrdered) >= 10 || Integer.parseInt(stockistVisited) >= 1) ? "1" : "0";
                currentData.put(QUALIFIED_VISIT, qualifiedVisit);
            }
        }

        // Add the date data to the final result list
        for (Map.Entry<String, Map<String, String>> entry : dateDataMap.entrySet()) {
            Map<String, Object> resultEntry = new HashMap<>();
            resultEntry.put("Date", entry.getKey());
            resultEntry.putAll(entry.getValue());
            finalResult.add(resultEntry);
        }

        // Sort and convert to object
        finalResult.sort(Comparator.comparing(e -> e.get("Date").toString()));
        List<Object> finalResultAsObject = new ArrayList<>(finalResult);
        queryResultDTOList.get(0).setData(finalResultAsObject);
        return queryResultDTOList;
    }
}
