package com.applicate.kbpl.validation;


import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.StockService;
import com.applicate.services.channelkart.utils.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.Stock;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class StockValidationKbpl extends AbstractValidationRule<Stock> {

    @Override
    public OperationResult.StepResult apply(Stock cdm) {
        StockService stockService = (StockService) ServiceLocator.lookup(Stock.class);

        List<String> errors = new ArrayList<>();

        double caseQty = cdm.getInitialQty() ;

        JsonNode extendedAttributes = cdm.getExtendedAttributes();

        JsonNode itemType = extendedAttributes.get("itemtypecode");

        JsonNode dateString = extendedAttributes.get("expiredate");

        if(!StringUtils.isNullOrBlank(itemType) &&  !ObjectUtils.isEmpty(itemType.asText())){
            if (!itemType.asText().equals("1")){
                errors.add("Non FG products cannot be picked");
            }
        } else {
            errors.add("Error while fetching itemtypecode");
        }

      /* if(StringUtils.isNotNull(caseQty) &&  !ObjectUtils.isEmpty(caseQty)){
               if (caseQty < 10){
              errors.add("case qty should be more than 10");
            }
        } else {
            errors.add("Error in quantity");
          }*/




        if(!StringUtils.isNullOrBlank(dateString) && !ObjectUtils.isEmpty(dateString.asText())){
            LocalDateTime dateTime = convertStringToLocalDateTime(dateString.asText());

            ZoneId istZoneId = ZoneId.of("Asia/Kolkata");
            ZonedDateTime currentDateTimeIST = ZonedDateTime.now(istZoneId);
            LocalDateTime currentDateTime = currentDateTimeIST.toLocalDateTime();

            LocalDateTime dateTimeMinus9Days = dateTime.minusDays(9);

            if (dateTimeMinus9Days.isBefore(currentDateTime)) {
                errors.add("product is expired");
            }
        } else {
            errors.add("Error while fetching expiredate");
        }

        if (!errors.isEmpty()) {
            String errorStr = StringUtils.format("Error saving stock Reason : [{}]",  org.apache.commons.lang3.StringUtils.join(errors, ","));
            return new OperationResult.StepResult(OperationResult.Status.ERROR, errorStr);
        }

        return OperationResult.StepResult.OK;
    }
    public static LocalDateTime convertStringToLocalDateTime(String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]");
        return LocalDateTime.parse(dateString, formatter);
    }
}

