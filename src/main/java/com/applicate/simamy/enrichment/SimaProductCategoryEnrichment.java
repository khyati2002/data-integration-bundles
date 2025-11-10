package com.applicate.simamy.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.models.ProductDetails;
import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.services.SpringContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SimaProductCategoryEnrichment extends AbstractEnrichment<ProductDetails> {

    GenericEntityRepository repository = SpringContext.getBean(GenericEntityRepository.class);
    static final String PRODUCT_MAPPING = "productMapping";
    static final String SALES_UNIT = "SALES_UNIT";
    static final String BEVERAGE_PRODUCT = "BEVERAGE_PRODUCT";
    static final String PACKAGE = "PACKAGE";
    static final String TRADE_MARK = "TRADE_MARK";
    static final String PACK_TYPE = "packtype";

    static Map<String, Double> unitConversionMap = new HashMap<>();
    public SimaProductCategoryEnrichment() {
        unitConversionMap.put("ML", 1.0);
        unitConversionMap.put("ml", 1.0);
        unitConversionMap.put("l", 1000.0);
        unitConversionMap.put("L", 1000.0);
        unitConversionMap.put("LTR", 1000.0);
        unitConversionMap.put("KG", 1000.0);
        unitConversionMap.put("kg", 1000.0);
        unitConversionMap.put("GM", 1.0);
        unitConversionMap.put("gm", 1.0);
    }

    static final String Malt_grp_2 = "MATL_GRP_2";
    @Override
    public EnrichmentResult apply(ProductDetails cdm) {
        JsonNode extendedAttributes = cdm.getExtendedAttributes();

      //cc1 brand
        String brand = cdm.getBrand();
        if (!ObjectUtils.isEmpty(brand)) {
            List<GenericEntity> brandMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "1", brand);
            if (!brandMapping.isEmpty()) {
                String cc1brand = brandMapping.get(0).getKey3();
                cdm.setBrand(cc1brand);
            }
        }
        else
        {
            cdm.setBrand("NA");
        }
        // cc11 piece size desc
        String piecesizedesc = cdm.getPieceSizeDesc();
        if (!ObjectUtils.isEmpty(piecesizedesc)) {
            List<GenericEntity> piecesizedescMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "11", piecesizedesc);
            if (!piecesizedescMapping.isEmpty()) {
                String cc11piecesizedesc = piecesizedescMapping.get(0).getKey3();
                cdm.setPieceSizeDesc(cc11piecesizedesc);
            }
        }
        else
        {
            cdm.setPieceSizeDesc("NA");
        }


        //cc2 SALES_UNIT
        if (extendedAttributes.has(SALES_UNIT)) {
            String cc2 = extendedAttributes.get(SALES_UNIT).asText();
            List<GenericEntity> cc2map = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "2", cc2);
            if(!cc2map.isEmpty()) {
                String code2 = cc2map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put(SALES_UNIT, code2);
            }
        }
        String cc2 = extendedAttributes.get(SALES_UNIT).asText();

        String itmdesc = cdm.getItemDesc();
        String ePS = enrichPackSize(cdm);
        double ePSDouble = Double.parseDouble(ePS);
//        Check if L or ml is needed
        if (ePSDouble >= 1000) {
//            Check if decimal digits are needed or not
            if ((int)Math.floor(ePSDouble/1000)*1000 == Integer.parseInt(ePS)) {
                ePS = (int) Math.abs(ePSDouble)/1000 + "L";
            } else {
                ePS = ePSDouble / 1000 + "L";
            }
        } else {
            ePS = ePS+"ml";
        }
        String enhanceddesc = itmdesc+"("+getsalesunit(cc2)+"X"+ePS+")";
        cdm.setItemDesc(enhanceddesc);
        cdm.setSkuDescription(enhanceddesc);




        //cc4 item type

        String itemtype = cdm.getItemType();
        if (!ObjectUtils.isEmpty(itemtype)) {
            List<GenericEntity> itemtypeMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "4", itemtype);
            if (!itemtypeMapping.isEmpty()) {
                String cc4itemtype = itemtypeMapping.get(0).getKey3();
                cdm.setItemType(cc4itemtype);
            }
        }
        else
        {
            cdm.setItemType("NA");
        }
        enrichBevProduct(cdm, extendedAttributes);
        enrichData(cdm, extendedAttributes);
       // cc13 uom
        String uom = cdm.getUom();
        if (!ObjectUtils.isEmpty(uom)) {
            List<GenericEntity> uomMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "13", uom);
            if (!uomMapping.isEmpty()) {
                String cc13uom = uomMapping.get(0).getKey3();
                cdm.setUom(cc13uom);
            }
        }
        else
        {
            cdm.setUom("NA");
        }
        return EnrichmentResult.OK ;
    }
    private String getsalesunit(String qwe)
    {
        if(qwe.startsWith("1X"))
            return qwe.substring(2);
        else
            return qwe;

    }
    private void enrichBevProduct(ProductDetails cdm, JsonNode extendedAttributes) {
        //cc5 BEVERAGE_PRODUCT
        if (extendedAttributes.has(BEVERAGE_PRODUCT)) {
            String cc5 = extendedAttributes.get(BEVERAGE_PRODUCT).asText();
            List<GenericEntity> cc5map = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "5", cc5);
            if(!cc5map.isEmpty()) {
                String code5 = cc5map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put(BEVERAGE_PRODUCT, code5);
            }
        }
        //cc6 category
        String category = cdm.getCategory();
        if (!ObjectUtils.isEmpty(category)) {
            List<GenericEntity> categoryMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "6", category);
            if (!categoryMapping.isEmpty()) {
                String cc6category = categoryMapping.get(0).getKey3();
                cdm.setCategory(cc6category);
            }
        }
        else
        {
            cdm.setCategory("NA");
        }
    }

    private String enrichPackSize(ProductDetails cdm) {
        //cc3 pack size
        Pattern pattern = Pattern.compile("-?(\\d+(\\.\\d+))|(\\.\\d)|(\\d+)?");
        String piecesize = cdm.getPieceSize();
        if (!ObjectUtils.isEmpty(piecesize)) {
            List<GenericEntity> piecesizeMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "3", piecesize);
            if (!piecesizeMapping.isEmpty()) {
                String cc3piecesize = piecesizeMapping.get(0).getKey3();
                String[] parts = cc3piecesize.split(" ");
                if(parts.length ==2 && pattern.matcher(parts[0]).matches()) {
                    double numericValue = Double.parseDouble(parts[0]);
                    String unit = parts[1];
                    String value = String.valueOf(unitConversionMap.get(unit)* numericValue);
                    cdm.setPieceSize(value.split("\\.")[0]);
                    return value.split("\\.")[0];
                }else{
                    cdm.setPieceSize(cc3piecesize);
                    return cc3piecesize;
                }
            }
        }
        else
        {
            cdm.setPieceSize("NA");
        }
        return "NA";
    }

    private void enrichData(ProductDetails cdm, JsonNode extendedAttributes) {
        //cc7 PACKAGE
        if (extendedAttributes.has(PACKAGE)) {
            String cc7 = extendedAttributes.get(PACKAGE).asText();
            List<GenericEntity> cc7map = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "7", cc7);
            if(!cc7map.isEmpty()) {
                String code7 = cc7map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put(PACKAGE, code7);
            }
        }

        //cc8 trademark

        if (extendedAttributes.has(TRADE_MARK)) {
            String cc8 = extendedAttributes.get(TRADE_MARK).asText();
            List<GenericEntity> cc8map = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "8", cc8);
            if(!cc8map.isEmpty()) {
                String code8 = cc8map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put(TRADE_MARK, code8);
            }
        }

        //cc9 brand

        // cc10 flavour

        String flavour = cdm.getFlavour();
        if (!ObjectUtils.isEmpty(flavour)) {
            List<GenericEntity> flavourMapping = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "10", flavour);
            if (!flavourMapping.isEmpty()) {
                String cc10flavour = flavourMapping.get(0).getKey3();
                cdm.setFlavour(cc10flavour);
            }
        }
        else
        {
            cdm.setFlavour("NA");
        }

        //cc11 and cc12
        if (extendedAttributes.has(PACK_TYPE)) {
            String cc11 = extendedAttributes.get(PACK_TYPE).asText();
            List<GenericEntity> cc11map = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING, "11", cc11);
            if(!cc11map.isEmpty()) {
                String code11 = cc11map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put(PACK_TYPE, code11);
            }
        }

        if (extendedAttributes.has(Malt_grp_2)){
            String cc12 = extendedAttributes.get(Malt_grp_2).asText();
            List<GenericEntity> cc12map = repository.findByNameAndKey1AndKey2(PRODUCT_MAPPING,"12",cc12);
            if(!cc12map.isEmpty()){
                String code12 = cc12map.get(0).getKey3();
                ((ObjectNode) extendedAttributes).put(Malt_grp_2,code12);
            }
        }
    }
}
