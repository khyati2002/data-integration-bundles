package com.applicate.validation;

import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.validations.repository.RegexValidation;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.impl.ProductMetaData;
import com.salescode.dim.jooq.impl.User;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VistaarProductMetadataValidator extends AbstractValidationRule<ProductMetaData> {
    String decimalRegex = "^([0-9]*\\.)[0-9]+$";
    String integerRegex = "(^[0-9]*$)";
    private static final String CASEPTR="casePtr";
    private static final String PACKPTR="packPtr";
    private static final String CASEPTS="CFCPTS";
    private static final String PACKPTS="PACPTS";
    private static final String SBU="SBU";
    private static final String SUB_CAT_CODE="SubCatCode";
    private static final String SUB_CAT_NAME="SubCatName";
    private static final String MKTSKUCODE="MktSkuCode";
    private static final String MKTSKUNAME="MktSkuName";
    private static final String BRANDCODE="BrandCode";
    private static final String BRANDNAME="BrandName";
    private static final String CATCODE="CatCode";
    private static final String CATNAME="CatName";

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private UserService userService = (UserService) ServiceLocator.lookup(User.class);
    private ProductDetailsService productDetailsService = (ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);

    @Override
    public OperationResult.StepResult apply(ProductMetaData cdm) {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlank(cdm.getSkuCode())) {
            errors.add("skuCode should not be null or empty and present in Product metadata");
        }

        if(StringUtils.isBlank(cdm.getBatchCode())) {
            errors.add("batchcode should not be null or empty and present in Product metadata");
        }

        if (cdm.getCasePtr()==null || cdm.getCasePtr().compareTo(BigDecimal.ZERO) == 0) {
            errors.add(CASEPTR + " should not be null or empty in Product metadata");
        } else {
            if (!validatePattern(decimalRegex, String.valueOf(cdm.getCasePtr()))) {
                errors.add("only decimal value are allowed in " +CASEPTR);
            }
        }

        if (cdm.getPackPtr()==null || cdm.getPackPtr().compareTo(BigDecimal.ZERO) == 0) {
            errors.add(PACKPTR +" should not be null or empty in Product metadata");
        } else {
            if (!validatePattern(decimalRegex, String.valueOf(cdm.getPackPtr()))) {
                errors.add("only decimal value are allowed in "+ PACKPTR);
            }
        }

        if (cdm.getMrp()==null || cdm.getMrp().compareTo(BigDecimal.ZERO) == 0) {
            errors.add("mrp should not be null or empty in Product metadata");
        } else {
            if (!validatePattern(decimalRegex, String.valueOf(cdm.getMrp())) &&  !validatePattern(integerRegex, String.valueOf(cdm.getMrp()))) {
                errors.add("only decimal or integer values are allowed in mrp");
            }
        }

        if (StringUtils.isBlank(cdm.getTax())) {
            errors.add("tax should not be null or empty in Product metadata");
        } else {
            if (!validatePattern(decimalRegex, String.valueOf(cdm.getTax())) &&  !validatePattern(integerRegex, String.valueOf(cdm.getTax()))) {
                errors.add("only decimal or integer values allowed in tax");
            }
        }

        JsonNode extendAttr = cdm.getExtendedAttributes();

        //non-mandatory fields - mentioned by nikhil
        if (extendAttr.has(CASEPTS)) {
            String valueAttr = extendAttr.get(CASEPTS).asText();
            if (StringUtils.isBlank(valueAttr) || valueAttr.equalsIgnoreCase("null")) {
                errors.add("CASEPTR should not be null or empty");
            }
            if (!validatePattern(decimalRegex, valueAttr) && !validatePattern(integerRegex, valueAttr)) {
                errors.add("only decimal value are allowed in" + CASEPTS);
            }
        }

        if (extendAttr.has(PACKPTS)) {
            String valueAttr = extendAttr.get(PACKPTS).asText();
            if (StringUtils.isBlank(valueAttr) || valueAttr.equalsIgnoreCase("null")) {
                errors.add("PACPTR should not be null or empty");
            }
            if (!validatePattern(decimalRegex, valueAttr) && !validatePattern(integerRegex, valueAttr)) {
                errors.add("only decimal value are allowed in " + PACKPTS);
            }
        }

        if (extendAttr.has("PACIn1CFC")) {
            String valueAttr = extendAttr.get("PACIn1CFC").asText();
            if (StringUtils.isBlank(valueAttr) || valueAttr.equalsIgnoreCase("null")) {
                errors.add("PACIn1CFC should not be null or empty");
            }
            try{
                if (Float.parseFloat(valueAttr)%1!=0) {
                    errors.add("only int value are allowed in PACIn1CFC");
                }
            }catch (NumberFormatException e){
                errors.add("only int value are allowed in PACIn1CFC");
            }
        }

        if(!ObjectUtils.isEmpty(extendAttr)) {
            if(extendAttr.has(SBU)) {
                String sbu = extendAttr.get(SBU).asText();
                if (StringUtils.isBlank(sbu) || sbu.equalsIgnoreCase("null")) {
                    errors.add("SBU should not be null or empty");
                }
            }
            else {
                errors.add(SBU + "cannot be null or empty");
            }

        }
        else {
            errors.add(SBU + " fields missing");
        }

        if (cdm.getLoginid() == null) {
            errors.add("supplier should not be null or empty");
        } else {
            User user = userService.findByLoginId(cdm.getLoginid());
            if (user == null) {
                errors.add("supplier not present in database " +cdm.getLoginid());
            }
            else if(!user.hasDesignation("wd")){
                errors.add("supplier is not wd");
            }
        }

        ProductDetails productDetails = productDetailsService.findByBatchCode(cdm.getBatchCode());
        if(productDetails == null) {
            errors.add("product details not found in database for batchcode " +cdm.getBatchCode());
        }
        else {
            String error = validateMismatchProdDetails(productDetails, cdm);
            error+= validateOtherFields(cdm);
            if(error.length() > 0) {
                errors.add(error);
            }
        }

        if (errors.size() > 0) {
            String errorstr = String.format(
                    "Validation error occurred for Product Metadata. Kindly go through provided errors and make sure those conditions should fulfill while retrying. %s",
                    StringUtils.join(errors, ", "));
            logger.info(errorstr);
            return new OperationResult.StepResult(OperationResult.Status.ERROR, errorstr);
        }
        return OperationResult.StepResult.OK;
    }

    private String validateMismatchProdDetails(ProductDetails productDetails, ProductMetaData cdm) {
        JsonNode extAttr = cdm.getExtendedAttributes();
        String brandCode = productDetails.getBrandCode();
        String brand = productDetails.getBrand();
        StringBuilder error= new StringBuilder();
        if(!StringUtils.isBlank(getValue(extAttr, BRANDCODE)) && !getValue(extAttr, BRANDCODE).equalsIgnoreCase(brandCode)) {
            error.append("BrandCode details mismatch with product details. ");
        }
        else if(!StringUtils.isBlank(getValue(extAttr, BRANDNAME)) && !getValue(extAttr, BRANDNAME).equalsIgnoreCase(brand)) {
            error.append("BrandName details mismatch with product details. " );
        }

        String CatCode = productDetails.getCategoryCode();
        String CatName = productDetails.getCategory();
        if(!StringUtils.isBlank(getValue(extAttr, CATCODE)) && !getValue(extAttr, CATCODE).equalsIgnoreCase(CatCode)) {
            error.append("CatCode details mismatch with product details. " );
        }
        else if(!StringUtils.isBlank(getValue(extAttr, CATNAME)) && !getValue(extAttr, CATNAME).equalsIgnoreCase(CatName)) {
            error.append("CatName details mismatch with product details. " );
        }

        String SubCatCode = productDetails.getSubCategoryCode();
        String SubCatName = productDetails.getSubCategory();
        if(!StringUtils.isBlank(getValue(extAttr,SUB_CAT_CODE)) && !getValue(extAttr, SUB_CAT_CODE).equalsIgnoreCase(SubCatCode)) {
            error.append("SubCatCode details mismatch with product details. " );
        }
        else if(!StringUtils.isBlank(getValue(extAttr, SUB_CAT_NAME)) && !getValue(extAttr, SUB_CAT_NAME).equalsIgnoreCase(SubCatName)) {
            error.append("SubCatName details mismatch with product details. " );
        }

        String MktSkuCode = productDetails.getMarketSkuCode();
        String MktSkuName = productDetails.getMarketSku();
        if(!StringUtils.isBlank(getValue(extAttr, MKTSKUCODE)) && !getValue(extAttr, MKTSKUCODE).equalsIgnoreCase(MktSkuCode)) {
            error.append("MktSkuCode details mismatch with product details. " );
        }
        else if(!StringUtils.isBlank(getValue(extAttr, MKTSKUNAME)) && !getValue(extAttr, MKTSKUNAME).equalsIgnoreCase(MktSkuName)) {
            error.append("MktSkuName details mismatch with product details. " );
        }
        return error.toString();
    }

    private String validateOtherFields(ProductMetaData cdm){
        JsonNode extAttr = cdm.getExtendedAttributes();
        StringBuilder error= new StringBuilder();
        if(extAttr!=null){
            if(StringUtils.isBlank(getValue(extAttr, BRANDCODE))){
                error.append("BrandCode should not be null or empty ");
            }
            if(StringUtils.isBlank(getValue(extAttr, BRANDNAME))){
                error.append("BrandName should not be null or empty ");
            }
            if (StringUtils.isBlank(getValue(extAttr, CATCODE))){
                error.append("CatCode should not be null or empty ");
            }
            if (StringUtils.isBlank(getValue(extAttr, CATNAME))){
                error.append("CatName should not be null or empty ");
            }
            if (StringUtils.isBlank(getValue(extAttr, SUB_CAT_CODE))){
                error.append("SubCatCode should not be null or empty ");
            }
            if (StringUtils.isBlank(getValue(extAttr, SUB_CAT_NAME))){
                error.append("SubCatName should not be null or empty ");
            }
            if (StringUtils.isBlank(getValue(extAttr, MKTSKUCODE))){
                error.append("MktSkuCode should not be null or empty ");
            }
            if (StringUtils.isBlank(getValue(extAttr, MKTSKUNAME))){
                error.append("MktSkuName should not be null or empty ");
            }
            if (StringUtils.isBlank(getValue(extAttr, "SysSkuName"))){
                error.append("SysSkuName should not be null or empty ");
            }
        }
        return error.toString();
    }


    private String getValue(JsonNode extAttr, String key) {
        if(extAttr.has(key)&& extAttr.get(key).asText().equalsIgnoreCase("null")){
            return null;
        }
        return extAttr.has(key)? extAttr.get(key).asText(): null;
    }

    public boolean validatePattern(String pattern, String value) {
        RegexValidation regexValidation = new RegexValidation();
        return regexValidation.match(pattern, value);

    }
}
