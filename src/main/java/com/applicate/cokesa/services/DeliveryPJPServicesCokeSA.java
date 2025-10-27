package com.applicate.cokesa.services;

import com.applicate.services.channelkart.calendar.BusinessCalendar;
import com.applicate.services.channelkart.calendar.CalendarService;
import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.applicate.services.channelkart.deliverydate.DeliveryDateInterface;
import com.applicate.services.channelkart.exceptions.CustomRuntimeException;
import com.applicate.services.channelkart.models.CustomerAccountInfo;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.applicate.services.channelkart.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
public class DeliveryPJPServicesCokeSA implements DeliveryDateInterface {

    private static final String DELIVERY_DATE="deliveryDate";
    private static final String LOGINID="loginId";
    private static final String DESIGNATION="designation";
    private static final String RETAILER="retailer";

    private PropertyRegistry propertyRegistry = SpringContext.getBean(PropertyRegistry.class);
    private GenericEntityService genericEntityService = SpringContext.getBean(GenericEntityService.class);
    private CalendarService calendarService = SpringContext.getBean(CalendarService.class);
    private UserService userService= SpringContext.getBean(UserService.class);


    private ZoneId getClientZoneId() {
        CustomerAccountsService customerService = (CustomerAccountsService) ServiceLocator
                .lookup(CustomerAccountInfo.class);
        String timeZone = customerService.getTimeZone();
        if (timeZone == null || timeZone.equals("")) {
            timeZone = "Asia/Kolkata";
        }
        return ZoneId.of(timeZone);
    }

    @Override
    public Map<String, String> getDeliveryDate(Map<String, Object> inputMap){
        String type=inputMap.get("type").toString();
        String value=inputMap.get("value").toString();


        String mappingName="LeadTimeMapping";
        Map<String,String> responseMap=new HashMap<>();

        LocalDateTime now = LocalDateTime.now(getClientZoneId());
        LocalDateTime expiryTime = now.toLocalDate().atStartOfDay();
        Map<String,String> deliveryDate;


        String currentLoginId =SecurityContextUtils.getPrincipal();
        List<Map<String, Object>> designationResult = userService.getUserDesignationsGroupedByLoginId(Collections.singletonList(currentLoginId));
        Set<String> userDesignation = new HashSet<>();
        if(!designationResult.isEmpty()){
            Map<String,Object> data= designationResult.get(0);
            if(data.containsKey(LOGINID) && data.get(LOGINID).equals(currentLoginId)){
                userDesignation=(Set<String>)data.get(DESIGNATION);
            }
        }

        // cut off time check and generate expiry time
        if(propertyRegistry.getAsBoolean(PropertyDefinition.CUSTOM_EXPIRY_FOR_DELIVERYDATE_ENALBED) &&  userDesignation.contains(RETAILER)) { // client properties cutoff configuration should be present
            List<GenericEntity> cutOffTimeConfig = genericEntityService.readModelsByName("CutOffTiming");
            if (cutOffTimeConfig.isEmpty() || cutOffTimeConfig.size() > 1) {
                throw new CustomRuntimeException("Invalid cut off time configration");
            }
            Integer cutOffhour = Integer.parseInt(cutOffTimeConfig.get(0).getKey1());
            Integer cutOffMin = Integer.parseInt(cutOffTimeConfig.get(0).getKey2());
            Integer cutOffSec = Integer.parseInt(cutOffTimeConfig.get(0).getKey3());
            expiryTime = now.withHour(cutOffhour).withMinute(cutOffMin).withSecond(cutOffSec);
            if (now.isAfter(expiryTime)) {
                now = now.plusDays(1);
                expiryTime=expiryTime.plusDays(1);
            }
        }else{ // default is next day
            expiryTime=expiryTime.plusDays(1);
        }


        String dateString=now.toLocalDate().toString();
        String key=type+"_"+value+"_"+dateString;

        // fetches all values for the given type
        List<GenericEntity> leadTimeMapping = genericEntityService.readModelsByNameAndKey1(mappingName,type);
        if(leadTimeMapping.isEmpty()) throw new CustomRuntimeException("Lead time mapping not present for the given type");

        //generate key
        Map<String,Integer> customLeadTimeMapping= leadTimeMapping.stream()
                .collect(Collectors.toMap(
                        entity->entity.getKey1()+"_"+entity.getKey2()+"_"+dateString,
                        entity -> Integer.parseInt(entity.getKey3())));

        // generate delivery dates
        deliveryDate=getDeliveryDate(customLeadTimeMapping,now);


        String calculatedDate=deliveryDate.get(key);
        if(calculatedDate==null) throw new CustomRuntimeException("Invalid value {} for given type {}",type,value);
        responseMap.put(DELIVERY_DATE,calculatedDate);


        responseMap.put("expiryTime",expiryTime.toString());
        responseMap.put("type","0");
        return responseMap;
    }
    private Map<String,String> getDeliveryDate(Map<String,Integer> leadTimeMapping,LocalDateTime dateTime){
        Map<String,String> map=new HashMap<>();
        String holidayConfig= propertyRegistry.getValue(PropertyDefinition.HOLIDAYS_CONFIG_FOR_DELIVERYDATE);
        boolean isFieldWiseHoliday = propertyRegistry.getAsBoolean(PropertyDefinition.IS_FIELD_WISE_HOLIDAY);
        if(holidayConfig.equalsIgnoreCase("IGNORE")){
            leadTimeMapping.forEach((type,value)->
                    map.put(type,dateTime.plusDays(value).toLocalDate().toString())
            );
        }else if(holidayConfig.equalsIgnoreCase("INCREMENT") || holidayConfig.equalsIgnoreCase("SKIP")){
            BusinessCalendar businessCalendar=calendarService.getBusinessCalendar();
            leadTimeMapping.forEach((type,value)->{
                LocalDate dateOfDelivery= LocalDate.from(dateTime);

                boolean isHoliday= isHoliday(isFieldWiseHoliday, businessCalendar, dateOfDelivery, type);
                while(value>0 || isHoliday){
                    if(Boolean.FALSE.equals(isHoliday) || holidayConfig.equalsIgnoreCase("INCREMENT")){
                        value--;
                    }
                    dateOfDelivery=dateOfDelivery.plusDays(1);
                    isHoliday=isHoliday(isFieldWiseHoliday, businessCalendar, dateOfDelivery, type);
                }
                map.put(type,dateOfDelivery.toString());
            });
        }else{
            throw new CustomRuntimeException("Inavlid property value for HOLIDAY_CONFIG_FOR_DELIVERYDATE");
        }
        return map;
    }
    private boolean isHoliday(boolean isFieldWiseHoliday, BusinessCalendar businessCalendar, LocalDate dateOfDelivery, String type){
        try{
            return isFieldWiseHoliday ?  businessCalendar.isTypeWiseHoliday(dateOfDelivery, type.split("_")[1]) : businessCalendar.isHoliday(dateOfDelivery);
        }catch (Exception e){
            return false;
        }
    }



}
