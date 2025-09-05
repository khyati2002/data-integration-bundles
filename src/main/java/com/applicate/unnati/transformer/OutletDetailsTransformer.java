package com.applicate.unnati.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.LinkedHashMap;
import java.util.Map;

public class OutletDetailsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> outletDetailsMap = new LinkedHashMap<>();

        outletDetailsMap.put("id", inputMap.get("id"));
        outletDetailsMap.put("activeStatus", inputMap.get("activeStatus"));
        outletDetailsMap.put("activeStatusReason", inputMap.get("activeStatusReason"));
        outletDetailsMap.put("createdBy", inputMap.get("createdBy"));
        outletDetailsMap.put("creationTime", inputMap.get("creationTime"));
        outletDetailsMap.put("extendedAttributes", inputMap.get("extendedAttributes"));
        outletDetailsMap.put("lastModifiedTime", inputMap.get("lastModifiedTime"));
        outletDetailsMap.put("lob", inputMap.get("lob"));
        outletDetailsMap.put("modifiedBy", inputMap.get("modifiedBy"));
        outletDetailsMap.put("version", inputMap.get("version"));
        outletDetailsMap.put("address", inputMap.get("address"));
        outletDetailsMap.put("beat", inputMap.get("beat"));
        outletDetailsMap.put("beatName", inputMap.get("beatName"));
        outletDetailsMap.put("channel", inputMap.get("channel"));
        outletDetailsMap.put("contactName", inputMap.get("contactName"));
        outletDetailsMap.put("contactno", inputMap.get("contactno"));
        outletDetailsMap.put("displayAddress", inputMap.get("displayAddress"));
        outletDetailsMap.put("frequency", inputMap.get("frequency"));
        outletDetailsMap.put("mapped", inputMap.get("mapped"));
        outletDetailsMap.put("outletcode", inputMap.get("outletcode"));
        outletDetailsMap.put("outletName", inputMap.get("outletName"));
        outletDetailsMap.put("outletType", inputMap.get("outletType"));
        outletDetailsMap.put("locationHierarchy", inputMap.get("locationHierarchy"));
        outletDetailsMap.put("loginid", inputMap.get("loginid"));
        outletDetailsMap.put("source", inputMap.get("source"));
        outletDetailsMap.put("lastOrderDate", inputMap.get("lastOrderDate"));
        outletDetailsMap.put("latitude", inputMap.get("latitude"));
        outletDetailsMap.put("longitude", inputMap.get("longitude"));
        outletDetailsMap.put("account", inputMap.get("account"));
        outletDetailsMap.put("gstNo", inputMap.get("gstNo"));
        outletDetailsMap.put("marketId", inputMap.get("marketId"));
        outletDetailsMap.put("marketName", inputMap.get("marketName"));
        outletDetailsMap.put("outletCategory", inputMap.get("outletCategory"));
        outletDetailsMap.put("outletClass", inputMap.get("outletClass"));
        outletDetailsMap.put("tinNo", inputMap.get("tinNo"));
        outletDetailsMap.put("hash", inputMap.get("hash"));
        outletDetailsMap.put("coordinate", inputMap.get("coordinate"));
        outletDetailsMap.put("doo", inputMap.get("doo"));
        outletDetailsMap.put("hierarchy", inputMap.get("hierarchy"));
        outletDetailsMap.put("rowid", inputMap.get("rowid"));
        outletDetailsMap.put("changed", inputMap.get("changed"));
        outletDetailsMap.put("outletDivision", inputMap.get("outletDivision"));
        outletDetailsMap.put("distributionChannel", inputMap.get("distributionChannel"));
        outletDetailsMap.put("soldTo", inputMap.get("soldTo"));
        outletDetailsMap.put("subChannel", inputMap.get("subChannel"));
        outletDetailsMap.put("subTerritory", inputMap.get("subTerritory"));
        outletDetailsMap.put("email", inputMap.get("email"));
        outletDetailsMap.put("blobKey", inputMap.get("blobKey"));
        outletDetailsMap.put("controlGroup", inputMap.get("controlGroup"));
        outletDetailsMap.put("normalizedHierarchy", inputMap.get("normalizedHierarchy"));
        outletDetailsMap.put("priceListId", inputMap.get("priceListId"));
        outletDetailsMap.put("prodauthcode", inputMap.get("prodauthcode"));
        outletDetailsMap.put("outletWhatsappNumber", inputMap.get("outletWhatsappNumber"));
        outletDetailsMap.put("dateOfClosing", inputMap.get("dateOfClosing"));
        outletDetailsMap.put("vpo", inputMap.get("vpo"));
        outletDetailsMap.put("segment", inputMap.get("segment"));
        outletDetailsMap.put("outletAttr1", inputMap.get("outletAttr1"));
        outletDetailsMap.put("outletAttr2", inputMap.get("outletAttr2"));
        outletDetailsMap.put("outletAttr3", inputMap.get("outletAttr3"));
        outletDetailsMap.put("outletAttr4", inputMap.get("outletAttr4"));
        outletDetailsMap.put("outletAttr5", inputMap.get("outletAttr5"));
        outletDetailsMap.put("outletAttr6", inputMap.get("outletAttr6"));
        outletDetailsMap.put("fssaiNumber", inputMap.get("fssaiNumber"));
        outletDetailsMap.put("paymentMode", inputMap.get("paymentMode"));
        outletDetailsMap.put("salesMode", inputMap.get("salesMode"));
        outletDetailsMap.put("tcsEligibility", inputMap.get("tcsEligibility"));
        outletDetailsMap.put("keyAccount", inputMap.get("keyAccount"));
        outletDetailsMap.put("outletDiscount", inputMap.get("outletDiscount"));
        outletDetailsMap.put("discountGroup", inputMap.get("discountGroup"));
        outletDetailsMap.put("shipToAddress", inputMap.get("shipToAddress"));

        outletDetailsMap.put("immediateParent", inputMap.get("immediateParent"));
        outletDetailsMap.put("userName", inputMap.get("userName"));
        outletDetailsMap.put("location", inputMap.get("location"));

        return outletDetailsMap;
    }
}
