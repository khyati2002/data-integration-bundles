package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.GenericEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PricingElementTransformerCokeSA extends AbstractTransformer<Map<String,Object>,List<Map<String, Object>>> {

    GenericEntityService genericEntityService = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        List<GenericEntity> taxes = genericEntityService.readModelsByName("TaxDefined");
        taxes.stream()
                .filter(tax -> {
                    JsonNode payload = tax.getPayload();
                    return payload.has("taxProgram") && "EXCISE".equalsIgnoreCase(payload.get("taxProgram").asText());
                })
                .forEach(tax -> responseList.add(createResponse(inputMap, tax)));

        return responseList;
    }

    private Map<String, Object> createResponse(Map<String, Object> inputMap, GenericEntity tax) {
        Map<String, Object> response = new HashMap<>();
        JsonNode taxPayload = tax.getPayload();

        Object sku = inputMap.get("AM11_ARTNUM");

        if (sku == null || sku.toString().trim().isEmpty()) {
            throw new NullPointerException("SKU Code is mandatory and cannot be null or empty");
        }

        String skuCode = sku.toString().replaceAll("\\.0$", "");
        response.put("skuCode", skuCode);
        response.put("batchCode", skuCode);

        Object taxTypeP=taxPayload.get("taxProgram");
        if (taxTypeP == null || taxTypeP.toString().trim().isEmpty()) {
            throw new NullPointerException("Tax Type is mandatory and cannot be null or empty");
        }
        String taxType = taxTypeP.toString().replaceAll("\\.0$", "");
        response.put("taxType", taxType);

        String taxRateValue = "0";

        if (taxPayload.has("priceElementColumn")) {
            String columnIndex = taxPayload.get("priceElementColumn").asText().trim();

            try {
                int columnNumber = Integer.parseInt(columnIndex);
                String columnKey = String.format("AM11_PRIENY%02d", columnNumber);
                Object rawValue = inputMap.get(columnKey);
                if (rawValue != null) {
                    taxRateValue = rawValue.toString();
                }
            } catch (NumberFormatException e) {

                System.err.println("Invalid priceElementColumn value: " + columnIndex);
            }
        }

        response.put("taxRate", taxRateValue);
        return response;
    }




}

