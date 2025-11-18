package com.applicate.cokeph.enrichment;

import com.applicate.services.channelkart.services.CategoryInfoService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.CategoryInfo;
import com.salescode.dim.jooq.impl.ProductDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ProductCategoryEnrichment extends AbstractEnrichment<ProductDetails> {
    CategoryInfoService repository;

    @Override
    public EnrichmentResult apply(ProductDetails cdm){
        repository = (CategoryInfoService) ServiceLocator.lookup(CategoryInfo.class);

        String authToken = SecurityContextUtils.getPrincipal();
        if (!"integration_user".equals(authToken)) {
            return new OperationResult.StepResult(OperationResult.Status.OK);
        }

        String itemType = cdm.getItemType();
        String brand = cdm.getBrand();
        String packType = cdm.getPieceSizeDesc();
        String pieceSize = cdm.getPieceSize();
        String category = cdm.getCategory();


        if (!StringUtils.isEmpty(itemType)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> itemTypeMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("1",itemType, "ProductMaster");
            if (!itemTypeMapping.isEmpty()) {
                String cc = itemTypeMapping.get(0).getNewDescription();
                cdm.setItemType(cc);
            }
        }

        if (!StringUtils.isEmpty(brand)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> brandMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("2",brand, "ProductMaster");
            if (!brandMapping.isEmpty()) {
                String cc = brandMapping.get(0).getNewDescription();
                cdm.setBrand(cc);
            }
        }
        if (!StringUtils.isEmpty(category)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> categoryMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("Beverage Category", category, "ProductMaster");
            if(!categoryMapping.isEmpty()){
                String cc = categoryMapping.get(0).getNewDescription();
                cdm.setCategory(cc);
            }
        }

        if (!StringUtils.isEmpty(packType)) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> packTypeMapping = repository.findByCategoryCodeAndCategoryValueAndFeature("3",packType, "ProductMaster");
            if (!packTypeMapping.isEmpty()) {
                String cc = packTypeMapping.get(0).getNewDescription();
                cdm.setPieceSizeDesc(cc);
            }
        }


        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        Map<String, Integer> unitConversionMap = new HashMap<>();
        unitConversionMap.put("ML", 1);
        unitConversionMap.put("ml", 1);
        unitConversionMap.put("MILLILITER",1);
        unitConversionMap.put("l", 1000);
        unitConversionMap.put("L", 1000);
        unitConversionMap.put("LTR", 1000);
        unitConversionMap.put("liter", 1000);
        unitConversionMap.put("KG", 1000);
        unitConversionMap.put("GM", 1);
        unitConversionMap.put("OZ", 30);
        unitConversionMap.put("GRAM",1);
        unitConversionMap.put("GAL",10000);
        unitConversionMap.put("gal",10000);
        unitConversionMap.put("Gal",10000);
        unitConversionMap.put("LB",10000);
        unitConversionMap.put("Lb",10000);
        unitConversionMap.put("lb",10000);

        handlePieceSize(pieceSize,cdm,pattern,unitConversionMap);


        String mc1 = cdm.getBrand();
        String mc3 = cdm.getPieceSizeDesc();
        String mc4 = cdm.getPieceSize();
        String mothercode = generate_parent_code(mc1,mc3,mc4);
        cdm.setMCode(mothercode);



        return new OperationResult.StepResult(OperationResult.Status.OK);


    }
    private void handlePieceSize(String pieceSize, ProductDetails cdm, Pattern pattern,Map<String, Integer> unitConversionMap) {
        if (pieceSize != null) {
            List<com.salescode.dim.jooq.generated.tables.pojos.CategoryInfo> mapping = repository.findByCategoryCodeAndCategoryValueAndFeature("4", pieceSize, "ProductMaster");
            if (!mapping.isEmpty()) {
                String desc = mapping.get(0).getNewDescription();
                String[] parts = desc.trim().split("\\s+|-");

                if (parts.length == 2 && pattern.matcher(parts[0]).matches()) {
                    Double numericValue = Double.parseDouble(parts[0]);
                    String unit = parts[1];
                    Integer finalPieceSize = (int) (numericValue * unitConversionMap.get(unit));
                    cdm.setPieceSize(String.valueOf(finalPieceSize));
                } else {
                    cdm.setPieceSize("NA");
                }
            }
        }
    }

    public static String generate_parent_code(String brand, String packType, String pieceSize) {
        int unq;
        try {
            int size = Integer.parseInt(pieceSize);
            if (size >= 10000) {
                unq = 10000;
            } else if (size >= 2500) {
                unq = 2500;
            } else if (size >= 1750) {
                unq = 1750;
            } else if (size >= 1000) {
                unq = 1000;
            } else if (size >= 750) {
                unq = 750;
            } else if (size >= 500) {
                unq = 500;
            } else if (size >= 400) {
                unq = 400;
            } else if (size >= 300) {
                unq = 300;
            } else if (size >= 250) {
                unq = 250;
            }else {
                unq = 200;
            }
        } catch (NumberFormatException e) {
            // Handle the case when pieceSize is not an integer
            unq = -1;
        }

        return brand + "_" + packType + "_" + (unq == -1 ? "NA" : unq);
    }
}
