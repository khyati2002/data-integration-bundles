//package com.applicate.simamy.validation;
//
//import com.applicate.services.channelkart.models.ProductMetaData;
//import com.applicate.services.channelkart.validations.AbstractRule;
//import com.applicate.services.channelkart.validations.RuleResult;
//import com.applicate.services.channelkart.validations.Status;
//
//public class SimaBasePriceValidation extends AbstractRule<ProductMetaData> {
//    @Override
//    public RuleResult apply(ProductMetaData productMetaData) {
//        if(productMetaData.getMrp().equals(null)){
//            return new RuleResult(Status.ERROR,"mrp is null");
//        }
//        return RuleResult.OK;
//    }
//}
