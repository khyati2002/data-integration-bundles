package com.applicate.transformer;

//import com.applicate.services.channelkart.models.User;
import com.salescode.dim.jooq.impl.User;
import com.applicate.services.channelkart.querys.QueryResultDTO;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.fasterxml.jackson.databind.JsonNode;

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

public class PsrActivityReportDataPush extends AbstractTransformer<List<QueryResultDTO<Object>>, Map<String, Object>> {

    private static final String QUALIFIED_VISIT = "Qualified_Visit";
    private static final String SEC_OUTLET_ORDERED = "Sec_Outlet_Ordered";
    private static final String STOCKIST_VISITED = "Stockist_Visited";
    private static final String BRANCH = "Branch";
    private static final String DISTRICT = "District";
    private static final String DATE = "Date";
    private static final String MONTH = "Month";
    private static final String YEAR = "Year";
    private static final String PSR_ID = "PSR_ID";
    private static final String PSR_NAME = "PSR_Name";
    private static final String PICKUP_STATUS = "pickupstatus";
    private static final String MODIFIED_DATE = "modifieddate";

    private static final DateTimeFormatter YYYY_MM_DD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Transformation of raw SQL data using query (getPsrActivityDataPush) into a complete daily report.
     *
     * @param queryResultDTOList The raw data structure returned from the database query.
     * @return The modified DTO containing the full daily report.
     */
    @Override
    public Object transform(List<QueryResultDTO<Object>> queryResultDTOList) {

        UserService userService = (UserService) ServiceLocator.lookup(UserService.class);
        List<Object> respData = queryResultDTOList.get(0).getData();

        Map<String, Object> initialRecord = (Map<String, Object>) respData.get(0);
        String loginId = (String) initialRecord.get("loginidParam");
        String fromDateStr = (String) initialRecord.get("fromDateParam");

        Map<String, Object> userDetails = fetchUserDetails(loginId, userService);

        Map<String, Map<String, Object>> actualDataMap = processActualData(respData);

        List<Map<String, Object>> finalResult = buildCompleteReport(fromDateStr, loginId, userDetails, actualDataMap);

        queryResultDTOList.get(0).setData(new ArrayList<>(finalResult));
        return queryResultDTOList;
    }

    private Map<String, Object> fetchUserDetails(String loginId, UserService userService) {
        User user = userService.findByLoginId(loginId);
        Map<String, Object> userDetails = new HashMap<>();
        if (user != null) {
            userDetails.put(PSR_NAME, user.getName());
            JsonNode extendedAttributes = user.getExtendedAttributes();
            if (extendedAttributes != null) {
                userDetails.put(DISTRICT, extendedAttributes.has(DISTRICT) ? extendedAttributes.get(DISTRICT).asText() : null);
                userDetails.put(BRANCH, extendedAttributes.has(BRANCH) ? extendedAttributes.get(BRANCH).asText() : null);
            }
        }
        return userDetails;
    }

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
     * @param loginId       The user's login ID.
     * @param userDetails   A map containing static user details.
     * @param actualDataMap A map of actual data from the database.
     * @return A complete and gapless list of daily records.
     */
    private List<Map<String, Object>> buildCompleteReport(String fromDateStr, String loginId, Map<String, Object> userDetails, Map<String, Map<String, Object>> actualDataMap) {
        List<Map<String, Object>> fullReport = new ArrayList<>();
        LocalDate endDate = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(1);
        LocalDate startDate = LocalDate.parse(fromDateStr, YYYY_MM_DD_FORMATTER);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String currentDateStr = date.format(YYYY_MM_DD_FORMATTER);
            Map<String, Object> dailyEntry = new HashMap<>();

            dailyEntry.putAll(userDetails);
            dailyEntry.put(PSR_ID, loginId);
            dailyEntry.put(DATE, currentDateStr);
            dailyEntry.put(MONTH, date.getMonth().toString().substring(0, 3));
            dailyEntry.put(YEAR, String.valueOf(date.getYear()));
            dailyEntry.put(PICKUP_STATUS, 0);
            dailyEntry.put(MODIFIED_DATE, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(new Date()));

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
     * Safely parses an object into a Number, defaulting to 0 if parsing fails or the object is null.
     *
     * @param obj The object to parse.
     * @return The parsed Number, or 0 as a default.
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