package com.applicate.unnati.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class OutletDetailTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        if (inputMap == null) return Collections.emptyMap();
        Map<String, Object> outletDetailsMap = new LinkedHashMap<>();

        outletDetailsMap.put("tcsEligibility", inputMap.get("tcsEligibility"));
        outletDetailsMap.put("userName", inputMap.get("userName"));
        outletDetailsMap.put("priceListId", inputMap.get("priceListId"));
        outletDetailsMap.put("prodauthcode", inputMap.get("prodauthcode"));
        outletDetailsMap.put("paymentMode", inputMap.get("paymentMode"));
        outletDetailsMap.put("keyAccount", inputMap.get("keyAccount"));
        outletDetailsMap.put("activeStatusReason", inputMap.get("activeStatusReason"));
        outletDetailsMap.put("address", inputMap.get("address"));
        outletDetailsMap.put("soldTo", inputMap.get("soldTo"));
        outletDetailsMap.put("activeStatus", inputMap.get("activeStatus"));
        outletDetailsMap.put("account", inputMap.get("account"));
        outletDetailsMap.put("discountGroup", inputMap.get("discountGroup"));
        outletDetailsMap.put("location", inputMap.get("location"));
        outletDetailsMap.put("beatName", inputMap.get("beatName"));
        outletDetailsMap.put("outletCategory", inputMap.get("outletCategory"));
        outletDetailsMap.put("vpo", inputMap.get("vpo"));
        outletDetailsMap.put("retailerInfo", inputMap.get("retailerInfo"));
        outletDetailsMap.put("marketName", inputMap.get("marketName"));
        outletDetailsMap.put("subTerritory", inputMap.get("subTerritory"));
        outletDetailsMap.put("marketId", inputMap.get("marketId"));
        outletDetailsMap.put("salesMode", inputMap.get("salesMode"));
        outletDetailsMap.put("outletType", inputMap.get("outletType"));
        outletDetailsMap.put("outletDiscount", inputMap.get("outletDiscount"));
        outletDetailsMap.put("serialVersionUID", inputMap.get("serialVersionUID"));
        outletDetailsMap.put("latitude", inputMap.get("latitude"));
        outletDetailsMap.put("doo", inputMap.get("doo"));
        outletDetailsMap.put("outletDivision", inputMap.get("outletDivision"));
        outletDetailsMap.put("shipToAddress", inputMap.get("shipToAddress"));
        outletDetailsMap.put("email", inputMap.get("email"));
        outletDetailsMap.put("controlGroup", inputMap.get("controlGroup"));
        outletDetailsMap.put("outletWhatsappNumber", inputMap.get("outletWhatsappNumber"));
        outletDetailsMap.put("tinNo", inputMap.get("tinNo"));
        outletDetailsMap.put("users", inputMap.get("users"));
        outletDetailsMap.put("geoDist", inputMap.get("geoDist"));
        outletDetailsMap.put("shiptoOutlets", inputMap.get("shiptoOutlets"));
        outletDetailsMap.put("contactName", inputMap.get("contactName"));
        outletDetailsMap.put("hierarchy", inputMap.get("hierarchy"));
        outletDetailsMap.put("salesReps", inputMap.get("salesReps"));
        outletDetailsMap.put("immediateParent", inputMap.get("immediateParent"));
        outletDetailsMap.put("coordinate", inputMap.get("coordinate"));
        outletDetailsMap.put("outletCode", inputMap.get("outletCode"));
        outletDetailsMap.put("normalizedHierarchy", inputMap.get("normalizedHierarchy"));
        outletDetailsMap.put("mapped", inputMap.get("mapped"));
        outletDetailsMap.put("displayAddress", inputMap.get("displayAddress"));
        outletDetailsMap.put("blobKey", inputMap.get("blobKey"));
        outletDetailsMap.put("outletClass", inputMap.get("outletClass"));
        outletDetailsMap.put("frequency", inputMap.get("frequency"));
        outletDetailsMap.put("fssaiNumber", inputMap.get("fssaiNumber"));
        outletDetailsMap.put("lastOrderDate", inputMap.get("lastOrderDate"));
        outletDetailsMap.put("channel", inputMap.get("channel"));
        outletDetailsMap.put("gstNo", inputMap.get("gstNo"));
        outletDetailsMap.put("contactno", inputMap.get("contactno"));
        outletDetailsMap.put("longitude", inputMap.get("longitude"));
        outletDetailsMap.put("distributionChannel", inputMap.get("distributionChannel"));
        outletDetailsMap.put("dateOfClosing", inputMap.get("dateOfClosing"));
        outletDetailsMap.put("outletAttr1", inputMap.get("outletAttr1"));
        outletDetailsMap.put("outletAttr2", inputMap.get("outletAttr2"));
        outletDetailsMap.put("outletAttr3", inputMap.get("outletAttr3"));
        outletDetailsMap.put("outletAttr4", inputMap.get("outletAttr4"));
        outletDetailsMap.put("outletAttr5", inputMap.get("outletAttr5"));
        outletDetailsMap.put("outletAttr6", inputMap.get("outletAttr6"));
        outletDetailsMap.put("beat", inputMap.get("beat"));
        outletDetailsMap.put("subChannel", inputMap.get("subChannel"));
        outletDetailsMap.put("segment", inputMap.get("segment"));
        outletDetailsMap.put("source", inputMap.get("source"));
        outletDetailsMap.put("outletName", inputMap.get("outletName"));

        return outletDetailsMap;
    }
}