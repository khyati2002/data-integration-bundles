//package com.applicate.simamy.enrichment;
//
//import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
//import com.applicate.services.channelkart.enrichments.EnrichmentResult;
//import com.applicate.services.channelkart.enrichments.Status;
//import com.applicate.services.channelkart.models.GenericEntity;
//import com.applicate.services.channelkart.models.Location;
//import com.applicate.services.channelkart.models.OutletDetails;
//import com.applicate.services.channelkart.models.enums.ActiveStatus;
//import com.applicate.services.channelkart.repository.GenericEntityRepository;
//import com.applicate.services.channelkart.services.SpringContext;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.apache.commons.lang3.ObjectUtils;
//
//import java.util.List;
//
//public class SimaOutletCategoryEnrichment extends AbstractEnrichment<OutletDetails>
//{
//
//    GenericEntityRepository repository = SpringContext.getBean(GenericEntityRepository.class);
//
//    @Override
//    public EnrichmentResult apply(OutletDetails cdm)
//    {
//
//        JsonNode extendedAttributes = cdm.getExtendedAttributes();
//
//
//        //----------//
//
//        //cc1 sales org
////        String outletType = cdm.getOutletType();
////        if (!ObjectUtils.isEmpty(outletType)) {
////            List<GenericEntity> outletTypeMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "1", outletType);
////            if (!outletTypeMapping.isEmpty()) {
////                String cc1 = outletTypeMapping.get(0).getKey3();
////                cdm.setOutletType(cc1);
////            }
////        }
////        else
////        {
////            cdm.setOutletType("NA");
////        }
//        if (extendedAttributes.has("SalesOrganisationId")) {
//            String cc1 = extendedAttributes.get("SalesOrganisationId").asText();
//            List<GenericEntity> cc1map = repository.findByNameAndKey1AndKey2("Outletcategory", "1", cc1);
//            if(!cc1map.isEmpty()) {
//                String code1 = cc1map.get(0).getKey3();
//                ((ObjectNode) extendedAttributes).put("SalesOrganisationId", code1);
//            }
//        }
//
//
//        //cc2 Region
//
//        Location location = cdm.getLocationHierarchy();
//        String cc2 = location.getRegion();
//        if (!ObjectUtils.isEmpty(cc2)) {
//            List<GenericEntity> cc2map = repository.findByNameAndKey1AndKey2("Outletcategory", "2", cc2);
//            if (!cc2map.isEmpty()) {
//                String code2 = cc2map.get(0).getKey3();
//                location.setRegion(code2);
//                cdm.setLocationHierarchy(location);
//            }
//        }
//
//        //cc3 tradename
//
//        if (extendedAttributes.has("tradename")) {
//            String cc3 = extendedAttributes.get("tradename").asText();
//            List<GenericEntity> cc3map = repository.findByNameAndKey1AndKey2("Outletcategory", "3", cc3);
//            if(!cc3map.isEmpty()) {
//                String code3 = cc3map.get(0).getKey3();
//                ((ObjectNode) extendedAttributes).put("tradename", code3);
//            }
//        }
//
//
//        //cc4 suppression_reason
//
//        if (extendedAttributes.has("suppression_reason")) {
//            String cc4 = extendedAttributes.get("suppression_reason").asText();
//            List<GenericEntity> cc4map = repository.findByNameAndKey1AndKey2("Outletcategory", "4", cc4);
//            if(!cc4map.isEmpty()) {
//                String code4 = cc4map.get(0).getKey3();
//                ((ObjectNode) extendedAttributes).put("suppression_reason", code4);
//            }
//
//        }
//
//        //cc5 sales_office
//
//        if (extendedAttributes.has("sales_office")) {
//            String cc5 = extendedAttributes.get("sales_office").asText();
//            List<GenericEntity> cc5map = repository.findByNameAndKey1AndKey2("Outletcategory", "5", cc5);
//            if(!cc5map.isEmpty()) {
//                String code5 = cc5map.get(0).getKey3();
//                ((ObjectNode) extendedAttributes).put("sales_office", code5);
//            }
//        }
//
//        //cc6 sales_group (storing in our db in channel column)
//
//       /*   String channel = cdm.getChannel();
//        if (!ObjectUtils.isEmpty(channel)) {
//            List<GenericEntity> channelMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "6", channel);
//            if (!channelMapping.isEmpty()) {
//                String cc10channel = channelMapping.get(0).getKey3();
//                cdm.setChannel(cc10channel);
//            }
//        }
//        else
//        {
//            cdm.setChannel("NA");
//        }*/
//
//
//
//        //cc7 PMNTTRMS
//
//        if (extendedAttributes.has("PMNTTRMS")) {
//            String cc7 = extendedAttributes.get("PMNTTRMS").asText();
//            List<GenericEntity> cc7map = repository.findByNameAndKey1AndKey2("Outletcategory", "6", cc7);
//            if(!cc7map.isEmpty()) {
//                String code7 = cc7map.get(0).getKey3();
//                ((ObjectNode) extendedAttributes).put("PMNTTRMS", code7);
//            }
//        }
//
//        //cc8 district
//
//        String cc8 = location.getDistrict();
//        if (!ObjectUtils.isEmpty(cc8)) {
//            List<GenericEntity> cc8map = repository.findByNameAndKey1AndKey2("Outletcategory", "8", cc8);
//            if (!cc8map.isEmpty()) {
//                String code8 = cc8map.get(0).getKey3();
//                location.setDistrict(code8);
//                cdm.setLocationHierarchy(location);
//            }
//        }
//
//        //cc9 Price List
//
//        String priceid = cdm.getPriceListId();
//        if (!ObjectUtils.isEmpty(priceid)) {
//            List<GenericEntity> priceidMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "9", priceid);
//            if (!priceidMapping.isEmpty()) {
//                String cc9priceid = priceidMapping.get(0).getKey3();
//                cdm.setPriceListId(cc9priceid);
//            }
//        }
//        else
//        {
//            cdm.setPriceListId("NA");
//        }
//
//        //cc10 trade_channel (in extended)
//
//        if (extendedAttributes.has("trade_channel")) {
//            String cc10 = extendedAttributes.get("trade_channel").asText();
//            List<GenericEntity> cc10map = repository.findByNameAndKey1AndKey2("Outletcategory", "10", cc10);
//            if(!cc10map.isEmpty()) {
//                String code10 = cc10map.get(0).getKey3();
//                ((ObjectNode) extendedAttributes).put("trade_channel", code10);
//            }
//        }
//
//        //code11 class
//
//        String class1 = cdm.getOutletClass();
//        if (!ObjectUtils.isEmpty(class1)) {
//            List<GenericEntity> classMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "11", class1);
//            if (!classMapping.isEmpty()) {
//                String cc11class = classMapping.get(0).getKey3();
//                cdm.setOutletClass(cc11class);
//            }
//        }
//        else
//        {
//            cdm.setOutletClass("NA");
//        }
//
//        //code 12 plant
//
//        String source = cdm.getSource();
//        if (!ObjectUtils.isEmpty(class1)) {
//            List<GenericEntity> sourceMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "12", source);
//            if (!sourceMapping.isEmpty()) {
//                String cc12class = sourceMapping.get(0).getKey3();
//                cdm.setSource(cc12class);
//            }
//        }
//        else
//        {
//            cdm.setSource("NA");
//        }
//
//        //code 14 sub channel
//        String subchannel = cdm.getSubChannel();
//        if (!ObjectUtils.isEmpty(subchannel)) {
//            List<GenericEntity> subchannelMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "14", subchannel);
//            if (!subchannelMapping.isEmpty()) {
//                String cc14class = subchannelMapping.get(0).getKey3();
//                cdm.setSubChannel(cc14class);
//            }
//        }
//        else
//        {
//            cdm.setSubChannel("NA");
//        }
//
//        // code 15 distri channel
//
//        String districhannel = cdm.getDistributionChannel();
//        if (!ObjectUtils.isEmpty(districhannel)) {
//            List<GenericEntity> districhannelMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "15", districhannel);
//            if (!districhannelMapping.isEmpty()) {
//                String cc15districhannelMapping = districhannelMapping.get(0).getKey3();
//                cdm.setDistributionChannel(cc15districhannelMapping);
//            }
//        }
//        else
//        {
//            cdm.setDistributionChannel("NA");
//        }
//
//        // code 16 outlet type
//
//        String outlettype = cdm.getOutletType();
//        if (!ObjectUtils.isEmpty(outlettype)) {
//            List<GenericEntity> outlettypeMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "16", outlettype);
//            if (!outlettypeMapping.isEmpty()) {
//                String cc16outlettype = outlettypeMapping.get(0).getKey3();
//                cdm.setOutletType(cc16outlettype);
//            }
//        }
//        else
//        {
//            cdm.setOutletType("NA");
//        }
//
//        // code 17
//
//        if (extendedAttributes.has("ship_condition")) {
//            String cc17 = extendedAttributes.get("ship_condition").asText();
//            List<GenericEntity> cc17map = repository.findByNameAndKey1AndKey2("Outletcategory", "17", cc17);
//            if(!cc17map.isEmpty()) {
//                String code17 = cc17map.get(0).getKey3();
//                ((ObjectNode) extendedAttributes).put("ship_condition", code17);
//            }
//        }
//
//        if(cdm.getOldModel() != null && "active".equalsIgnoreCase(cdm.getOldModel().getActiveStatus().getStatus()) && "active".equalsIgnoreCase(extendedAttributes.get("activeStatus").asText())){
//            cdm.setActiveStatus(ActiveStatus.ACTIVE);
//        }
//
//
////        //cc3
////        String marketName = cdm.getMarketName();
////        if (!ObjectUtils.isEmpty(marketName)) {
////            List<GenericEntity> marketNameMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "3", marketName);
////            if (!marketNameMapping.isEmpty()) {
////                String cc3 = marketNameMapping.get(0).getKey3();
////                cdm.setMarketName(cc3);
////            }
////        }
////        else
////        {
////            cdm.setMarketName("NA");
////        }
//////        enrichExtendedAttributes(extendedAttributes, "CategoryCode3", "3");--
////
//////        if(containsValue(extendedAttributes, "CategoryCode3"))
//////        {
//////            String data = getDataForKey(extendedAttributes.get("CategoryCode3").asText(), "3");
//////            ((ObjectNode) extendedAttributes).put("categoryCode3", data);
//////        }
////
//////        enrichExtendedAttributes(extendedAttributes, "CategoryCode3", "3");--
////
//////        public String extraCode(List<GenericEntity> cc3map_1, {
//////
//////            if(ObjectUtils.isNotEmpty(cc3map_1)){
//////                return cc3map_1.get(0).getKey3();
//////            }
//////            else{
//////                return extendedAttributes.get("").asText();
//////            }
//////    }
////
////
////        //cc4
////        String channel = cdm.getChannel();
////        if (!ObjectUtils.isEmpty(channel)) {
////            List<GenericEntity> channelMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "4", channel);
////            if (!channelMapping.isEmpty()) {
////                String cc4 = channelMapping.get(0).getKey3();
////                cdm.setChannel(cc4);
////            }
////        }
////        else
////        {
////            cdm.setChannel("NA");
////        }
////
//////        String data = getDataForKey(channel4,"4");
//////        List<GenericEntity> cc4map = repository.findByNameAndKey1AndKey2("Outletcategory", "4", channel4);
//////        String code4 = cc4map.get(0).getKey3();
////
//////        cdm.setChannel( getDataForKey(cdm.getChannel(),"4") );--
////
////        //cc5
////        String subchannel = cdm.getSubChannel();
////        if (!ObjectUtils.isEmpty(subchannel)) {
////            List<GenericEntity> subchannelMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "5", subchannel);
////            if (!subchannelMapping.isEmpty()) {
////                String cc5 = subchannelMapping.get(0).getKey3();
////                cdm.setSubChannel(cc5);
////            }
////        }
////        else
////        {
////            cdm.setSubChannel("NA");
////        }
//////        List<GenericEntity> cc5map = repository.findByNameAndKey1AndKey2("Outletcategory", "5", subchannel5);
//////        String code5 = cc5map.get(0).getKey3();
//////        cdm.setSubChannel(code5);
////
////
////        //cc6
////        String outletcategory = cdm.getOutletCategory();
////        if (!ObjectUtils.isEmpty(outletcategory)) {
////            List<GenericEntity> outletcategoryMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "6", outletcategory);
////            if (!outletcategoryMapping.isEmpty()) {
////                String cc6 = outletcategoryMapping.get(0).getKey3();
////                cdm.setOutletCategory(cc6);
////            }
////
////        }
////        else
////        {
////            cdm.setOutletCategory("NA");
////        }
//////        List<GenericEntity> cc6map = repository.findByNameAndKey1AndKey2("Outletcategory", "6", category6);
//////        String code6 = cc6map.get(0).getKey3();
//////        cdm.setOutletCategory(code6);
////
////
////        //cc7
////        String outletclass = cdm.getOutletClass();
////        if (!ObjectUtils.isEmpty(outletclass)) {
////            List<GenericEntity> outletclassMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "7", outletclass);
////            if (!outletclassMapping.isEmpty()) {
////                String cc7 = outletclassMapping.get(0).getKey3();
////                cdm.setOutletClass(cc7);
////            }
////        }
////        else
////        {
////            cdm.setOutletClass("NA");
////        }
////
//////        if (extendedAttributes.has("CategoryCode7")) {
//////            String cc7 = extendedAttributes.get("CategoryCode7").asText();
//////            List<GenericEntity> cc7map = repository.findByNameAndKey1AndKey2("Outletcategory", "7", cc7);
//////            String code7 = cc7map.get(0).getKey3();
//////            ((ObjectNode) extendedAttributes).put("categoryCode7", code7);
//////        }
////
////        //cc8
////        String distributionChannel = cdm.getDistributionChannel();
////        if (!ObjectUtils.isEmpty(distributionChannel)) {
////            List<GenericEntity> distributionChannelMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "8", distributionChannel);
////            if (!distributionChannelMapping.isEmpty()) {
////                String cc8 = distributionChannelMapping.get(0).getKey3();
////                cdm.setDistributionChannel(cc8);
////            }
////        }
////        else
////        {
////            cdm.setDistributionChannel("NA");
////        }
//////        if (extendedAttributes.has("CategoryCode8")) {
//////            String cc8 = extendedAttributes.get("CategoryCode8").asText();
//////            List<GenericEntity> cc8map = repository.findByNameAndKey1AndKey2("Outletcategory", "8", cc8);
//////            String code8 = cc8map.get(0).getKey3();
//////            ((ObjectNode) extendedAttributes).put("categoryCode8", code8);
//////        }
////
////        //cc9
////        String outletDivision = cdm.getOutletDivision();
////        if (!ObjectUtils.isEmpty(outletDivision)) {
////            List<GenericEntity> outletDivisionMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "9", outletDivision);
////            if (!outletDivisionMapping.isEmpty()) {
////                String cc9 = outletDivisionMapping.get(0).getKey3();
////                cdm.setOutletDivision(cc9);
////            }
////        }
////        else
////        {
////            cdm.setOutletDivision("NA");
////        }
//////        if (extendedAttributes.has("CategoryCode9")) {
//////            String cc9 = extendedAttributes.get("CategoryCode9").asText();
//////            List<GenericEntity> cc9map = repository.findByNameAndKey1AndKey2("Outletcategory", "9", cc9);
//////            String code9 = cc9map.get(0).getKey3();
//////            ((ObjectNode) extendedAttributes).put("categoryCode9", code9);
//////        }
////
////        //cc10
////        String account = cdm.getAccount();
////        if (!ObjectUtils.isEmpty(account)) {
////            List<GenericEntity> accountMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "10", account);
////            if (!accountMapping.isEmpty()) {
////                String cc10 = accountMapping.get(0).getKey3();
////                cdm.setAccount(cc10);
////            }
////        }
////        else
////        {
////            cdm.setAccount("NA");
////        }
//
////        if (extendedAttributes.has("CategoryCode10")) {
////            String cc10 = extendedAttributes.get("CategoryCode10").asText();
////            List<GenericEntity> cc10map = repository.findByNameAndKey1AndKey2("Outletcategory", "10", cc10);
////            String code10 = cc10map.get(0).getKey3();
////            ((ObjectNode) extendedAttributes).put("categoryCode10", code10);
////
////            //---------//
////        }
//        return new EnrichmentResult(Status.OK, "Data Enriched");
//
//    }
//}
//
//
////    private String getDataForKey(String key, String outletType){
////        if(StringUtils.isEmpty(key)){
////            return key;
////        }
//////        String cc3 = extendedAttributes.get("CategoryCode3").asText();
////        List<GenericEntity> dataMap = repository.findByNameAndKey1AndKey2("Outletcategory", outletType, key);
////        return ObjectUtils.isNotEmpty(dataMap) ?  dataMap.get(0).getKey3() : key;
//////        ((ObjectNode) extendedAttributes).put("categoryCode3", code3);
////    }
////
////        private boolean containsValue(JsonNode extendedAttributes, String key){
////            return extendedAttributes.has(key) && extendedAttributes.get(key)!=null && !StringUtils.isEmpty(extendedAttributes.get(key).asText());
////        }
////
////        private void enrichExtendedAttributes(JsonNode extendedAttributes, String key, String outletType){
////            if(containsValue(extendedAttributes, key))
////            {
////                String data = getDataForKey(extendedAttributes.get(key).asText(), outletType);
////                ((ObjectNode) extendedAttributes).put(key, data);
////            }
////        }
//
