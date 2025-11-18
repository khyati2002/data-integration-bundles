package com.applicate.simasg.enrichment;


import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.OutletDetails;


/**
 * If the phone number change is to be blocked, find the previous phone number from database
 * and set it to incoming record. This way, the phone number of outlet will remain same if the
 * phone number is to be blocked.
 */
public class SimaSgOutletPhoneNumberEnrichment extends AbstractEnrichment<OutletDetails> {

    @Override
    public EnrichmentResult apply(OutletDetails outletDetails) {
        if(allowContactNumberChange(outletDetails)){
            return new OperationResult.StepResult(OperationResult.Status.OK,"Data Enrichment skipped");
        }
        outletDetails.setContactno( ((OutletDetails) outletDetails.getOldModel()).getContactno() );
        return new OperationResult.StepResult(OperationResult.Status.OK,"Data Enrichment skipped");
    }

    /**
     * Checks if we need to allow phone number change for outlet. We allow phone number change if the
     * following conditions fulfill.
     * 1.   Change request is made by a user other than integration_user.
     * 2.   oldModel is null, which means the object is meant for insert operation and not update operation.
     * 3.   Either extendedAttribute is null or it does not contain oldMobileNo key. This means that the phone number
     *      is not previously changed by a supplier or some other legitimate user using API call.
     * @param outletDetails {@link OutletDetails} object for which we need to allow / block phone number update
     * @return boolean true or false
     */
    private boolean allowContactNumberChange(OutletDetails outletDetails){
        return !"integration_user".equalsIgnoreCase(SecurityContextUtils.getPrincipal()) ||
                outletDetails.getOldModel()==null ||
                outletDetails.getExtendedAttributes()==null ||
                !outletDetails.getExtendedAttributes().has("oldMobileNo");
    }


}
