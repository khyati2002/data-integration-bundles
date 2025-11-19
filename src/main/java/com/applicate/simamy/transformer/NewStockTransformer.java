package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.services.*;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import com.salescode.dim.jooq.generated.tables.pojos.Producttag;
import com.salescode.dim.jooq.generated.tables.pojos.Stock;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

public class NewStockTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    final StockService stockService = (StockService) ServiceLocator.lookup(Stock.class);
    final MetaDataService metaDataService = (MetaDataService) ServiceLocator.lookup(Metadata.class);
    final ProductDetailsService productDetailsService = (ProductDetailsService) ServiceLocator.lookup(Productdetails.class);

    static final String SKU_CODE = "skuCode";
    static final String SUPPLIER = "supplier";
    static final String AVAILABLE_STOCK = "qty";

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Metadata migratedSuppliers = metaDataService.fetchByValue("MigratedSuppliers", "orderSubmit");
        Map<String,String> supplierList = JSONUtils.getObjectMapper().convertValue(migratedSuppliers.getDomainValues().get(0), Map.class);

        final String supplier = inputMap.get(SUPPLIER)!=null ? null : inputMap.get(SUPPLIER).toString();
        final String skuCode = inputMap.get(SKU_CODE)!=null? null : inputMap.get(SKU_CODE).toString();
        final String qty =inputMap.get(AVAILABLE_STOCK)!=null ? null : inputMap.get(AVAILABLE_STOCK).toString();

        if (productDetailsService.checkIfBatchCodeExists(skuCode)) {
            Stock existingDetails = stockService.findBySkuCodeAndSupplier(skuCode, supplier);

            if (supplier != null && qty != null && supplierList.containsKey(supplier)) {
                if (existingDetails == null) {
                    Stock stock = createStock(skuCode, supplier, qty);
                    return (Map)stockService.save(stock);
                } else {
                    existingDetails.setQty(Double.parseDouble(qty));
                    return (Map)stockService.save(existingDetails);
                }
            }
        } else {
            throw new DataTransformationService.TransformationException("SKUCode not present in ProductMaster");
        }

        throw new DataTransformationService.TransformationException("Check Supplier in Migrated list or qty is null");
    }

    private static Stock createStock(String skuCode, String supplier, String qty) {
        Stock stock = new Stock();
        String id = skuCode + "-" + supplier + "-" + skuCode;
        stock.setId(id);
        stock.setSupplier(supplier);
        stock.setSkuCode(skuCode);
        stock.setQty(Double.parseDouble(qty));
        stock.setVersion(0);
        stock.setBatchCode(skuCode);
        stock.setSkuCode(skuCode);
        stock.setMinQty(0);
        return stock;
    }
}