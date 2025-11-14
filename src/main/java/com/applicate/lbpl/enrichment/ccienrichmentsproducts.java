package com.applicate.lbpl.enrichment;

import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import javax.validation.ValidationException;
import java.util.*;
import java.util.regex.Pattern;

public class ccienrichmentsproducts extends AbstractEnrichment<ProductDetails> {

    GenericEntityService genericEntityService;

    @Override
    public EnrichmentResult apply(ProductDetails cdm) {

        genericEntityService = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);

        String authToken = SecurityContextUtils.getPrincipal();
        if (!"integration_user".equals(authToken)) {
            return new OperationResult.StepResult(OperationResult.Status.OK, "Product Details Enriched");
        }
        JsonNode extendedAttributes = cdm.getExtendedAttributes();
//
        //cc1
        String itemType = cdm.getItemType();
        String display = cdm.getDisplay();
        //cc2
        String brandCode = cdm.getBrandCode();
        //cc3
        String category = cdm.getCategory();
        //cc4 field not present pack type id
        String cat4 = cdm.getSubCategory();
        //cc5
        String piecesizedesc = cdm.getPieceSizeDesc();
        //cc6
        String brand = cdm.getBrand();
        //cc7 field not present
        String cat7 = extendedAttributes.get("categorycode7").asText();
        //cc8
        String piecesize = cdm.getPieceSize();


        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        Map<String, Integer> unitConversionMap = new HashMap<>();
        unitConversionMap.put("ML", 1);
        unitConversionMap.put("ml", 1);
        unitConversionMap.put("l", 1000);
        unitConversionMap.put("L", 1000);
        unitConversionMap.put("liter", 1000);
        unitConversionMap.put("LTR",1000);


        Set<String> itemTypeSet = Set.of("1","9","4","99");
        if(!itemTypeSet.contains(itemType)){
            throw new ValidationException("Invalid Item type :" +itemType + ". Only 1,9, 4 and 99 are valid values");
        }
        if(itemType.equalsIgnoreCase("1") || itemType.equalsIgnoreCase("4") ||  itemType.equalsIgnoreCase("99")  ||  itemType.equalsIgnoreCase("9")){
            display = "false" ;
            cdm.setDisplay(display);
        }


        if(!ObjectUtils.isEmpty(itemType)) {
            List<GenericEntity> itemtypeMapping = genericEntityService.findByNameAndKey1AndKey2("productMapping", "1", itemType);
            if(!itemtypeMapping.isEmpty()) {
                String cc1 = itemtypeMapping.get(0).getKey3();
                cdm.setItemType(cc1);
            }
        }

        if(!ObjectUtils.isEmpty(brandCode)){
            List<GenericEntity> brancodemapping = genericEntityService.findByNameAndKey1AndKey2("productMapping","6", brandCode);
            if(!brancodemapping.isEmpty()) {
                String cc6 = brancodemapping.get(0).getKey3();
                cdm.setBrandCode(cc6);
            }
        }
        if(!ObjectUtils.isEmpty(brand)){
            List<GenericEntity> brandmapping = genericEntityService.findByNameAndKey1AndKey2("productMapping","2", brand);
            if(!brandmapping.isEmpty()){
                String cc2 =  brandmapping.get(0).getKey3();
                cdm.setBrand(cc2);
            }
        }

        if(!ObjectUtils.isEmpty(category)) {
            List<GenericEntity> categorymapping = genericEntityService.findByNameAndKey1AndKey2("productMapping", "3", category);
            if(!categorymapping.isEmpty()) {
                String cc3 = categorymapping.get(0).getKey3();
                cdm.setCategory(cc3);
            }
        }

        if(!ObjectUtils.isEmpty(piecesizedesc)) {
            List<GenericEntity> piecesizedescmap = genericEntityService.findByNameAndKey1AndKey2("productMapping", "5", piecesizedesc);
            if(!piecesizedescmap.isEmpty()) {
                String cc5 = piecesizedescmap.get(0).getKey3();
                cdm.setPieceSizeDesc(cc5);
            }
        }




        if(!ObjectUtils.isEmpty(piecesize)) {
            List<GenericEntity> piecesizemap = genericEntityService.findByNameAndKey1AndKey2("productMapping", "8", piecesize);
            if(!piecesizemap.isEmpty()){
                String cc8 = piecesizemap.get(0).getKey3();
                String[] parts = cc8.split(" ");
                if(parts.length ==2 && pattern.matcher(parts[0]).matches()) {
                    Integer numericValue = Integer.parseInt(parts[0]);
                    String unit = parts[1];
                    cdm.setPieceSize(String.valueOf(numericValue * unitConversionMap.get(unit)));
                }else{
                    cdm.setPieceSize("");
                }

            }


        }else{
            cdm.setPieceSize("NA");
        }

        if(!ObjectUtils.isEmpty(cat4)) {
            List<GenericEntity> cc4map = genericEntityService.findByNameAndKey1AndKey2("productMapping", "4", cat4);
            if(!cc4map.isEmpty()){
                String cc4 = cc4map.get(0).getKey3();
                cdm.setSubCategory(cc4);
            }
//            ((ObjectNode) extendedAttributes).put("categorycode4", cc4);
        }

        if(!ObjectUtils.isEmpty(cat7)) {
            List<GenericEntity> cc7map = genericEntityService.findByNameAndKey1AndKey2("productMapping", "7", cat7);
            if(!cc7map.isEmpty()) {
                String cc7 = cc7map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put("categorycode7", cc7);
            }
        }


        cdm.setPriority(0);
        // to set values for items in extended
// this comment is to check if channelkart bundle is working or not .
        return new OperationResult.StepResult(OperationResult.Status.OK, "Product details enriched.");
    }
}
