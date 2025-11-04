package com.applicate.transformer;

import com.applicate.services.channelkart.querys.QueryResultDTO;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PsrActivityReportAttendanceDataPush extends AbstractTransformer<List<QueryResultDTO<Object>>, Map<String, Object>> {

    private static final String QUALIFIED_VISIT = "Qualified_Visit";
    private static final String SEC_OUTLET_ORDERED = "Sec_Outlet_Ordered";
    private static final String STOCKIST_VISITED = "Stockist_Visited";
    private static final String DATE = "Date";

    private static final DateTimeFormatter YYYY_MM_DD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Orchestrates the transformation of raw SQL data fetched using query (PsrActivityReportDataPush) into a complete daily report.
     *
     * @param queryResultDTOList The raw data structure returned from the database query.
     * @return The modified DTO containing the full, unsorted daily report.
     */
    @Override
    public Object transform(List<QueryResultDTO<Object>> queryResultDTOList) {
        List<Object> respData = queryResultDTOList.get(0).getData();

        Map<String, Object> initialRecord = (Map<String, Object>) respData.get(0);
        String fromDateStr = (String) initialRecord.get("fromDateParam");

        Map<String, Map<String, Object>> actualDataMap = processActualData(respData);
        List<Map<String, Object>> finalResult = buildCompleteReport(fromDateStr, actualDataMap);

        List<Object> finalResultAsObject = new ArrayList<>(finalResult);
        queryResultDTOList.get(0).setData(finalResultAsObject);
        return queryResultDTOList;
    }


    /**
     * Processes the raw list from the database into a clean map for efficient lookups.
     *
     * @param respData The raw list of data objects from the query.
     * @return A Map where the key is the date string ("yyyy-MM-dd") and the value is the data record.
     */
    private Map<String, Map<String, Object>> processActualData(List<Object> respData) {
        SimpleDateFormat sourceFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd");

        return respData.stream()
                .map(obj -> (Map<String, Object>) obj)
                .filter(map -> map.get(DATE) != null)
                .map(map -> {
                    Map<String, Object> newMap = new HashMap<>(map);
                    try {
                        Date date = sourceFormat.parse((String) newMap.get(DATE));
                        newMap.put(DATE, targetFormat.format(date));
                    } catch (ParseException e) {
                        return null;
                    }
                    newMap.putIfAbsent(STOCKIST_VISITED, "0");
                    newMap.putIfAbsent(SEC_OUTLET_ORDERED, "0");
                    return newMap;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(map -> (String) map.get(DATE), map -> map, (v1, v2) -> v1));
    }

    /**
     * Builds the complete report by iterating through the date range and populating each day
     * with either actual data from the DB or generated zero-filled data.
     *
     * @param fromDateStr   The start date for the report.
     * @param actualDataMap A map of actual data from the database.
     * @return A complete and gapless list of daily records.
     */
    private List<Map<String, Object>> buildCompleteReport(String fromDateStr, Map<String, Map<String, Object>> actualDataMap) {
        List<Map<String, Object>> fullReport = new ArrayList<>();
        LocalDate endDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1);
        LocalDate startDate = LocalDate.parse(fromDateStr, YYYY_MM_DD_FORMATTER);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String currentDateStr = date.format(YYYY_MM_DD_FORMATTER);
            Map<String, Object> dailyEntry = new HashMap<>();
            dailyEntry.put(DATE, currentDateStr);
            if (actualDataMap.containsKey(currentDateStr)) {
                Map<String, Object> actualData = actualDataMap.get(currentDateStr);
                dailyEntry.put(STOCKIST_VISITED, actualData.get(STOCKIST_VISITED));
                dailyEntry.put(SEC_OUTLET_ORDERED, actualData.get(SEC_OUTLET_ORDERED));
            } else {
                dailyEntry.put(STOCKIST_VISITED, "0");
                dailyEntry.put(SEC_OUTLET_ORDERED, "0");
            }

            calculateQualifiedVisit(dailyEntry);
            cleanupTemporaryColumns(dailyEntry);
            fullReport.add(dailyEntry);
        }
        return fullReport;
    }

    /**
     * Calculates the 'Qualified_Visit' field for a given row based on its activity metrics.
     *
     * @param row The data row (as a Map) to be updated.
     */
    private void calculateQualifiedVisit(Map<String, Object> row) {
        Number stockistVisitedNum = parseNumber(row.get(STOCKIST_VISITED));
        Number secOutletOrderedNum = parseNumber(row.get(SEC_OUTLET_ORDERED));

        if (stockistVisitedNum.intValue() >= 1 || secOutletOrderedNum.intValue() >= 10) {
            row.put(QUALIFIED_VISIT, 1);
        } else {
            row.put(QUALIFIED_VISIT, 0);
        }
    }

    /**
     * Safely parses an object into a Number, defaulting to 0.
     *
     * @param obj The object to parse.
     * @return The parsed Number, or 0.
     */
    private Number parseNumber(Object obj) {
        if (obj instanceof Number) {
            return (Number) obj;
        }
        try {
            return Integer.parseInt(String.valueOf(obj));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Removes temporary helper columns from the final result row.
     *
     * @param row The data row (as a Map) to be cleaned.
     */
    private void cleanupTemporaryColumns(Map<String, Object> row) {
        row.remove("loginidParam");
        row.remove("fromDateParam");
    }
}