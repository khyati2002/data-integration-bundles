package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.models.MetaData;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.MetaDataService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.util.*;

import static com.applicate.services.channelkart.taskexecutors.integrationtaskservices.service.IntegrationTaskService.mapper;

public class SimaOutletDetails extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    GenericEntityService genericEntityService = SpringContext.getBean(GenericEntityService.class);
    MetaDataService metaDataService = SpringContext.getBean(MetaDataService.class);
    static final String PRICE_GROUP = "PRICE_GROUP";
    static final String PRICING_PROCEDURE = "PRICING_PROCEDURE";
    static final String PRICE_LIST_TYPE = "PRICE_LIST_TYPE";
    static final String ADDRESS = "address";
    static final String ACTIVE_STATUS = "activeStatus";
    static final String TRADE_NAME = "tradename";
    static final String SALES_REP_ID = "salesrep_id";
    static final String  OUTLET_ATTR1="outletAttr1";

    @Override
    public Object transform(Map<String, Object> stringObjectMap) {
//        if(ObjectUtils.isEmpty(stringObjectMap.get("phonenumber"))) throw new BadRequestException("phone number is missing");
        ObjectNode extended = new ObjectMapper().createObjectNode();
        HashMap<String, Object> finalTransformedObj = new HashMap<>();
        Object primaryAddress1 = stringObjectMap.get("PrimaryAddress");
        Object classification1 = stringObjectMap.get("Classification");
        MetaData supplier = metaDataService.fetchByValue("MigratedSuppliers","orderSubmit");
        Map<String,String> supplierList = JSONUtils.toStringMap(supplier.getDomainValues().get(0));



        Object pricing1 = stringObjectMap.get("PricingDetails");
        Object partnerFunctions1 = stringObjectMap.get("PartnerFunctions");
        JsonNode classification = JSONUtils.toJsonNode(classification1);
        JsonNode primaryAddress = JSONUtils.toJsonNode(primaryAddress1);
        JsonNode pricing = JSONUtils.toJsonNode(pricing1);
        JsonNode partnerFunctions = JSONUtils.toJsonNode(partnerFunctions1);
        extended.put(ACTIVE_STATUS, setActiveStatus(stringObjectMap));
        finalTransformedObj.put(ACTIVE_STATUS, "inactive");
        String[] optionalFields = "longitude,latitude".split(",");
        for (String fields : optionalFields) {
            if (ObjectUtils.isNotEmpty(primaryAddress.get(fields))) {
                String fieldValue = primaryAddress.get(fields).asText();
                BigDecimal truncatedValue = new BigDecimal(fieldValue).setScale(8, BigDecimal.ROUND_DOWN);
                Double parsedValue = truncatedValue.doubleValue();
                finalTransformedObj.put(fields, parsedValue);
            }
        }
        if (ObjectUtils.isNotEmpty(primaryAddress.get(ADDRESS)))
            finalTransformedObj.put(ADDRESS, primaryAddress.get(ADDRESS).asText());
        String[] mandatoryFields = "accountgroup,trade_channel,sales_office,SalesOrganisationId".split(",");
        for (String fields : mandatoryFields) {
            extended.put(fields, classification.get(fields).asText());
        }
        String[] manField = "ship_condition,companycode,Plant".split(",");
        String[] optField = "PMNTTRMS,blocked_by_credit_limit,currency,ORDR_BLK_G,CUST_GROUP_1,creation_date,Suppressed,TRANSPORTATION_ZONE,contactname".split(",");
        for (String field : manField) {
            extended.put(field, stringObjectMap.get(field).toString());
        }
        String dc = stringObjectMap.get("distributorcode").toString().substring(2, 4);
        String isNewsPage = "N";
        if(supplierList.containsKey(dc.toString()))
            isNewsPage ="Y";
        for (String field : optField) {
            if (ObjectUtils.isNotEmpty(stringObjectMap.get(field)))
                extended.put(field, stringObjectMap.get(field).toString());
        }
        String[] partnerManField = "SoldToCustomerId,ShipToCustomerId,PayerCustomerId,BillToCustomerId".split(",");
        for (String field : partnerManField) {
            extended.put(field, partnerFunctions.get(field).asText());
        }
        if(ObjectUtils.isNotEmpty(partnerFunctions.get(SALES_REP_ID)))
            extended.put(SALES_REP_ID,partnerFunctions.get(SALES_REP_ID).asText());
        Optional.ofNullable(pricing.get(PRICE_LIST_TYPE)).ifPresentOrElse(
                i -> extended.set("priceListId", pricing.get(PRICE_LIST_TYPE)), () -> extended.put("priceListId", "NA"));
        Optional.ofNullable(pricing.get(PRICE_GROUP)).ifPresentOrElse(
                i -> extended.set(PRICE_GROUP, pricing.get(PRICE_GROUP)), () -> extended.put(PRICE_GROUP, "NA"));
        Optional.ofNullable(pricing.get(PRICING_PROCEDURE)).ifPresentOrElse(
                i -> extended.set(PRICING_PROCEDURE, pricing.get(PRICING_PROCEDURE)), () -> extended.put(PRICING_PROCEDURE, "NA"));
        finalTransformedObj.put("outletType", classification.get("accounttype").asText());
        finalTransformedObj.put("distributionChannel", classification.get("distributor_channel").asText());
        if( ObjectUtils.isNotEmpty(classification.get("cust_classification")))
            finalTransformedObj.put("outletClass", classification.get("cust_classification").asText());
        if( ObjectUtils.isNotEmpty(classification.get("PMNTTRMS")))
            extended.put("PMNTTRMS", classification.get("PMNTTRMS").asText());
        if( ObjectUtils.isNotEmpty(classification.get("KEY_ACC_CUST")))
            extended.put("KEY_ACC_CUST", classification.get("KEY_ACC_CUST").asText());
        if( ObjectUtils.isNotEmpty(pricing.get("pricing_attrib")))
            extended.put("pricing_attrib", pricing.get("pricing_attrib").asText());
        if(ObjectUtils.isNotEmpty(classification.get(TRADE_NAME))){
            extended.put(TRADE_NAME,classification.get(TRADE_NAME).asText());
            finalTransformedObj.put(OUTLET_ATTR1, classification.get(TRADE_NAME).asText());
            finalTransformedObj.put("marketName", classification.get(TRADE_NAME).asText());
        }
        finalTransformedObj.put("marketId",isNewsPage);
        finalTransformedObj.put("subChannel", classification.get("subtrade_channel").asText());
        finalTransformedObj.put("channel", classification.get("sales_group").asText());
        finalTransformedObj.put("outletDivision", classification.get("division").asText());
        finalTransformedObj.put("outletCode", stringObjectMap.get("outletcode").toString());
        finalTransformedObj.put("outletName", stringObjectMap.get("outletname").toString());

        if(NullUtils.isNull(stringObjectMap.get("phonenumber")) || ObjectUtils.isEmpty(stringObjectMap.get("phonenumber")))
            finalTransformedObj.put("contactno", "0000000000");
        else
            finalTransformedObj.put("contactno", validateMobile(stringObjectMap));
        finalTransformedObj.put("source", stringObjectMap.get("Plant").toString());

        ObjectNode locationNode = getLocationInfo(primaryAddress);
        if (locationNode.get("region") != null) {
            List<GenericEntity> subTerritoryList = genericEntityService.readModelsByNameAndKey1("RegionStateMap", String.valueOf(locationNode.get("region").asText()));
            if (!subTerritoryList.isEmpty()) {
                finalTransformedObj.put("subTerritory", subTerritoryList.get(0).getKey2());
            } else {
                finalTransformedObj.put("subTerritory", "all");
            }
        } else {
            finalTransformedObj.put("subTerritory", "all");
        }
        finalTransformedObj.put("location", locationNode);
        Object salesRoute = stringObjectMap.get("sales_route");
        ArrayNode salesrepid = JSONUtils.getObjectMapper().convertValue(salesRoute, ArrayNode.class);
        extended.put("sales_route",salesrepid.get(0).asText());
        String distributorcode = stringObjectMap.get("distributorcode").toString().substring(2, 4);
        extended.put("distributorCode", distributorcode);
        final String IMM_PARENT = "immediateParent";
        Map<Object, Object> immediateParent1 = Map.of(IMM_PARENT, distributorcode);
        Map<Object, Object> immediateParent2 = Map.of(IMM_PARENT, salesrepid.get(0));
        List<Map<Object, Object>> parentList = new ArrayList<>();
        parentList.add(immediateParent1);
        parentList.add(immediateParent2);
        finalTransformedObj.put(IMM_PARENT, parentList);
        JsonNode extendedAttributes = JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class);
        finalTransformedObj.put("extendedAttributes", extendedAttributes);
        return finalTransformedObj;
    }

    private String validateMobile(Map<String, Object> input) {
        final String contact = "phonenumber";
        if (input.get(contact).toString().startsWith("+60")) {
            return input.get(contact).toString().substring(3);
        }else if (input.get(contact).toString().startsWith("60")) {
            return input.get(contact).toString().substring(2);
        }else if (input.get(contact).toString().startsWith("0")) {
            return input.get(contact).toString().substring(1);
        }
        return input.get(contact).toString();
    }

    private String setActiveStatus(Map<String, Object> stringObjectMap) {
        final String supReason = "suppression_reason";
        final String ACTIVE = "active";
        final String INACTIVE = "inactive";
        if(ObjectUtils.isNotEmpty(stringObjectMap.get(ACTIVE_STATUS)) && ObjectUtils.isNotEmpty(stringObjectMap.get(supReason)) && stringObjectMap.get(ACTIVE_STATUS).toString().equals("false") && stringObjectMap.get(supReason).toString().equals("S"))
            return INACTIVE;
        if (!stringObjectMap.containsKey(supReason)) return ACTIVE;
        if (stringObjectMap.containsKey(supReason) && stringObjectMap.get(supReason) != null) {
            String[] canBe = "I,K,T,N".split(",");
            for (String val : canBe) if (stringObjectMap.get(supReason).equals(val)) return ACTIVE;
        }
        return INACTIVE;
    }

    private ObjectNode getLocationInfo(JsonNode primaryAddress) {
        final String COUNTRY = "country";
        final String CITY = "city";
        final String REGION = "region";
        final String ZIP = "zip";
        final String DISTRICT = "sales_district";
        ObjectNode locationNode = mapper.createObjectNode();
        String city = ObjectUtils.isNotEmpty(primaryAddress.get(CITY)) ? primaryAddress.get(CITY).asText() : null;
        String region = ObjectUtils.isNotEmpty(primaryAddress.get(REGION)) ? primaryAddress.get(REGION).asText() : null;
        String country = ObjectUtils.isNotEmpty(primaryAddress.get(COUNTRY)) ? primaryAddress.get(COUNTRY).asText() : null;
        String zip = ObjectUtils.isNotEmpty(primaryAddress.get(ZIP)) ? primaryAddress.get(ZIP).asText() : null;
        String district = ObjectUtils.isNotEmpty(primaryAddress.get(DISTRICT)) ? primaryAddress.get(DISTRICT).asText() : null;
        if (ObjectUtils.isNotEmpty(city))
            locationNode.put("city", city);
        if (ObjectUtils.isNotEmpty(region)) {
            locationNode.put("region", region);
        }
        if (ObjectUtils.isNotEmpty(country))
            locationNode.put(COUNTRY, country);
        if (ObjectUtils.isNotEmpty(zip))
            locationNode.put("pincode", zip);
        if (ObjectUtils.isNotEmpty(district))
            locationNode.put("district", district);
        if (ObjectUtils.isEmpty(locationNode))
            locationNode.put(COUNTRY, "India");
        return locationNode;
    }
}