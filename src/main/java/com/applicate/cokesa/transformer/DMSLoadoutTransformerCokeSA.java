package com.applicate.cokesa.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.generated.enums.DmsLoadoutDetailsLoadOutStatus;
import com.salescode.dim.jooq.generated.enums.DmsLoadoutItemsItemType;
import com.salescode.dim.jooq.generated.enums.DmsLoadoutLoadOutStatus;
import com.salescode.dim.jooq.generated.enums.DmsLoadoutLoadOutType;
import com.salescode.dim.jooq.generated.tables.pojos.DmsLoadout;
import com.salescode.dim.jooq.impl.LoadoutDetails;
import com.salescode.dim.jooq.impl.LoadoutItems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMSLoadoutTransformerCokeSA  extends AbstractTransformer<Map<String,Object>, List<Map<String, Object>>> {
    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inputMap) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        responseList.add(createResponse(inputMap));
        return responseList;
    }

    private DmsLoadout buildDMSLoadout(Map<String,Object> dmsLoadoutInput){
        DmsLoadout dmsLoadout=new DmsLoadout();
        dmsLoadout.setLoadNumber((String) dmsLoadoutInput.get("loadNumber"));
        dmsLoadout.setSalesmanId((String) dmsLoadoutInput.get("salesmanId"));
        dmsLoadout.setVehicleId((String) dmsLoadoutInput.get("vehicleId"));
        dmsLoadout.setTotalCaseQty( ((Number) dmsLoadoutInput.get("totalCaseQty")).doubleValue());
        dmsLoadout.setTotalCaseLeftQty(((Number) dmsLoadoutInput.get("totalCaseLeftQty")).doubleValue());
        dmsLoadout.setLoadOutStatus(DmsLoadoutLoadOutStatus.IN_DRAFT);
        dmsLoadout.setLoadOutType(DmsLoadoutLoadOutType.PRE_SELLER);
        return dmsLoadout;
    }
    private List<LoadoutDetails> buildLoadoutDetailsList(List<Map<String,Object>> loadOutDetailsListInput,String loadNumber,String activityRoute){
        List<LoadoutDetails> loadoutDetailsList=new ArrayList<>();
        for(Map<String,Object> loadOutDetailsInput:loadOutDetailsListInput){
            LoadoutDetails loadoutDetails=new LoadoutDetails();
            loadoutDetails.setInvoiceNumber((String) loadOutDetailsInput.get("invoiceNumber"));
            loadoutDetails.setOutletCode((String) loadOutDetailsInput.get("outletCode"));
            loadoutDetails.setPresellerId((String) loadOutDetailsInput.get("presellerId"));
            loadoutDetails.setTotalCaseQty(((Number) loadOutDetailsInput.get("totalCaseQty")).doubleValue());
            loadoutDetails.setTotalCaseLeftQty(((Number) loadOutDetailsInput.get("totalCaseLeftQty")).doubleValue());
            loadoutDetails.setLoadNumber(loadNumber);
            loadoutDetails.setRouteCode(activityRoute);
            loadoutDetails.setLoadOutStatus(DmsLoadoutDetailsLoadOutStatus.IN_DRAFT);

            String loadOutDetailsId=loadNumber+"_"+loadoutDetails.getInvoiceNumber();
            loadoutDetails.setLoadoutItems(buildLoadOutItemsList((List<Map<String, Object>>) loadOutDetailsInput.get("loadoutItems"),loadOutDetailsId));

            loadoutDetailsList.add(loadoutDetails);
        }
        return loadoutDetailsList;
    }

    private List<LoadoutItems> buildLoadOutItemsList(List<Map<String,Object>> loadOutItemsListInput,String loadOutDetailsId){
        List<LoadoutItems> loadoutItemsList=new ArrayList<>();

        for(Map<String,Object> loadOutItemInput:loadOutItemsListInput){
            LoadoutItems loadoutItems=new LoadoutItems();
            loadoutItems.setSkuCode((String) loadOutItemInput.get("skuCode"));
            loadoutItems.setCaseQty(((Number) loadOutItemInput.get("caseQty")).doubleValue());
            loadoutItems.setCaseQtyLeft(((Number) loadOutItemInput.get("caseQtyLeft")).doubleValue());
            loadoutItems.setItemType(DmsLoadoutItemsItemType.NORMAL);

            if(loadOutDetailsId.length()<=255){
                loadoutItems.setLoadOutDetailsId(loadOutDetailsId);
            }

            loadoutItemsList.add(loadoutItems);
        }
        return loadoutItemsList;
    }



    private Map<String, Object> createResponse(Map<String, Object> inputMap) {
        Map<String,Object> response=new HashMap<>();
        String loadNumber= ((Map<String, Object>) inputMap.get("dmsLoadout")).get("loadNumber").toString();
        String[] parts=loadNumber.split("_");
        String activityRoute=parts[3];
        response.put("dmsLoadout",buildDMSLoadout((Map<String, Object>) inputMap.get("dmsLoadout")));
        response.put("loadoutDetailsList",buildLoadoutDetailsList(((List<Map<String, Object>>) inputMap.get("loadoutDetailsList")),loadNumber,activityRoute));
        return response;
    }

}
