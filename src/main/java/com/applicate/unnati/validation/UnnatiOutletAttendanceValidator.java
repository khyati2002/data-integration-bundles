package com.applicate.unnati.validation;


import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletActivity;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UnnatiOutletAttendanceValidator extends AbstractValidationRule<OutletActivity> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String RCSID = "RCSID";
    private static final String SIFYID = "SIFYID";
    private static final String WDNAME = "WDNAME";
    private static final String DSID = "DSID";
    private static final String DSNAME = "DSNAME";

    @Override
    public OperationResult.StepResult apply(OutletActivity cdm) {

        if(!cdm.getActivity().equals("attendance")){
            return OperationResult.StepResult.OK;
        }

        final OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);

        List<String> errors = new ArrayList<>();

        if(cdm.getOutletCode() == null ||
                cdm.getOutletCode().trim().isEmpty() ||
                cdm.getOutletCode().equals("null")){
            errors.add("UID is null or empty");
        } else {
            OutletDetails dbOutlet = outletDetailsService.findByOutletCode(cdm.getOutletCode());
            if(dbOutlet == null){
                errors.add("UID is not present in DB.");
            }
        }

        if(cdm.getLoginId() == null ||
                cdm.getLoginId().trim().isEmpty() ||
                cdm.getLoginId().equals("null")){
            errors.add("WD Dest is null or empty");
        }

        validateExtendedAttributes(cdm, errors);

        if (!errors.isEmpty()) {
            String errorstr = StringUtils.format(
                    "Validation error occured for OutletActivity. Kindly go through provided errors and make sure those conditions should fulfill while retrying. {}",
                    String.join(",", errors));
            logger.error(errorstr);
            return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
        }
        return OperationResult.StepResult.OK;
    }

    private static void validateExtendedAttributes(OutletActivity cdm, List<String> errors) {
        if (cdm.getExtendedAttributes() != null) {
            JsonNode extendedAttributes= cdm.getExtendedAttributes();
            validateNullOrEmpty(RCSID,extendedAttributes, errors);
            validateNullOrEmpty(SIFYID,extendedAttributes, errors);
            validateNullOrEmpty(WDNAME,extendedAttributes, errors);
            validateNullOrEmpty(DSID,extendedAttributes, errors);
            validateNullOrEmpty(DSNAME,extendedAttributes, errors);
        }
    }

    private static void validateNullOrEmpty(String key, JsonNode extendedAttributes, List<String> errors) {
        if (!extendedAttributes.has(key) ||
                (extendedAttributes.get(key) == null ||
                        extendedAttributes.get(key).asText().trim().isEmpty() ||
                        extendedAttributes.get(key).asText().trim().equals("null"))){
            errors.add(String.format("%s is null or empty.",key));
        }
    }
}
