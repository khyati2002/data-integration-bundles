package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.services.SequenceInfoService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.SequenceInfo;
import org.apache.commons.lang3.StringUtils;

import java.time.Year;

public class NewOutletMappingEnrichment extends AbstractEnrichment<OutletDetails> {

    private static final String OUTLET_DETAILS= "OutletDetails";
    private static final String OUTLET_CODE= "outletCode";

    private final SequenceInfoService service= (SequenceInfoService) ServiceLocator.lookup(SequenceInfo.class);

    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {
        if(org.apache.commons.lang3.StringUtils.isNotEmpty(cdm.getOutletcode()) && !"auto_generated".equalsIgnoreCase(cdm.getOutletcode()) && !service.matchesSequencePattern(OUTLET_DETAILS, OUTLET_CODE, cdm.getOutletcode())){
            return new OperationResult.StepResult(OperationResult.Status.OK,"Data enrichment skipped due to already existing outletcode");
        }
        if(cdm.getExtendedAttributes().get("designation").toString().equals("depot")) {
            return new OperationResult.StepResult(OperationResult.Status.OK,"Data enrichment skipped due to already existing outletcode");
        }
        int nextSequence = service.getSequenceNumber(OUTLET_DETAILS, OUTLET_CODE);
        String yearPattern = getYearPattern();
        String sequenceFormat="%05d";
        String generatedCode = yearPattern + "COKESA"+ String.format(sequenceFormat, nextSequence);
        cdm.setOutletCode(generatedCode);
        return new OperationResult.StepResult(OperationResult.Status.OK,"Data enriched successfully");
    }

    private String getYearPattern(){
        String currentYear = StringUtils.substring(Year.now().getValue()+"", 2);
        String aheadYear =  StringUtils.substring((Year.now().getValue()+1)+"", 2);
        return currentYear + "" + aheadYear ;
    }
}