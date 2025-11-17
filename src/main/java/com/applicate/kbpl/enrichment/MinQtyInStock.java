package com.applicate.kbpl.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.StockService;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.Stock;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MinQtyInStock extends AbstractEnrichment<Stock> {

    @Override
    public EnrichmentResult apply(Stock cdm) {
        String user = SecurityContextUtils.getPrincipal();
        if ("integration_user".equalsIgnoreCase(user)) {
            return new OperationResult.StepResult(OperationResult.Status.OK, "Skipped Stock Min Qty Enrichment");
        }

        String cdmSku = cdm.getSkuCode();
        String supplier = cdm.getSupplier();
        StockService stockService = (StockService) ServiceLocator.lookup(Stock.class);
        List<Stock> stocks = stockService.findbySkuCodeSkuCodeAndSupplierLoginId(List.of(cdmSku), supplier);
        if (stocks.isEmpty()) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "No such sku code with given supplier");
        } else {
            stocks.forEach(s -> {
                s.setMinQty(cdm.getMinQty());
                JsonNode ext = s.getExtendedAttributes();
                updateExpiry(ext);
            });
            OperationResult.StepResult updatedStockMinQty = new OperationResult.StepResult(OperationResult.Status.OK, "Updated stock Min Qty");
            updatedStockMinQty.setStepResultData(new ArrayList<>(stocks));
            return updatedStockMinQty;
        }
    }


    public LocalDateTime convertStringToLocalDateTime(String dateString) {
        dateString = dateString.contains(".") ?  dateString.substring(0,dateString.indexOf('.')) : dateString;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.parse(dateString, formatter);
    }


    public void updateExpiry(JsonNode extendedAttributes) {
        if (NullUtils.isNotNull(extendedAttributes)) {
            JsonNode cdmDateString = extendedAttributes.get("expiredate");
            if (!StringUtils.isNullOrBlank(cdmDateString) && !ObjectUtils.isEmpty(cdmDateString.asText())) {
                LocalDateTime dateTime = convertStringToLocalDateTime(cdmDateString.asText());

                LocalDateTime currentDateTime = LocalDateTime.now();

                if (dateTime.isBefore(currentDateTime)) {
                    ((ObjectNode) extendedAttributes).put("updateExpiry", true);
                    ((ObjectNode) extendedAttributes).put("expiredate", LocalDateTime.now().plusDays(25).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'23:59:59")));
                    ((ObjectNode) extendedAttributes).set("previousExpireDate", cdmDateString);
                }
            }

        }
    }
}
