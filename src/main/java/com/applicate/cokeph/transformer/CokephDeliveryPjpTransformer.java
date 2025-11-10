package com.applicate.cokeph.transformer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;

public class CokephDeliveryPjpTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
    private static final Logger logger = LoggerFactory.getLogger(CokephDeliveryPjpTransformer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
//    OrderDetailsService orderDetailsService = SpringContext.getBean(OrderDetailsService.class);
    private String [] weekdays={"monday","tuesday","wednesday","thursday","friday","saturday","sunday"};
    @Override
    public List<Map<String, Object>> transform(Map<String, Object> input) {
        if (input != null && !input.isEmpty()) {
            List<Map<String, Object>> transformedList = new ArrayList<>();
            try {
                // Transform the data

//               List<Map<String,String>> result= PjpMaster(input);
//                transformedList.add(PjpMasterTransform(result));

                transformedList.add(transformOrderBody(input));


                // Convert the result to a JSON string
                String resultJsonString = objectMapper.writeValueAsString(transformedList);

                // Log the transformed data
                logger.info("Transformed data: {}", resultJsonString);

                // Return the transformed data
                return transformedList;
            } catch (Exception ex) {
                logger.error("Transformer Exception", ex);
                throw new RuntimeException("Transformation failed", ex);
            }
        } else {
            return new ArrayList<>();
        }
    }


    public ArrayNode PjpMasterTransform(List<Map<String,String>> dayAndFrequencyMap){
        Map<String,Object> transformedDataMap=new HashMap<>();

        ArrayNode dayAndFrequencyArray = objectMapper.createArrayNode();
        for (Map<String, String> dayAndFreq : dayAndFrequencyMap) {
            ObjectNode dayAndFrequencyNode = objectMapper.createObjectNode();
            dayAndFrequencyNode.put("day", dayAndFreq.get("day"));
            dayAndFrequencyNode.put("frequency", dayAndFreq.get("frequency"));
            dayAndFrequencyArray.add(dayAndFrequencyNode);
        }
        return  dayAndFrequencyArray;
    }

    public List<Map<String,String>> PjpMaster(Map<String, Object> input){

        Map<String,Object> hm=getDayAndWeekOfMonth(input.get("StartingWeekAnchorDate").toString());
        String day=hm.get("dayOfWeek").toString();
        int weekno= (int) hm.get("weekOfMonth");
        int num=0;

        boolean [] weekdays1=new boolean[7];
        weekdays1[0]= Boolean.valueOf(input.get("VisitMonday").toString());
        weekdays1[1]= Boolean.valueOf(input.get("VisitTuesday").toString());
        weekdays1[2]= Boolean.valueOf(input.get("VisitWednesday").toString());
        weekdays1[3]= Boolean.valueOf(input.get("VisitThursday").toString());
        weekdays1[4]= Boolean.valueOf(input.get("VisitFriday").toString());
        weekdays1[5]= Boolean.valueOf(input.get("VisitSaturday").toString());
        weekdays1[6]= Boolean.valueOf(input.get("VisitSunday").toString());


        for(int i =0;i<7;i++){
            String weekday=weekdays[i];
            if(day.equals(weekday)){
                num=i;
                break;
            }
        }

        HashMap<String,Boolean> hm1=new HashMap<>();
        HashMap<String,Boolean> hm2=new HashMap<>();

        for(int i =0;i<7;i++){
            hm1.put(weekdays[i],false);
            hm2.put(weekdays[i],false);
        }

        for(int i =num;i<7;i++){
            if(weekdays1[i]){
                hm1.put(weekdays[i],true);
            }
        }

        for(int i =0;i<num;i++){
            if(weekdays1[i]){
                hm2.put(weekdays[i],true);
            }
        }

        List<Map<String,String>> lis= listcheck1(weekno,hm1, Integer.parseInt(input.get("Frequency").toString()));
        List<Map<String,String>> lis1=listcheck2(weekno,hm2, Integer.parseInt(input.get("Frequency").toString()),lis);
        return lis1;
    }


    public List<Map<String,String>> listcheck1(int weekno,HashMap<String,Boolean > hm,int frequency){
        List<Map<String,String>> l =new ArrayList<>();
        for(int i =weekno;i<=5;i+=(frequency/7)){
            for (Map.Entry<String, Boolean> entry : hm.entrySet()) {

                String key = entry.getKey();
                Boolean value = entry.getValue();
                if(value){
                    HashMap<String,String> hm1=new HashMap<>();
                    hm1.put("day",key);
                    hm1.put("frequency", String.valueOf(i));
                    l.add(hm1);
                }


            }
        }

        return l;


    }

    public List<Map<String,String>> listcheck2(int weekno,HashMap<String,Boolean > hm,int frequency,List<Map<String,String>> l){

        for(int i =weekno+1;i<=5;i+=(frequency/7)){
            for (Map.Entry<String, Boolean> entry : hm.entrySet()) {

                String key = entry.getKey();
                Boolean value = entry.getValue();
                if(value){
                    HashMap<String,String> hm1=new HashMap<>();
                    hm1.put("day",key);
                    hm1.put("frequency",String.valueOf(i));
                    l.add(hm1);
                }


            }
        }

        return l;
    }


    public  Map<String, Object> getDayAndWeekOfMonth(String dateStr) {
        dateStr = dateStr.replaceAll("[-.]", "/");

        if (dateStr.matches("\\d{2}/\\d{2}/\\d{4}")) {

            dateStr = dateStr.substring(0, 6) + dateStr.substring(8);
        }

        // Parse the input date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yy");
        LocalDate date = LocalDate.parse(dateStr, formatter);

        // Get the day of the week
        String dayOfWeek = date.getDayOfWeek().toString().toLowerCase();

        // Get the week of the month
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekOfMonth = date.get(weekFields.weekOfMonth());

        // Get month and year
        String monthName = date.getMonth().name();  // Get full month name in uppercase
        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1).toLowerCase();  // Capitalize first letter
        int year = date.getYear();

        // Prepare the result map
        Map<String, Object> result = new HashMap<>();
        result.put("dayOfWeek", dayOfWeek);
        result.put("weekOfMonth", weekOfMonth);
        result.put("month", monthName);
        result.put("year", year);



        return result;
    }

    private Map<String, Object> transformOrderBody(Map<String, Object> dataMap) {
        Map<String, Object> transformedDataMap = new HashMap<>();

        Map<String, Object> dayAndWeekInfo = getDayAndWeekOfMonth(dataMap.get("StartingWeekAnchorDate").toString());
        String month = (String) dayAndWeekInfo.get("month");  // Get month from the dayAndWeekInfo
        int year = (int) dayAndWeekInfo.get("year");

        // Get day and frequency mappings
//        List<Map<String, String>> dayAndFrequencyMap = createDayAndFrequencyMap(dataMap);
        List<Map<String,String>> result= PjpMaster(dataMap);

//        put some code here


        // Add day and frequency to an ArrayNode
        ArrayNode dayAndFrequencyArray = objectMapper.createArrayNode();
//        for (Map<String, String> dayAndFreq : dayAndFrequencyMap) {
//            ObjectNode dayAndFrequencyNode = objectMapper.createObjectNode();
//            dayAndFrequencyNode.put("day", dayAndFreq.get("day"));
//            dayAndFrequencyNode.put("frequency", dayAndFreq.get("frequency"));
//            dayAndFrequencyArray.add(dayAndFrequencyNode);
//        }
        transformedDataMap.put("dayAndFrequency", PjpMasterTransform(result));

        // Add the required attributes
        transformedDataMap.put("outletCode", String.valueOf(dataMap.get("CustomerId")));
        transformedDataMap.put("loginId", String.valueOf(dataMap.get("RouteId")));
        transformedDataMap.put("activeStatus", (Boolean.parseBoolean(String.valueOf(dataMap.get("Active")))) ? "active" : "inactive");

        // Add month and year to the transformed data map
        transformedDataMap.put("month", month);
        transformedDataMap.put("year", year);

        // Add extendedAttributes as a single field
        Map<String, Object> extendedAttributes = new HashMap<>();
        extendedAttributes.put("StartingWeekAnchorDate", dataMap.get("StartingWeekAnchorDate"));
        extendedAttributes.put("Frequency", dataMap.get("Frequency"));
        extendedAttributes.put("ScheduleTypeId", dataMap.get("ScheduleTypeId"));
        extendedAttributes.put("Sequence", dataMap.get("Sequence"));
        transformedDataMap.put("extendedAttributes", extendedAttributes);

        return transformedDataMap;
    }

    public List<Map<String, String>> createDayAndFrequencyMap(Map<String, Object> dataMap) {
        List<Map<String, String>> dayAndFrequency = new ArrayList<>();
        List<String> days = Arrays.asList("VisitMonday", "VisitTuesday", "VisitWednesday", "VisitThursday", "VisitFriday", "VisitSaturday", "VisitSunday");
//        Map<String, Object> hm = getDayAndWeekOfMonth("StartingWeekAnchorDate");
        for (String day : days) {
            if ("true".equals(String.valueOf(dataMap.get(day)))) {
                String dayStr = day.substring(5).toLowerCase(); // Remove 'Visit' prefix and convert to lowercase
                dayAndFrequency.addAll(getWeeklyFrequencyMap(dayStr, Integer.parseInt(dataMap.get("Frequency").toString())));
            }
        }
        return dayAndFrequency;
    }

    public List<Map<String, String>> getWeeklyFrequencyMap(String day, int frequency) {
        List<Map<String, String>> weeklyFrequencyMap = new ArrayList<>();
        int pjpFrequency = (31 / frequency) + 1;
        for (int i = 1; i <= pjpFrequency; i++) {
            Map<String, String> weeklyFrequency = new HashMap<>();
            weeklyFrequency.put("day", day);
            weeklyFrequency.put("frequency", String.valueOf(i));
            weeklyFrequencyMap.add(weeklyFrequency);
        }



        return weeklyFrequencyMap;
    }
}
