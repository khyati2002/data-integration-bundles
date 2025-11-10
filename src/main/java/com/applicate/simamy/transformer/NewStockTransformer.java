package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.models.MetaData;
import com.applicate.services.channelkart.models.Stock;
import com.applicate.services.channelkart.repository.StockRepository;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.StockService;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NewStockTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    final StockRepository stockRepository = SpringContext.getBean(StockRepository.class);
    final StockService stockService = SpringContext.getBean(StockService.class);
    final MetaDataService metaDataService = SpringContext.getBean(MetaDataService.class);
    final ProductDetailsService productDetailsService = SpringContext.getBean(ProductDetailsService.class);
    static final String SKU_CODE = "skuCode";
    static final String SUPPLIER = "supplier";
    static final String AVAILABLE_STOCK = "qty";

    @Override
    public Object transform(Map<String, Object> inputMap) {
        MetaData migratedSuppliers =  metaDataService.fetchByValue("MigratedSuppliers","orderSubmit");
        Map<String,String> supplierList = JSONUtils.toStringMap(migratedSuppliers.getDomainValues().get(0));
        Map<String, Object> stockTransformed = new HashMap<>();
        final String supplier = ObjectUtils.isEmpty(inputMap.get(SUPPLIER)) ? null : inputMap.get(SUPPLIER).toString();
        final String skuCode = ObjectUtils.isEmpty(inputMap.get(SKU_CODE)) ? null : inputMap.get(SKU_CODE).toString();
        final String qty = ObjectUtils.isEmpty(inputMap.get(AVAILABLE_STOCK)) ? null : inputMap.get(AVAILABLE_STOCK).toString();
        if(productDetailsService.checkIfBatchCodeExists(skuCode)) {
            Stock existingDetails = getExistingDetails(skuCode, supplier);
            Date now = new Date();
            if (supplier != null && qty != null && supplierList.containsKey(supplier.toString())) {
                if (existingDetails == null) {
                    Stock stock = new Stock();
//            stockTransformed.put(SUPPLIER,supplier);
//            stockTransformed.put(AVAILABLE_STOCK,qty);
//            stockTransformed.put(SKU_CODE,skuCode);
                    String id = skuCode + "-" + supplier + "-" + skuCode;
                    stock.setId(id);
                    stock.setSupplier(supplier);
                    stock.setSkuCode(skuCode);
                    stock.setQty(Float.parseFloat(qty));
                    stock.setVersion(0);
                    stock.setBatchCode(skuCode);
                    stock.setBatchPrice(0.0);
                    stock.setInitialStock(0);
                    stock.setInitialQty(0);
                    stock.setMinQty(0);
                    stock.setPrice(0);
                    stock.setStockValue(0);
                    return stockService.save(stock);
                } else {
                    existingDetails.setQty(Float.parseFloat(qty));
                    return existingDetails;
                }
            }
        }
        else{
            throw new TransformationException("SKUCode not present in ProductMaster");
        }
        throw new TransformationException("Check Supplier in Migrated list or qty is null");

    }


    private Stock getExistingDetails(String skuCode,String supplier) {
        if(!(stockService.findBySkuCodeAndSupplier(skuCode,supplier)==null))
            return stockService.findBySkuCodeAndSupplier(skuCode,supplier);
        return null;
    }
}
