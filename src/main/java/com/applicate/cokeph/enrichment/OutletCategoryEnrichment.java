package com.applicate.cokeph.enrichment;

import com.applicate.services.channelkart.services.CategoryInfoService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.jooq.impl.CategoryInfo;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.ProductDetails;
//import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;



import java.util.List;

public class OutletCategoryEnrichment {
    public EnrichmentResult apply(OutletDetails cdm) throws JsonProcessingException {

        CategoryInfoService repository = (CategoryInfoService) ServiceLocator.lookup(CategoryInfo.class);

        org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode shadedNode = cdm.getExtendedAttributes();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode extendedAttributes = (ObjectNode) mapper.readTree(shadedNode.toString());

        String channel = cdm.getChannel();
        String outletCategory = cdm.getOutletCategory();
        String outletType = cdm.getOutletType();
        String outletClass = cdm.getOutletClass();
        String subChannel = cdm.getSubChannel();
        String marketId = cdm.getMarketId();



        if (!StringUtils.isEmpty(channel)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> channelMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("8",channel, "outletMaster");
            if (!channelMapping.isEmpty()) {
                String cc = channelMapping.get(0).getNewDescription();
                cdm.setChannel(cc);
            }
        }

        if (!StringUtils.isEmpty(outletCategory)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> outletCategoryMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("2",outletCategory, "outletMaster");
            if (!outletCategoryMapping.isEmpty()) {
                String cc = outletCategoryMapping.get(0).getNewDescription();
                cdm.setOutletCategory(cc);
            }
        }

        if (!StringUtils.isEmpty(outletType)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> outletTypeMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("4",outletType, "outletMaster");
            if (!outletTypeMapping.isEmpty()) {
                String cc = outletTypeMapping.get(0).getNewDescription();
                cdm.setOutletType(cc);
            }
        }

        if (!StringUtils.isEmpty(outletClass)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> outletClassMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("9",outletClass, "outletMaster");
            if (!outletClassMapping.isEmpty()) {
                String cc = outletClassMapping.get(0).getNewDescription();
                cdm.setOutletClass(cc);
            }
        }

        if (!StringUtils.isEmpty(subChannel)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> subChannelMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("6",subChannel, "outletMaster");
            if (!subChannelMapping.isEmpty()) {
                String cc = subChannelMapping.get(0).getNewDescription();
                cdm.setSubChannel(cc);
            }
        }

        if (!StringUtils.isEmpty(marketId)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> marketIdMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("5",marketId, "outletMaster");
            if (!marketIdMapping.isEmpty()) {
                String cc = marketIdMapping.get(0).getNewDescription();
                cdm.setMarketId(cc);
            }
        }

        if (extendedAttributes.has("trade_group")) {
            String cc_extended = extendedAttributes.get("trade_group").asText();
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> cc_map = repository.findByCategoryCodeAndCategoryValueAndFeature("3",cc_extended, "outletMaster");
            if(!cc_map.isEmpty()){
                String code_3 = cc_map.get(0).getNewDescription();
                 extendedAttributes.put("trade_group", code_3);}

        }


        if (extendedAttributes.has("business_complex_type")) {
            String cc_extended = extendedAttributes.get("business_complex_type").asText();
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> cc_map = repository.findByCategoryCodeAndCategoryValueAndFeature("7",cc_extended, "outletMaster");
            if (!cc_map.isEmpty()){
                String code_3 = cc_map.get(0).getNewDescription();
                extendedAttributes.put("business_complex_type", code_3);}
        }

        if (extendedAttributes.has("categorycode10")) {
            String cc_extended = extendedAttributes.get("categorycode10").asText();
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> cc_map = repository.findByCategoryCodeAndCategoryValueAndFeature("10",cc_extended, "outletMaster");
            if (!cc_map.isEmpty()){
                String code_3 = cc_map.get(0).getNewDescription();
                 extendedAttributes.put("categorycode10", code_3);
            }
        }


        return new OperationResult.StepResult(OperationResult.Status.OK);


    }
}
