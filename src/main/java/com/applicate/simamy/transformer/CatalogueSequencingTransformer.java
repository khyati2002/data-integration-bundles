package com.applicate.simamy.transformer;


import com.applicate.services.channelkart.services.ProductTagService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.Producttag;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class CatalogueSequencingTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
    private static final ProductTagService productTagService = (ProductTagService) ServiceLocator.lookup(Producttag.class);
    private static final String DESCRIPTION = "CatalogueSequencing";
    private static final String DEFAULT = "default";
    @Override
    public List<Map<String, Object>> transform(Map<String, Object> input) {
        Producttag productTag ;
        if(input.containsKey("OUTLET CODE"))
            productTag=transformOutletDataAndRefresh(input);
        else if(input.containsKey("SKU CODE"))
            productTag = transformProductDataAndRefresh(input);
        else
            throw new DataTransformationService.TransformationException("Either SKU CODE or OUTLET CODE should be present");
        productTagService.save(productTag);
        return new ArrayList<>();
    }
    private Producttag transformOutletDataAndRefresh(Map<String, Object> input){
        String outletcode = getStringValue(input, "OUTLET CODE");
        String set = getStringValue(input, "SET");
        if(outletcode.equalsIgnoreCase("DELETE")){
            List<Producttag> entitiesToDelete = productTagService.findByTagGroup(set);
            productTagService.delete(entitiesToDelete);
            return getDefaultProducttag();
        } else if (set.equalsIgnoreCase("DELETE")) {
            List<Producttag> entitiesToDelete = productTagService.findByOutlet(outletcode);
            productTagService.deleteAll(entitiesToDelete);
            return getDefaultProducttag();
        }
        List<Producttag> existingTags = productTagService.findByOutlet(outletcode);
        Producttag pTag;
        if(!existingTags.isEmpty()){
            pTag =  existingTags.get(0);
        }
        else{
            pTag = new Producttag();
            pTag.setOutletCode(outletcode);
        }
        pTag.setTagGroup(set);
        pTag.setTagCode("0");
        pTag.setTagDescription(DESCRIPTION.concat("-outlet"));
        pTag.setProductValue("OUTLET");
        return productTagService.refresh(productTagService.save(pTag));
    }
    public static LocalDateTime parseInputDate(String dateString) {
        try {
            DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate date = LocalDate.parse(dateString, inputFormat);
            return date.atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format: " + dateString, e);
        }
    }
    private Producttag getDefaultProducttag(){
        Producttag productTag = new Producttag();
        productTag.setOutletCode(DEFAULT);
        productTag.setTagGroup(DEFAULT);
        productTag.setTagDescription(DEFAULT);
        productTag.setProductValue(DEFAULT);
        return  productTag;
    }
    private String getStringValue(Map<String, Object> map, String key) {
        return map.get(key) != null ? map.get(key).toString() : "";
    }
    private Producttag transformProductDataAndRefresh(Map<String, Object> input){
        String skuCode = getStringValue(input, "SKU CODE");
        String set = getStringValue(input, "Sequence Set");
        String priority = getStringValue(input, "PRIORITY");
        String startDate = getStringValue(input, "startDate");
        String endDate = getStringValue(input, "endDate");

        List<Producttag> existingTags = productTagService.findBySkuCodeAndTagGroup(skuCode,set);
        Producttag pTag;
        if(!existingTags.isEmpty()){
            pTag =  existingTags.get(0);
        }
        else{
            pTag = new Producttag();
            pTag.setSkuCode(skuCode);
        }
        pTag.setStartDate(parseInputDate(startDate));
        pTag.setEndDate(parseInputDate(endDate));
        pTag.setTagGroup(set);
        pTag.setTagCode(priority);
        pTag.setTagDescription(DESCRIPTION.concat("-product"));
        pTag.setProductValue("PRODUCT");

        return productTagService.save(pTag);
    }
}