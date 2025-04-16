package com.applicate.unnati.validation;


import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OutletDetailsNullValueValidatorITCL extends AbstractValidationRule<OutletDetails> {

    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {

        List<String> ruleResult = new ArrayList<String>();
        if (cdm.getOutletName() == null || "".equals(cdm.getOutletName())) {
            ruleResult.add("OutletName can not be null");
        }

        if (cdm.getOutletcode() == null || "".equals(cdm.getOutletcode())) {
            ruleResult.add("OutletName can not be null");
        }

        if (cdm.getChannel() == null || "".equals(cdm.getChannel())) {
            ruleResult.add("Channel can not be null");
        }

        if (cdm.getExtendedAttributes() != null) {
            if (!cdm.getExtendedAttributes().has("supplierMapping")) {
                ruleResult.add("custID can not be null, outletSIFYID can not be null");
            }
        } else {
            ruleResult.add("custID can not be null, outletSIFYID can not be null");
        }

        if (ruleResult.size() > 0) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, StringUtils.join(ruleResult, ", "));
        } else {
            return OperationResult.StepResult.OK;
        }
    }

}

