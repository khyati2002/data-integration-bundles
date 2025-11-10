package com.applicate.cokeph.transformer;

import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CokephStockMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {


    @Override
    public Map<String, Object> transform(Map<String, Object> stringObjectMap) {
        HashMap<String, Object> finalTransformedObj = new HashMap<>();
        finalTransformedObj.put("skuCode", stringObjectMap.get("ProductId").toString());
        finalTransformedObj.put("batchCode", stringObjectMap.get("ProductId").toString());

        List<String> validDistributorCodes = Arrays.asList("0502615941", "0503558429","0502359930","0502148945","0505353136","0502148915","0504905749","0505421116","0505285873");
        String distributorCode = (String) stringObjectMap.get("DistributorId");
        if (NullUtils.isNull(distributorCode) || StringUtils.isEmpty(distributorCode)) {
            throw new DataTransformationService.TransformationException("distributor_code is null");
        }
        if (!validDistributorCodes.contains(distributorCode)) {
            throw new DataTransformationService.TransformationException("This distributor does not exist as a part of our onboarding plan");
        }
        finalTransformedObj.put("supplier",distributorCode );
        finalTransformedObj.put("initialQty", stringObjectMap.get("QuantityForCase").toString());
        finalTransformedObj.put("qty", stringObjectMap.get("QuantityForEach").toString());


        return finalTransformedObj;
    }
}
