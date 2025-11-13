package com.applicate.cokeph.transformer;

import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.OutletMetadataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.tables.pojos.OutletMetadata;
import com.salescode.dim.jooq.impl.OutletDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CokephOutletPricingTransfomer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    final OutletDetailsService outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
    final OutletMetadataService outletMetadataService = (OutletMetadataService) ServiceLocator.lookup(OutletMetadata.class);


    @Override
    public Map<String, Object> transform(Map<String, Object> stringObjectMap) {
        HashMap<String, Object> finalTransformedObj = new HashMap<>();
        String name = (String) stringObjectMap.getOrDefault("name", "outletpricingcode");
        String outletCode = (String) stringObjectMap.get("OutletCode");
        String id = (outletCode != null ? outletCode : "") + "-" + name;
        OutletDetails outletDetails=outletDetailsService.findByOutletCode(outletCode);
        if(outletDetails==null){
            Optional<String> outletCodeOptional= outletMetadataService.getOutletCodeIfExists(outletCode);
            if(outletCodeOptional.isPresent()){
                outletCode=outletCodeOptional.get();
            }
        }

        finalTransformedObj.put("id",id);
        finalTransformedObj.put("name", name);
        if (stringObjectMap.containsKey("OutletCode")) {
            finalTransformedObj.put("key2", outletCode);
        }

        if (stringObjectMap.containsKey("PricingCode")) {
            finalTransformedObj.put("key1", stringObjectMap.get("PricingCode"));
        }
        return finalTransformedObj;
    }
}
