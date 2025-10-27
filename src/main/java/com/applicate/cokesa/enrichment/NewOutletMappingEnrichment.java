package com.applicate.cokesa.enrichment;


import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.OperationResult.StepResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.services.SequenceInfoService;
import com.applicate.services.channelkart.services.SpringContext;
import org.apache.commons.lang.StringUtils;

import java.time.Year;

public class NewOutletMappingEnrichment extends AbstractEnrichment<OutletDetails> {

    private static final String OUTLET_DETAILS= "OutletDetails";
    private static final String OUTLET_CODE= "outletCode";

    private final SequenceInfoService service= SpringContext.getBean(SequenceInfoService.class);

    /**
     * Skips enrichment if outletCode passed is a non-empty value other than "auto-generated".
     * Creates outletCode in format {}{}. First field is occupied by channel code and other field is occupied by 6 digit sequnce number.
     * Sequence Number is auto incremented. We have separate sequence numbers for each channel code.
     *
     * @param cdm the Outlet object to enrich.
     * @return Enrichment result
     */
    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {
        if(org.apache.commons.lang3.StringUtils.isNotEmpty(cdm.getOutletCode()) && !"auto_generated".equalsIgnoreCase(cdm.getOutletCode()) && !service.matchesSequencePattern(OUTLET_DETAILS, OUTLET_CODE, cdm.getOutletCode())){
            return new OperationResult.StepResult(Status.OK,"Data enrichment skipped due to already existing outletcode");
        }
        int nextSequence = service.getSequenceNumber(OUTLET_DETAILS, OUTLET_CODE);
        String yearPattern = getYearPattern();
        String sequenceFormat="%05d";
        String generatedCode = yearPattern + "COKESA"+ String.format(sequenceFormat, nextSequence);
        cdm.setOutletCode(generatedCode);
        return new OperationResult.StepResult(Status.OK,"Data enriched successfully");
    }

    private String getYearPattern(){
        String currentYear = StringUtils.substring(Year.now().getValue()+"", 2);
        String aheadYear =  StringUtils.substring((Year.now().getValue()+1)+"", 2);
        return currentYear + "" + aheadYear ;
    }
}
