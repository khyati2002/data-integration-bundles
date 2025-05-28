package com.applicate.bepensa.enrichment;


import com.applicate.services.channelkart.services.DeliveryPJPService;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.DeliveryPJP;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Set;

public class DeliveryPJPEnrichment extends AbstractEnrichment<DeliveryPJP> {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public OperationResult.StepResult apply(DeliveryPJP pjp) {
        logger.info("Enrichment for deliverypjp started : ");
        try {
            UserService userService = (UserService) ServiceLocator.lookup(User.class);
            OutletDetailsService outletService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
            DeliveryPJPService pjpService = (DeliveryPJPService) ServiceLocator.lookup(DeliveryPJP.class);

            if(pjp.getLoginid()!=null && !pjp.getLoginid().equals("")) {
                setDesignation(pjp,userService);
            }
            if(pjp.getOutletcode()!=null) {
                setOutletDetails(pjp,userService,outletService);
            }
            LocalDateTime pjpDate = pjp.getPjpDate();

            if(pjpDate!=null) {
                pjpService.addDayAndFrequency(pjp);
            }
        }catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage());
        }
        return new OperationResult.StepResult(OperationResult.Status.OK,"Delivery PJP Data enriched successfully");
    }

    private void setDesignation(DeliveryPJP pjp, UserService userService) {
        User resultFromDb=userService.findByLoginId(pjp.getLoginid());
        if(resultFromDb!=null) {
            Set<String> designations = resultFromDb.getDesignation();
            String designationName = null;
            if(designations!=null && !CollectionUtils.isEmpty(designations)) {
                designationName = designations.iterator().next();
            }
            pjp.setDesignation(designationName!=null?designationName.toLowerCase():null);
        }else {
            pjp.setLoginid(null);
        }
    }

    private void setOutletDetails(DeliveryPJP pjp, UserService userService, OutletDetailsService outletService) {

        OutletDetails outletFromDB = outletService.findByOutletCode(pjp.getOutletcode());
        if(outletFromDB!=null) {
            String outletLoginId = outletFromDB.getUserName()!=null?outletFromDB.getUserName().getLoginid():null;
            if(StringUtils.isValidString(outletLoginId)) {
                User resultFromDb=userService.findByLoginId(outletLoginId);
                if(resultFromDb!=null)
                    outletFromDB.setUserName(resultFromDb);
            }else {
                outletFromDB.setUserName(null);
                logger.info("login id is null for outlet code {} ",pjp.getOutletcode());
            }
        }else {
            pjp.setOutletcode(null);
        }

    }

}
