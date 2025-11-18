package com.applicate.alsafi.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OutletTransformer extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {
    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> outletMap = new HashMap<>();
        outletMap.put("outletCode", inputMap.get("CUSTOMERID"));
        outletMap.put("activeStatus", inputMap.get("ISACTIVE").toString().equals("1") ? "ACTIVE" :  "INACTIVE");
        outletMap.put("address", inputMap.get("ADDRESS"));
        outletMap.put("contactName", inputMap.get("CUSTOMERNAME"));
        outletMap.put("contactno", inputMap.get("CONTACTMOBILE"));
        outletMap.put("displayAddress", inputMap.get("ADDRESS"));
        outletMap.put("outletName", inputMap.get("CUSTOMERNAME"));
        outletMap.put("channel", inputMap.get("CHANNELNAME"));
        outletMap.put("latitude", inputMap.get("LATITUDE"));
        outletMap.put("longitude", inputMap.get("LONGITUDE"));
        outletMap.put("outletClass", inputMap.get("OUTLETCLASS"));
        outletMap.put("distributionChannel", inputMap.get("DISTRIBUTIONCHANNEL"));
        outletMap.put("outletType", inputMap.get("OUTLETTYPE"));
        outletMap.put("beat", inputMap.get("ROUTECODE"));
        outletMap.put("beatName", inputMap.get("ROUTENAME"));
        Map<String, Object> user = new HashMap<>();
        user.put("loginId", inputMap.get("ROUTECODE"));
        outletMap.put("userName", user);
        Map<String, Object> extendedAttributes = new HashMap<>();
        extendedAttributes.put("channelCode", inputMap.get("CHANNELCODE"));
        extendedAttributes.put("distributor",inputMap.get("DISTRIBUTORCODE"));
        extendedAttributes.put("vatNo",inputMap.get("vatNo"));
        outletMap.put("extendedAttributes", extendedAttributes);
        Map<String, Object> outletHierarchy = new HashMap<>();
        outletHierarchy.put("immediateParent", inputMap.get("ROUTECODE"));
        outletMap.put("immediateParent", List.of(outletHierarchy));
        Map<String, Object> location = new HashMap<>();
        location.put("country", "Saudi Arabia");
        location.put("city",  inputMap.get("CITY"));
        location.put("district",  inputMap.get("district"));
        outletMap.put("location", location);
        return outletMap;
    }
}