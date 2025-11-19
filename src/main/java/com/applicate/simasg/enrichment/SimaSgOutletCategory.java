package com.applicate.simasg.enrichment;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class SimaSgOutletCategory extends AbstractEnrichment<OutletDetails>
{
        GenericEntityRepository repository =
            (GenericEntityRepository) ServiceLocator.lookup(GenericEntity.class);

    @Override
    public EnrichmentResult apply(OutletDetails cdm)
    {

        JsonNode extendedAttributes = cdm.getExtendedAttributes();


        //----------//

        //cc1 sales org is stored in account

        String cc1 = cdm.getAccount();
        if (!ObjectUtils.isEmpty(cc1)) {
            List<GenericEntity> cc1map = repository.findByNameAndKey1AndKey2("Outletcategory", "1",cc1 );
            if (!cc1map.isEmpty()) {
                String cc1account = cc1map.get(0).getKey3();
                cdm.setAccount(cc1account);
            }
        }
        else
        {
            cdm.setAccount("NA");
        }


        //cc2 Region

        Location location = cdm.getLocationHierarchyAsLocation();
        String cc2 = location.getRegion();
        if (!ObjectUtils.isEmpty(cc2)) {
            List<GenericEntity> cc2map = repository.findByNameAndKey1AndKey2("Outletcategory", "2", cc2);
            if (!cc2map.isEmpty()) {
                String code2 = cc2map.get(0).getKey3();
                location.setRegion(code2);
                cdm.setLocationHierarchy(location);
            }
        }

        //cc3 tradename

        if (extendedAttributes.has("tradename")) {
            String cc3 = extendedAttributes.get("tradename").asText();
            List<GenericEntity> cc3map = repository.findByNameAndKey1AndKey2("Outletcategory", "3", cc3);
            if(!cc3map.isEmpty()) {
                String code3 = cc3map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put("tradename", code3);
            }
        }


        //cc4 suppression_reason

        if (extendedAttributes.has("suppression_reason")) {
            String cc4 = extendedAttributes.get("suppression_reason").asText();
            List<GenericEntity> cc4map = repository.findByNameAndKey1AndKey2("Outletcategory", "4", cc4);
            if(!cc4map.isEmpty()) {
                String code4 = cc4map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put("suppression_reason", code4);
            }

        }

        //cc5 sales_office to market_name

        String salesoffice = cdm.getMarketName();
        if (!ObjectUtils.isEmpty(salesoffice)) {
            List<GenericEntity> marketnameMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "5", salesoffice);
            if (!marketnameMapping.isEmpty()) {
                String salesofficeid = marketnameMapping.get(0).getKey3();
                cdm.setMarketName(salesofficeid);
            }
        }
        else
        {
            cdm.setMarketName("NA");
        }





        //cc7 PMNTTRMS

        if (extendedAttributes.has("PMNTTRMS")) {
            String cc7 = extendedAttributes.get("PMNTTRMS").asText();
            List<GenericEntity> cc7map = repository.findByNameAndKey1AndKey2("Outletcategory", "6", cc7);
            if(!cc7map.isEmpty()) {
                String code7 = cc7map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put("PMNTTRMS", code7);
            }
        }

        //cc8 district

        String cc8 = location.getDistrict();
        if (!ObjectUtils.isEmpty(cc8)) {
            List<GenericEntity> cc8map = repository.findByNameAndKey1AndKey2("Outletcategory", "8", cc8);
            if (!cc8map.isEmpty()) {
                String code8 = cc8map.get(0).getKey3();
                location.setDistrict(code8);
                cdm.setLocationHierarchy(location);
            }
        }

        //cc9 Price List

        String priceid = cdm.getPriceListId();
        if (!ObjectUtils.isEmpty(priceid)) {
            List<GenericEntity> priceidMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "9", priceid);
            if (!priceidMapping.isEmpty()) {
                String cc9priceid = priceidMapping.get(0).getKey3();
                cdm.setPriceListId(cc9priceid);
            }
        }
        else
        {
            cdm.setPriceListId("NA");
        }

        //cc10 trade_channel (in extended)

        if (extendedAttributes.has("trade_channel")) {
            String cc10 = extendedAttributes.get("trade_channel").asText();
            List<GenericEntity> cc10map = repository.findByNameAndKey1AndKey2("Outletcategory", "10", cc10);
            if(!cc10map.isEmpty()) {
                String code10 = cc10map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put("trade_channel", code10);
            }
        }

        //code11 class

        String class1 = cdm.getOutletClass();
        if (!ObjectUtils.isEmpty(class1)) {
            List<GenericEntity> classMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "11", class1);
            if (!classMapping.isEmpty()) {
                String cc11class = classMapping.get(0).getKey3();
                cdm.setOutletClass(cc11class);
            }
        }
        else
        {
            cdm.setOutletClass("NA");
        }

        //code 12 plant

        String source = cdm.getSource();
        if (!ObjectUtils.isEmpty(class1)) {
            List<GenericEntity> sourceMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "12", source);
            if (!sourceMapping.isEmpty()) {
                String cc12class = sourceMapping.get(0).getKey3();
                cdm.setSource(cc12class);
            }
        }
        else
        {
            cdm.setSource("NA");
        }

        //code 14 sub channel
        String subchannel = cdm.getSubChannel();
        if (!ObjectUtils.isEmpty(subchannel)) {
            List<GenericEntity> subchannelMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "14", subchannel);
            if (!subchannelMapping.isEmpty()) {
                String cc14class = subchannelMapping.get(0).getKey3();
                cdm.setSubChannel(cc14class);
            }
        }
        else
        {
            cdm.setSubChannel("NA");
        }

        // code 15 distri channel

        String districhannel = cdm.getDistributionChannel();
        if (!ObjectUtils.isEmpty(districhannel)) {
            List<GenericEntity> districhannelMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "15", districhannel);
            if (!districhannelMapping.isEmpty()) {
                String cc15districhannelMapping = districhannelMapping.get(0).getKey3();
                cdm.setDistributionChannel(cc15districhannelMapping);
            }
        }
        else
        {
            cdm.setDistributionChannel("NA");
        }

        // code 16 outlet type

        String outlettype = cdm.getOutletType();
        if (!ObjectUtils.isEmpty(outlettype)) {
            List<GenericEntity> outlettypeMapping = repository.findByNameAndKey1AndKey2("Outletcategory", "16", outlettype);
            if (!outlettypeMapping.isEmpty()) {
                String cc16outlettype = outlettypeMapping.get(0).getKey3();
                cdm.setOutletType(cc16outlettype);
            }
        }
        else
        {
            cdm.setOutletType("NA");
        }

        // code 17

        if (extendedAttributes.has("ship_condition")) {
            String cc17 = extendedAttributes.get("ship_condition").asText();
            List<GenericEntity> cc17map = repository.findByNameAndKey1AndKey2("Outletcategory", "17", cc17);
            if(!cc17map.isEmpty()) {
                String code17 = cc17map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put("ship_condition", code17);
            }
        }

        if(cdm.getOldModel() != null && "active".equalsIgnoreCase(cdm.getOldModel().getActiveStatus().getStatus()) && "active".equalsIgnoreCase(extendedAttributes.get("activeStatus").asText())){
            cdm.setActiveStatus(ActiveStatus.ACTIVE);
        }

        return new OperationResult.StepResult(OperationResult.Status.OK, "Data Enriched");

    }
}


