package com.applicate.unnati.enrichment;


import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.TargetResults;
import com.salescode.dim.jooq.impl.Targets;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;

@Slf4j
public class TargetsEnrichmentITCL extends AbstractEnrichment<Targets> {

    public static final String MONTH = "month";
    public static final String YEAR = "year";
    public static final String ENRICHMENT_ERROR = "Enrichment error: %s";

    @Override
    public OperationResult.StepResult apply(Targets cdm) {

        JsonNode extendAttr = cdm.getExtendedAttributes();
        List<String> errors = new ArrayList<>();
        String monthStr ="";
        String yearStr ="";
        if(extendAttr.has(MONTH)){
            monthStr=getMonth(cdm,errors);
        }else {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Enrichment error: year is required");
        }
        if(extendAttr.has(YEAR)){
            yearStr = getYear(cdm);
        }else {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Enrichment error: year is required");
        }
        if (NullUtils.isNotNull(cdm.getTargetName()) && cdm.getTargetName().equals("Foods-Snacks<Rs.20")) {
            cdm.setTargetName("Foods-Snacks<Rs. 20");
        }
        if(!errors.isEmpty()){
            return new OperationResult.StepResult(OperationResult.Status.ERROR, String.format(TargetsEnrichmentITCL.ENRICHMENT_ERROR,String.join(",",errors)));
        }
        int year= Integer.parseInt(yearStr);
        int month= Integer.parseInt(monthStr);
        LocalDateTime startDate = LocalDateTime.of(year,month, 1, 0, 0, 0);
        cdm.setStartDate(startDate);

        LocalDate endOfMonthDate = YearMonth.of(year, month).atEndOfMonth();
        LocalDateTime endDate = LocalDateTime.of(endOfMonthDate, LocalTime.of(23, 59, 59));
        cdm.setEndDate(endDate);


        List<TargetResults> targetResults = cdm.getTargetResults();
        for(TargetResults targetResults1 : targetResults){
            targetResults1.setId(cdm.getId());
            targetResults1.setTargetId(cdm.getId());
        }
        return OperationResult.StepResult.OK;
    }

    public String  getYear(Targets cdm){
        JsonNode extendAttr = cdm.getExtendedAttributes();
        return  extendAttr.get(YEAR).textValue();
    }

    public String  getMonth(Targets cdm,List<String> errors){
        JsonNode extendAttr = cdm.getExtendedAttributes();
        ArrayList<String> months =  new ArrayList<>(List.of("1","2","3","4","5","6","7","8","9","10","11","12"));
        int ind;
        String month="";
        if(cdm.getTargetName().equals("IncentiveTarget")) {
            if(months.contains(extendAttr.get(MONTH).textValue())) {
                try {
                    ind = Integer.parseInt(extendAttr.get(MONTH).textValue());
                    return months.get(ind - 1).length() == 2 ? months.get(ind - 1) : "0" + months.get(ind - 1);
                } catch (Exception e) {
                    errors.add(String.format(ENRICHMENT_ERROR ,e.getMessage()));
                }
            }
        }
        else {
            month = extendAttr.get(MONTH).textValue();
            month = month.substring(0, 1).toUpperCase() + month.substring(1).toLowerCase();
            try {
                Date date = new SimpleDateFormat("MMM", Locale.ENGLISH).parse(month);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int mnth = cal.get(Calendar.MONTH) + 1;
                month = mnth < 10 ? "0" + mnth : String.valueOf(mnth);
            } catch (ParseException e) {
                errors.add(String.format(ENRICHMENT_ERROR ,e.getMessage()));
            }
        }
        return month;
    }
}
