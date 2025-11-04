package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.utils.NullUtils;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;

import java.math.BigDecimal;
import java.util.*;

public class CreditOutletTransformerCokeSA extends AbstractTransformer<Map<String,Object>, List<Map<String, Object>>> {
    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createResponse(inputMap));
        return responseList;
    }
    private Map<String, Object> createResponse(Map <String, Object> inputMap){
        Map<String, Object> response = new HashMap<>();
        if(NullUtils.isNull(inputMap.get("OM02_OUTNUM"))) throw new DataTransformationService.TransformationException("OutletCode cannot be null");
        if(Integer.parseInt(inputMap.get("OM02_CRDDAYCOD").toString())!=1 && Integer.parseInt(inputMap.get("OM02_CRDDAYCOD").toString())!=2) throw new DataTransformationService.TransformationException("Credit day code should be 1 or 2");
        response.put("outletCode", inputMap.get("OM02_OUTNUM").toString().replaceAll("\\.0$", ""));
        response.put("baseCreditLimit", NullUtils.isNotNull(inputMap.get("OM02_CRDLIM"))?inputMap.get("OM02_CRDLIM").toString():null);
        response.put("currentCreditLimit", NullUtils.isNotNull(inputMap.get("OM02_CRDLIM"))?inputMap.get("OM02_CRDLIM").toString():null);
        response.put("creditDays", (NullUtils.isNotNull(inputMap.get("OM02_CRDDAY")) && Integer.parseInt(inputMap.get("OM02_CRDDAY").toString())>0)?inputMap.get("OM02_CRDDAY").toString():0);
        response.put("invoiceCount", (NullUtils.isNotNull(inputMap.get("OM02_OPNINVNUM")) && Integer.parseInt(inputMap.get("OM02_OPNINVNUM").toString())>0)?inputMap.get("OM02_OPNINVNUM").toString():0);
        response.put("creditDayCode", NullUtils.isNotNull(inputMap.get("OM02_CRDDAYCOD")) ?inputMap.get("OM02_CRDDAYCOD").toString():null);
        if(NullUtils.isNotNull(inputMap.get("AR12_OPNBAL"))){
            response.put("availableCredit",(Integer.parseInt(inputMap.get("AR12_OPNBAL").toString())<=Integer.parseInt(inputMap.get("OM02_CRDLIM").toString()))
                    ?   (Integer.parseInt(inputMap.get("OM02_CRDLIM").toString())-Integer.parseInt(inputMap.get("AR12_OPNBAL").toString()))
                    :   0);
        }
        response.put("id", UUID.randomUUID().toString());
        response.put("activeStatus", ActiveStatus.ACTIVE.name());
        return response;
    }
}
