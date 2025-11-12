package com.applicate.kgbpl.enrichment;

import com.applicate.services.channelkart.services.CategoryInfoService;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.CategoryInfo;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.ProductDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ProductEnrichmentKgpl extends AbstractEnrichment<ProductDetails> {
    GenericEntityService genericEntityService = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);
    CategoryInfoService categoryInfoService = (CategoryInfoService) ServiceLocator.lookup(CategoryInfo.class);

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    private static final Map<String, Integer> unitConversionMap = new HashMap<>();
    static {
        unitConversionMap.put("ML", 1);
        unitConversionMap.put("ml", 1);
        unitConversionMap.put("L", 1000);
        unitConversionMap.put("l", 1000);
        unitConversionMap.put("LTR", 1000);
        unitConversionMap.put("ltr", 1000);
        unitConversionMap.put("liter", 1000);
        unitConversionMap.put("KG", 1000);
        unitConversionMap.put("kg", 1000);
    }

    @Override
    public OperationResult.StepResult apply(ProductDetails cdm) {
        List<CategoryInfo> brandList = categoryInfoService.findByCategoryCodeAndFeature(cdm.getBrandCode(), "brandcode");
        if (!brandList.isEmpty()) {
            cdm.setBrand(brandList.get(0).getCategoryValue());
        }

        List<CategoryInfo> flcdList = categoryInfoService.findByCategoryCodeAndFeature(cdm.getFlcd(), "flcd");
        if (!flcdList.isEmpty()) {
            cdm.setFlcd(flcdList.get(0).getCategoryValue());
        }

        List<CategoryInfo> szcdList = categoryInfoService.findByCategoryCodeAndFeature(cdm.getSzcd(), "size");
        if (!szcdList.isEmpty()) {
            cdm.setSzcd(szcdList.get(0).getCategoryValue());
        }

        List<CategoryInfo> flavourList = categoryInfoService.findByCategoryCodeAndFeature(cdm.getFlavour(), "brandCategory");
        if (!flavourList.isEmpty()) {
            cdm.setFlavour(flavourList.get(0).getCategoryValue());
        }

        List<CategoryInfo> subCategoryList = categoryInfoService.findByCategoryCodeAndFeature(cdm.getSubCategory(), "packType");
        if (!subCategoryList.isEmpty()) {
            cdm.setSubCategory(subCategoryList.get(0).getCategoryValue());
        }

        List<CategoryInfo> pieceSizeDescList = categoryInfoService.findByCategoryCodeAndFeature(cdm.getPieceSizeDesc(), "packtypegroup");
        if (!pieceSizeDescList.isEmpty()) {
            cdm.setPieceSizeDesc(pieceSizeDescList.get(0).getCategoryValue());
        }

        List<CategoryInfo> categoryList = categoryInfoService.findByCategoryCodeAndFeature(cdm.getCategoryCode(), "productSegment");
        if (!categoryList.isEmpty()) {
            cdm.setCategory(categoryList.get(0).getCategoryValue());
        }

        String pieceSizeInput = cdm.getPieceSize();

        List<CategoryInfo> sizeCategoryList = categoryInfoService.findByCategoryCodeAndFeature(pieceSizeInput, "packSize");

        if (!sizeCategoryList.isEmpty()) {
            String categoryValue = sizeCategoryList.get(0).getCategoryValue();
            String standardizedSize = convertPieceSize(categoryValue);

            if (!standardizedSize.equals("NA")) {
                cdm.setPieceSize(standardizedSize);
            } else {
                cdm.setPieceSize(categoryValue);
            }
        } else {
            cdm.setPieceSize("NA");
        }

        return new OperationResult.StepResult(OperationResult.Status.OK, "Product data enriched successfully");
    }

    private String convertPieceSize(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) return "NA";

        String[] parts = rawValue.trim().split(" ", 2);
        if (parts.length != 2) return "NA";

        String numberPart = parts[0].trim();
        String unit = parts[1].trim();

        if (!NUMBER_PATTERN.matcher(numberPart).matches()) return "NA";

        try {
            double numericValue = Double.parseDouble(numberPart);
            Integer factor = unitConversionMap.get(unit);
            if (factor == null) return "NA";

            return String.valueOf((int) Math.round(numericValue * factor));
        } catch (NumberFormatException e) {
            return "NA";
        }
    }
}