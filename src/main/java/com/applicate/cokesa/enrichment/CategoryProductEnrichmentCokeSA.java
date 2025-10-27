package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.jooq.impl.CategoryInfo;
import com.applicate.services.channelkart.services.CategoryInfoService;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.jooq.generated.tables.pojos.Productdetails;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CategoryProductEnrichmentCokeSA extends AbstractEnrichment<Productdetails> {
    private static final Pattern VOLUME_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([A-Za-z]+)");

    @Override
    public OperationResult.StepResult apply(Productdetails product) {
        String authToken= SecurityContextUtils.getPrincipal();
        if("integration_user".equals(authToken)) {
            CategoryInfoService categoryInfoService = (CategoryInfoService) ServiceLocator.lookup(CategoryInfo.class);
            try {
                List<String> categoryList = splitByHyphen(product.getCategory());
                List<CategoryInfo> category = categoryInfoService.findByCategoryCodeAndFeature(categoryList.get(0), categoryList.get(1));
                product.setCategory(category.get(0).getCategoryValue());
                product.setSubCategory(category.get(0).getCategoryValue());
                String packType = category.get(0).getCategoryValue();

                List<String> brandList = splitByHyphen(product.getBrand());
                List<CategoryInfo> brand = categoryInfoService.findByCategoryCodeAndFeature(brandList.get(0), brandList.get(1));
                product.setBrand(brand.get(0).getCategoryValue());
                String brandCode = product.getBrand();

                List<String> pieceSizeList = splitByHyphen(product.getPieceSize());
                List<CategoryInfo> piece = categoryInfoService.findByCategoryCodeAndFeature(pieceSizeList.get(0), pieceSizeList.get(1));
                String toMill = toMillilitres(piece.get(0).getCategoryValue());
                product.setPieceSize(toMill);
                product.setSize(toMill);
                Double packSize = Double.parseDouble(product.getSize());

                List<String> pieceSizeDescList = splitByHyphen(product.getPieceSizeDesc());
                List<CategoryInfo> pieceSizeDesc = categoryInfoService.findByCategoryCodeAndFeature(pieceSizeDescList.get(0), pieceSizeDescList.get(1));
                product.setPieceSizeDesc(pieceSizeDesc.get(0).getCategoryValue());

                List<String> itemList = splitByHyphen(product.getItemClass());
                List<CategoryInfo> itemClass = categoryInfoService.findByCategoryCodeAndFeature(itemList.get(0), itemList.get(1));
                product.setItemClass(itemClass.get(0).getCategoryValue());

                List<String> flavourList = splitByHyphen(product.getFlavour());
                List<CategoryInfo> flavour = categoryInfoService.findByCategoryCodeAndFeature(flavourList.get(0), flavourList.get(1));
                product.setFlavour(flavour.get(0).getCategoryValue());
                String flavourCode = product.getFlavour();

                String mCode = generateParentCode(flavourCode, brandCode, packType, packSize);
                product.setMCode(mCode);

                return new OperationResult.StepResult(OperationResult.Status.OK, "Category Enriched Successfully");
            } catch (Exception e) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage() + "Missing Category");
            }
        }
        else return OperationResult.StepResult.OK;
    }

    private String generateParentCode(String flavour, String brand, String packType, Double packSize) {

        String sizeBucket;
        if (packSize >= 10000) sizeBucket = "10000";
        else if (packSize >= 2500) sizeBucket = "2500";
        else if (packSize >= 1750) sizeBucket = "1750";
        else if (packSize > 750) sizeBucket = "1000";
        else if (packSize >= 500) sizeBucket = "500";
        else if (packSize >= 400) sizeBucket = "400";
        else if (packSize >= 300) sizeBucket = "300";
        else if (packSize >= 250) sizeBucket = "250";
        else if (packSize >= 200) sizeBucket = "200";
        else sizeBucket = "200";

        return brand + "_" + flavour + "_" + packType + "_" + sizeBucket;
    }

    private List<String> splitByHyphen(String input) {
        List<String> result = new ArrayList<>(2);
        if (NullUtils.isNull(input)) {
            result.add("");
            result.add("");
            return result;
        }
        String[] parts = input.split("-", 2);
        result.add(parts[0]);
        result.add(parts.length > 1 ? parts[1] : "");
        return result;
    }

    private String toMillilitres(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Volume input is null or empty");
        }
        Matcher m = VOLUME_PATTERN.matcher(input.trim().toUpperCase());
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid volume format: " + input);
        }
        BigDecimal value = new BigDecimal(m.group(1));
        String unit = m.group(2);

        BigDecimal ml;
        switch (unit) {
            case "ML":
                ml = value;
                break;
            case "L":
            case "LTR":
            case "LITER":
            case "LITRE":
                ml = value.multiply(BigDecimal.valueOf(1000));
                break;
            default:
                throw new IllegalArgumentException("Unknown volume unit: " + unit);
        }

        // strip any trailing .0 for whole numbers
        return ml.stripTrailingZeros().toPlainString();
    }
}