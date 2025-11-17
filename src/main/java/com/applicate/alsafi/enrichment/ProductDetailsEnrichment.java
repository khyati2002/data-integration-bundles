package com.applicate.alsafi.enrichment;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class ProductDetailsEnrichment extends AbstractEnrichment<Productdetails> {

    @Override
    public OperationResult.StepResult apply(Productdetails cdm) {
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
            cdm.setCaseToPieceQuantity(BigDecimal.valueOf((0)));
        }

        if (cdm.getCaseToOtherUnitQuantity() == BigDecimal.valueOf(0)) {
            cdm.setCaseToOtherUnitQuantity(BigDecimal.valueOf(0));
        }

        if (cdm.getOtherUnitToPieceQuantity() == BigDecimal.valueOf(0)) {
            cdm.setOtherUnitToPieceQuantity(BigDecimal.valueOf(0));
        }

        if (cdm.getPieceToOtherUnitQuantity() == BigDecimal.valueOf(0)) {
            cdm.setPieceToOtherUnitQuantity(BigDecimal.valueOf(0));
        }

        if (cdm.getMrp() == BigDecimal.valueOf(0)) {
            cdm.setMrp(BigDecimal.valueOf(0));
        }

        if (cdm.getCaseMrp() == BigDecimal.valueOf(0)) {
            cdm.setCaseMrp(BigDecimal.valueOf(0));
        }

        if (Objects.equals(cdm.getOtherUnitMrp(), BigDecimal.valueOf(0))) {
            cdm.setOtherUnitMrp(BigDecimal.valueOf(0));
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