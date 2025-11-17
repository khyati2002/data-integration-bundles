package com.applicate.mmcoke.transformer;

import com.applicate.services.channelkart.models.Sales;
import com.applicate.services.channelkart.models.SalesDetails;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class SalesTransformer extends AbstractTransformer<List<Map<String, Object>>, List<Sales>> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<Sales> transform(List<Map<String, Object>> inputList) {
        Map<String, List<Map<String, Object>>> groupedByInvoice = inputList.stream()
                .collect(Collectors.groupingBy(map -> map.get("invoiceNumber").toString()));

        List<Sales> salesList = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedByInvoice.entrySet()) {
            String invoiceNumber = entry.getKey();
            List<Map<String, Object>> lineItems = entry.getValue();

            Sales sales = createSalesHeader(lineItems.get(0), invoiceNumber);
            List<SalesDetails> salesDetailsList = createSalesDetails(lineItems, invoiceNumber);

            sales.setSalesDetails(salesDetailsList);

            float totalQuantity = salesDetailsList.stream()
                    .map(SalesDetails::getItemQuantity1)
                    .reduce(0f, Float::sum);
            sales.setTotalQuantity(totalQuantity);

            salesList.add(sales);
        }

        return salesList;
    }

    private Sales createSalesHeader(Map<String, Object> firstRow, String invoiceNumber) {
        Sales sales = new Sales();

        sales.setInvoiceNumber(invoiceNumber);
        sales.setReferenceNumber(getStringValue(firstRow, "orderNumber"));
        sales.setOutletCode(getStringValue(firstRow, "outletCode"));
        sales.setLoginId(getStringValue(firstRow, "loginId"));
        sales.setBillAmount(getDoubleValue(firstRow, "billAmount"));
        sales.setNetAmount(getDoubleValue(firstRow, "netAmount"));
        sales.setOrderedDate(parseDate(firstRow.get("orderedDate")));
        sales.setBeat(getStringValue(firstRow, "routeId"));
        sales.setCurrencyCode(getStringValue(firstRow, "currencyCode"));
        sales.setStatus("ACTIVE");

        return sales;
    }

    private List<SalesDetails> createSalesDetails(List<Map<String, Object>> lineItems, String invoiceNumber) {
        List<SalesDetails> detailsList = new ArrayList<>();

        for (Map<String, Object> lineItem : lineItems) {
            SalesDetails details = new SalesDetails();

            details.setInvoiceNumber(invoiceNumber);
            details.setSerialNumber(getStringValue(lineItem, "salesLineNo"));
            details.setItemCode(getStringValue(lineItem, "itemCode"));
            details.setItemQuantity1(getFloatValue(lineItem, "itemQuantity1"));
            details.setPcQuantity(getFloatValue(lineItem, "pcQuantity"));
            details.setItemPrice(getDoubleValue(lineItem, "itemPrice"));
            details.setAmount(getDoubleValue(lineItem, "amount"));
            details.setTotalDiscountAmount(getDoubleValue(lineItem, "totalDiscountAmount"));
            details.setTotalTaxAmount(getDoubleValue(lineItem, "totalTaxAmount"));

            Integer freeItem = getIntegerValue(lineItem, "isFreeGood");
            details.setIsFreeGood(freeItem != null && freeItem == 1);

            detailsList.add(details);
        }

        return detailsList;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return ObjectUtils.isEmpty(value) ? null : value.toString().trim();
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (ObjectUtils.isEmpty(value)) return 0.0;

        try {
            if (value instanceof BigDecimal) {
                return ((BigDecimal) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Float getFloatValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (ObjectUtils.isEmpty(value)) return 0f;

        try {
            if (value instanceof BigDecimal) {
                return ((BigDecimal) value).floatValue();
            }
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return 0f;
        }
    }

    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (ObjectUtils.isEmpty(value)) return null;

        try {
            if (value instanceof BigDecimal) {
                return ((BigDecimal) value).intValue();
            }
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Date parseDate(Object dateValue) {
        if (ObjectUtils.isEmpty(dateValue)) return null;

        try {
            if (dateValue instanceof Date) {
                return (Date) dateValue;
            }
            return DATE_FORMAT.parse(dateValue.toString());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}