package com.applicate.cokesa.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.enums.DmsVanItemsActiveStatus;
import com.salescode.dim.jooq.generated.enums.DmsVanItemsItemType;
import com.salescode.dim.jooq.generated.enums.DmsVanLoadoutActiveStatus;
import com.salescode.dim.jooq.generated.enums.DmsVanLoadoutLoadOutStatus;
import com.salescode.dim.jooq.generated.tables.pojos.DmsVanLoadout;
import com.salescode.dim.jooq.impl.VanItems;

import java.math.BigDecimal;
import java.util.*;

public class VanLoadoutTransformerCokeSA extends AbstractTransformer<Map<String,Object>,List<Map<String,Object>>> {
    @Override
    public List<Map<String,Object>> transform(Map<String,Object> inputMap) {
        List<Map<String,Object>> responseList=new ArrayList<>();
        responseList.add(createResponse(inputMap));
        return  responseList;
    }

    private DmsVanLoadout buildDMSVanLoadout(Map<String,Object> vanLoadoutInput){
        DmsVanLoadout dmsVanLoadout=new DmsVanLoadout();

        dmsVanLoadout.setLoadNumber(vanLoadoutInput.get("loadNumber").toString());
        dmsVanLoadout.setSalesmanId(vanLoadoutInput.get("salesmanId").toString());
        dmsVanLoadout.setSupplier(vanLoadoutInput.get("supplier").toString());
        dmsVanLoadout.setVehicleId(vanLoadoutInput.get("vehicleId").toString());
        dmsVanLoadout.setRouteCode(Collections.singletonList(vanLoadoutInput.get("routeCode").toString()));
        dmsVanLoadout.setTotalCaseQty(((Number)vanLoadoutInput.get("totalCaseQty")).doubleValue());
        dmsVanLoadout.setTotalCaseLeftQty(((Number)vanLoadoutInput.get("totalCaseLeftQty")).doubleValue());
        dmsVanLoadout.setTotalSuggestedCaseQty(((Number)vanLoadoutInput.get("totalCaseLeftQty")).doubleValue());
        dmsVanLoadout.setTotalAcceptedCaseQty(0.0);
        dmsVanLoadout.setTotalPieceQty(0.0);
        dmsVanLoadout.setTotalPieceLeftQty(0.0);
        dmsVanLoadout.setTotalSuggestedPieceQty(0.0);
        dmsVanLoadout.setTotalAcceptedPieceQty(0.0);
        dmsVanLoadout.setTotalOtherQty(0.0);
        dmsVanLoadout.setTotalOtherLeftQty(0.0);
        dmsVanLoadout.setTotalSuggestedOtherQty(0.0);
        dmsVanLoadout.setTotalAcceptedOtherQty(0.0);
        dmsVanLoadout.setLoadOutStatus(DmsVanLoadoutLoadOutStatus.IN_TRANSIT);
        dmsVanLoadout.setActiveStatus(DmsVanLoadoutActiveStatus.ACTIVE);
        dmsVanLoadout.setTotalAmount(BigDecimal.ZERO);

        return dmsVanLoadout;
    }

    private List<VanItems> buildVanItems(List<Map<String,Object>> vanItemsInputList,String loadNumber){
        List<VanItems> vanItemsList=new ArrayList<>();

        for(Map<String,Object> vanItemInput:vanItemsInputList){
            VanItems vanItems=new VanItems();
            vanItems.setSkuCode(vanItemInput.get("skuCode").toString());
            vanItems.setCaseQty(((Number)vanItemInput.get("caseQty")).doubleValue());
            vanItems.setCaseQtyLeft(((Number)vanItemInput.get("caseQtyLeft")).doubleValue());
            vanItems.setPieceQty(0.0);
            vanItems.setPieceQtyLeft(0.0);
            vanItems.setOtherQty(0.0);
            vanItems.setOtherQtyLeft(0.0);
            vanItems.setItemType(DmsVanItemsItemType.NORMAL);
            vanItems.setLoadNumber(loadNumber);
            vanItems.setActiveStatus(DmsVanItemsActiveStatus.ACTIVE);


            vanItemsList.add(vanItems);
        }
        return vanItemsList;
    }



    private Map<String,Object> createResponse(Map<String,Object> inputMap){
        Map<String,Object> response=new LinkedHashMap<>();
        String loadNumber= ((Map<String, Object>) inputMap.get("dmsVanLoadout")).get("loadNumber").toString();

        response.put("dmsVanLoadout",buildDMSVanLoadout((Map<String, Object>) inputMap.get("dmsVanLoadout")));
        response.put("vanItemsList",buildVanItems((List<Map<String, Object>>) inputMap.get("vanItemsList"),loadNumber));

        return response;
    }

}
