package com.applicate.niine.validation;

import com.applicate.services.channelkart.models.ProductDetails;
import com.applicate.services.channelkart.models.SecondaryProduct;
import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;

import java.util.List;

public class SecondaryProductNiineValidator extends AbstractRule<SecondaryProduct> {

    @Override
    public RuleResult apply(SecondaryProduct cdm) {
        ProductDetailsService productDetails = SpringContext.getBean(ProductDetailsService.class);
        StringBuilder ruleResult = new StringBuilder();
        if(! validLocation(cdm)){
            ruleResult.append("Combined Zone and Region value not found in Location Table \n");
        }

        String product = cdm.getProduct();

        if(product.isEmpty()){
            ruleResult.append("ProductCode cannot be empty \n ");
        }else{
            if(validProductCode(product,productDetails)){
                ruleResult.append("No data found for the ProductCode ").append(product);
            }
        }

        if (ruleResult.toString().isEmpty()) {
            return RuleResult.OK;
        }

        return new RuleResult(Status.ERROR, ruleResult.toString());
    }

    private boolean validLocation(SecondaryProduct cdm){
        return cdm.getLocationHierarchy() != null;
    }

    private boolean validProductCode(String product,ProductDetailsService service){
        List<ProductDetails> productDetails = service.findByProductCodeAndActiveStatus(product, ActiveStatus.ACTIVE);
        return productDetails.isEmpty();

    }
}
