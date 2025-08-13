package com.applicate.unnati.validation;


import com.applicate.services.channelkart.client.properties.PropertyDefinition;
import com.applicate.services.channelkart.client.properties.PropertyRegistry;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class OutletDetailsNullValueValidatorITCL extends AbstractValidationRule<OutletDetails> {

    private static final Logger logger = LoggerFactory.getLogger(OutletDetailsNullValueValidatorITCL.class);
    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {

        logger.info("Entering validation: {}", OutletDetailsNullValueValidatorITCL.class.getSimpleName());
        logger.debug("Validation input details - outletName: {}, outletCode: {}, channel: {}, extendedAttributes: {}",
                cdm.getOutletName(),
                cdm.getOutletcode(),
                cdm.getChannel(),
                cdm.getExtendedAttributes());

        PropertyRegistry propertyRegistry = PropertyRegistry.getInstance();

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
        if (!propertyRegistry.getAsBoolean(PropertyDefinition.USE_SUPPLIER_FROM_OUTLET_METADATA)) {
            if (cdm.getExtendedAttributes() != null) {
                if (!cdm.getExtendedAttributes().has("supplierMapping")) {
                    ruleResult.add("custID cannot be null, outletSIFYID cannot be null");
                }
            } else {
                ruleResult.add("custID cannot be null, outletSIFYID cannot be null");
            }
        }
        return ruleResult.isEmpty() ? OperationResult.StepResult.OK : new OperationResult.StepResult(OperationResult.Status.ERROR, StringUtils.join(ruleResult, ", "));

    }
}

