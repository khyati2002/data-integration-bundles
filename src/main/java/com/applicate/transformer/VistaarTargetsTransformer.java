package com.applicate.transformer;

import com.applicate.services.channelkart.utils.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.applicate.services.channelkart.utils.JSONUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.util.*;

public class VistaarTargetsTransformer extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {

    private static final Logger logger = LoggerFactory.getLogger(VistaarTargetsTransformer.class);

    private static final String PARAMETER = "PARAMETER";
    private static final String TARGET = "TARGET";
    private static final String MONTH = "MONTH";
    private static final String YEAR = "YEAR";
    private static final String RCSID = "RCSID";
    private static final String ACH_PER = "ACH_PER";
    private static final String ACH = "ACH";
    private static final String FOCUS_DESC = "FOCUS_DESC";
    private static final String MAX_POINTS = "MAX_POINTS";
    private static final String LOGIN_ID = "loginId";

    private static final ObjectMapper mapper = JSONUtils.getObjectMapper();
    private static final Map<String, MonthsInfo> monthMap = createMonthMap();

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> targetMap = new HashMap<>();
        Map<String, Object> convertedMap = convertInputKeysToUppercase(inputMap);

        String parameter = getOptionalString(convertedMap, PARAMETER);
        String targetValue = getOptionalString(convertedMap, TARGET);
        String month = getOptionalString(convertedMap, MONTH);
        String year = getOptionalString(convertedMap, YEAR);
        String userValue = getOptionalString(convertedMap, RCSID);
        String achPer = getOptionalString(convertedMap, ACH_PER);
        String ach = getOptionalString(convertedMap, ACH);
        String focusDesc = getOptionalString(convertedMap, FOCUS_DESC);
        String maxPoints = getOptionalString(convertedMap, MAX_POINTS);

        targetMap.put("targetName", parameter);
        targetMap.put("activeStatus", "active");

        try {
            targetMap.put("target", new BigDecimal(targetValue));
        } catch (Exception e) {
            logger.warn("Could not parse 'TARGET' value: {}. Defaulting to 0.", targetValue);
            targetMap.put("target", BigDecimal.ZERO);
        }

        List<String> list = new ArrayList<>();
        list.add(userValue);
        ArrayNode arrayNode = mapper.valueToTree(list);
        targetMap.put("userValue", arrayNode);
        targetMap.put("userValueStr", userValue);

        String generatedTargetId = null;
        try{
            generatedTargetId = createTargetId(userValue, parameter, month, year);
            targetMap.put("targetId", generatedTargetId);
        }catch (Exception e){
            logger.error(e.getMessage());
        }

        Float achFloat = 0F;
        try {
            if(ach != null) achFloat = Float.parseFloat(ach);
        } catch (NumberFormatException e) {
            logger.warn("Could not parse 'ACH' value: {}", ach);
        }

        String achPerStr = null;
        try {
            if(achPer != null) {
                Double achPerDouble = Double.parseDouble(achPer);
                achPerStr = String.format("%.3f", achPerDouble);
            }
        } catch (NumberFormatException e) {
            logger.warn("Could not parse 'ACH_PER' value: {}", achPer);
        }

        targetMap.put("extendedAttributes", createExtendedAttributes(year, month, achPerStr, achFloat, userValue, focusDesc, maxPoints));
        targetMap.put("targetResults", createTargetResults(achFloat, userValue, generatedTargetId));

        if(!StringUtils.isEmpty(month) && !StringUtils.isEmpty(year)){
            List<LocalDateTime> dateList = getDates(month, year);
            if (dateList != null && !dateList.isEmpty()) {
                targetMap.put("startDate", dateList.get(0));
                targetMap.put("endDate", dateList.get(1));
            }
        }

        return targetMap;
    }

    /** Helper to get string from map safely */
    private String getOptionalString(Map<String, Object> map, String key) {
        return (map.containsKey(key) && map.get(key) != null) ? map.get(key).toString() : null;
    }

    /** Helper to convert all input keys to uppercase */
    private Map<String, Object> convertInputKeysToUppercase(Map<String, Object> inputMap) {
        Map<String, Object> convertedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
            String uppercaseKey = entry.getKey().toUpperCase();
            Object value = entry.getValue();
            convertedMap.put(uppercaseKey, value);
        }
        return convertedMap;
    }

    /** Helper to create the extendedAttributes JSON object */
    private ObjectNode createExtendedAttributes(String year, String month, String achPerStr, Float achFloat, String userValue, String focusDesc, String maxPoints) {
        ObjectNode jsonNode = mapper.createObjectNode();
        jsonNode.put("year", year);
        jsonNode.put("month", month);
        jsonNode.put("ach_per", achPerStr);
        jsonNode.put("achieved", achFloat);
        jsonNode.put(LOGIN_ID, userValue);
        jsonNode.put("focusDesc", focusDesc);
        jsonNode.put("MaxPts", maxPoints);
        return jsonNode;
    }

    /** Helper to create the targetResults JSON object list */
    private List<ObjectNode> createTargetResults(Float achFloat, String userValue, String targetId) {
        ObjectNode targetRes = mapper.createObjectNode();

        if (targetId != null) {
            targetRes.put("id", targetId);
            targetRes.put("targetId", targetId);
        }

        targetRes.put("activeStatus", "active");
        targetRes.put("achieved", achFloat);
        targetRes.put("hierarchy", "admin@applicate.in");
        targetRes.put("locationHierarchy", "India");
        targetRes.put(LOGIN_ID, userValue);
        return Collections.singletonList(targetRes);
    }

    public static class MonthsInfo {
        String num;
        String days;

        MonthsInfo(String num, String days){
            this.num =  num;
            this.days = days;
        }
    }

    /** Helper method to initialize the static monthMap */
    private static Map<String, MonthsInfo> createMonthMap() {
        Map<String, MonthsInfo> map = new HashMap<>();
        map.put("jan",new MonthsInfo("01", "31"));
        map.put("feb",new MonthsInfo("02", "28"));
        map.put("mar",new MonthsInfo("03", "31"));
        map.put("apr",new MonthsInfo("04", "30"));
        map.put("may",new MonthsInfo("05", "31"));
        map.put("jun",new MonthsInfo("06", "30"));
        map.put("jul",new MonthsInfo("07", "31"));
        map.put("aug",new MonthsInfo("08", "31"));
        map.put("sep",new MonthsInfo("09", "30"));
        map.put("oct",new MonthsInfo("10", "31"));
        map.put("nov",new MonthsInfo("11", "30"));
        map.put("dec",new MonthsInfo("12", "31"));
        return Collections.unmodifiableMap(map);
    }

    public static List<LocalDateTime> getDates(String month, String year){
        if (month == null || year == null) {
            logger.warn("Month or Year is null, cannot get dates.");
            return Collections.emptyList();
        }

        LocalDateTime startDate;
        LocalDateTime endDate;
        MonthsInfo monthInfo = monthMap.get(month.toLowerCase());
        if (monthInfo == null) {
            logger.warn("Invalid month provided: {}", month);
            return Collections.emptyList();
        }

        int monthNum = Integer.parseInt(monthInfo.num);
        int yearNum = Integer.parseInt(year);

        YearMonth yearMonth = YearMonth.of(yearNum, monthNum);
        startDate = yearMonth.atDay(1).atStartOfDay();

        if(monthNum == 2 && Year.isLeap(yearNum)){
            endDate = yearMonth.atDay(29).atTime(23, 59, 59);
        }else{
            endDate = yearMonth.atDay(Integer.parseInt(monthInfo.days)).atTime(23, 59, 59);
        }
        return new ArrayList<>(Arrays.asList(startDate, endDate));
    }

    public static String createTargetId(String loginId, String parameter, String month, String year) throws NullPointerException {
        if(StringUtils.isEmpty(loginId)){
            throw new NullPointerException("RCSID is Null or empty");
        }else if(StringUtils.isEmpty(parameter)){
            throw new NullPointerException("Parameter is null or empty");
        }else if(StringUtils.isEmpty(month)){
            throw new NullPointerException("Parameter is null or empty");
        }else if(StringUtils.isEmpty(year)){
            throw new NullPointerException("Parameter is null or empty");
        }
        return loginId + '-' + parameter + '-' + month + '-' + year;
    }
}