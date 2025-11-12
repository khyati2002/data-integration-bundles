package com.applicate.alsafi.enrichment;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.ProductDetails;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ProductDetailsEnrichment extends AbstractEnrichment<ProductDetails> {

    @Override
    public OperationResult.StepResult apply(ProductDetails cdm) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (cdm == null) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Enrichment error: ProductDetails not found null");
        }

        if (StringUtils.isBlank(cdm.getBatchCode())) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Enrichment error: batchCode is required");
        }

        if (StringUtils.isBlank(cdm.getCategory())) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Enrichment error: category is required");
        }

        if (StringUtils.isBlank(cdm.getSubCategory())) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Enrichment error: subCategory is required");
        }

        if (StringUtils.isBlank(cdm.getSkuCode()) && StringUtils.isNotBlank(cdm.getBatchCode())) {
            cdm.setSkuCode(cdm.getBatchCode());
        }

        if (cdm.getCaseToPieceQuantity() == null) {
            cdm.setCaseToPieceQuantity(0.0f);
        }

        if (cdm.getCaseToOtherUnitQuantity() == 0) {
            cdm.setCaseToOtherUnitQuantity(0.0f);
        }

        if (cdm.getOtherUnitToPieceQuantity() == 0) {
            cdm.setOtherUnitToPieceQuantity(0.0f);
        }

        if (cdm.getPieceToOtherUnitQuantity() == 0) {
            cdm.setPieceToOtherUnitQuantity(0.0f);
        }

        if (cdm.getMrp() == 0) {
            cdm.setMrp(0.0f);
        }

        if (cdm.getCaseMrp() == 0) {
            cdm.setCaseMrp(0.0f);
        }

        if (cdm.getOtherUnitMrp() == 0) {
            cdm.setOtherUnitMrp(0.0f);
        }

        if (cdm.getPriority() == 0) {
            cdm.setPriority(999);
        }

        if (cdm.getActiveStatus() == null || cdm.getActiveStatus().equals(ActiveStatus.INACTIVE)) {
            cdm.setActiveStatus(ActiveStatus.INACTIVE);
            if (StringUtils.isBlank(cdm.getActiveStatusReason()) || !cdm.getActiveStatusReason().startsWith("Deactivated")) {
                cdm.setActiveStatusReason("Deactivated by " + SecurityContextUtils.getPrincipal() + " on " + dateFormat.format(new Date()));
            }
        } else if (cdm.getActiveStatus().equals(ActiveStatus.ACTIVE)) {
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
            if (StringUtils.isBlank(cdm.getActiveStatusReason()) || cdm.getActiveStatusReason().startsWith("Deactivated")) {
                cdm.setActiveStatusReason(ActiveStatus.ACTIVE.name());
            }
        } else {
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
            cdm.setActiveStatusReason(ActiveStatus.ACTIVE.name());
        }

        return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully");
    }
}