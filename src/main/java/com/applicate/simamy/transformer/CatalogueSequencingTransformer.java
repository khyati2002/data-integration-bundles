package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.models.ProductTag;
import com.applicate.services.channelkart.services.ProductTagService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CatalogueSequencingTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
    private static final ProductTagService productTagService = SpringContext.getBean(ProductTagService.class);
    private static final String DESCRIPTION = "CatalogueSequencing";
    private static final String DEFAULT = "default";
    @Override
    public Object transform(Map<String, Object> input) {
        ProductTag productTag ;
        if(input.containsKey("OUTLET CODE"))
            productTag=transformOutletDataAndRefresh(input);
        else if(input.containsKey("SKU CODE"))
            productTag = transformProductDataAndRefresh(input);
        else
            throw new TransformationException("Either SKU CODE or OUTLET CODE should be present");

        return JSONUtils.convert(productTagService.save(productTag),Map.class);
    }
    private ProductTag transformOutletDataAndRefresh(Map<String, Object> input){
        String outletcode = getStringValue(input, "OUTLET CODE");
        String set = getStringValue(input, "SET");
        if(outletcode.equalsIgnoreCase("DELETE")){
            List<ProductTag> entitiesToDelete = productTagService.findByTagGroup(set);
            productTagService.deleteAll(entitiesToDelete);
            return getDefaultProductTag();
        } else if (set.equalsIgnoreCase("DELETE")) {
            List<ProductTag> entitiesToDelete = productTagService.findByOutlet(outletcode);
            productTagService.deleteAll(entitiesToDelete);
            return getDefaultProductTag();
        }
        List<ProductTag> existingTags = productTagService.findByOutlet(outletcode);
        ProductTag pTag;
        if(!existingTags.isEmpty()){
            pTag =  existingTags.get(0);
        }
        else{
            pTag = new ProductTag();
            pTag.setOutlet(outletcode);
        }
        pTag.setTagGroup(set);
        pTag.setTagCode("0");
        pTag.setTagDescription(DESCRIPTION.concat("-outlet"));
        pTag.setProductValue("OUTLET");
        return productTagService.refresh(productTagService.save(pTag));
    }
    public static Date parseInputDate(String dateString) throws ParseException {
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        return inputFormat.parse(dateString);
    }
    private ProductTag getDefaultProductTag(){
        ProductTag productTag = new ProductTag();
        productTag.setOutlet(DEFAULT);
        productTag.setTagGroup(DEFAULT);
        productTag.setTagDescription(DEFAULT);
        productTag.setProductValue(DEFAULT);
        return  productTag;
    }
    private String getStringValue(Map<String, Object> map, String key) {
        return map.get(key) != null ? map.get(key).toString() : "";
    }
    private ProductTag transformProductDataAndRefresh(Map<String, Object> input){
        String skuCode = getStringValue(input, "SKU CODE");
        String set = getStringValue(input, "Sequence Set");
        String priority = getStringValue(input, "PRIORITY");
        String startDate = getStringValue(input, "startDate");
        String endDate = getStringValue(input, "endDate");

        List<ProductTag> existingTags = productTagService.findBySkuCodeAndTagGroup(skuCode,set);
        ProductTag pTag;
        if(!existingTags.isEmpty()){
            pTag =  existingTags.get(0);
        }
        else{
            pTag = new ProductTag();
            pTag.setSkuCode(skuCode);
        }
        try{
            pTag.setStartDate(parseInputDate(startDate));
            pTag.setEndDate(parseInputDate(endDate));
        } catch (ParseException e) {
            throw new TransformationException("Unable to convert date with formatter dd/MM/yyyy");
        }
        pTag.setTagGroup(set);
        pTag.setTagCode(priority);
        pTag.setTagDescription(DESCRIPTION.concat("-product"));
        pTag.setProductValue("PRODUCT");

        return productTagService.refresh(productTagService.save(pTag));
    }
}