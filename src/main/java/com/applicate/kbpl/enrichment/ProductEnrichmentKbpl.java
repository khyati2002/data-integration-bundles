package com.applicate.kbpl.enrichment;

import com.applicate.services.channelkart.repository.GenericEntityRepository;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProductEnrichmentKbpl extends AbstractEnrichment<ProductDetails> {
    private static final String PRODUCT_MAPPING = "productMapping";

    @Override
    public EnrichmentResult apply(ProductDetails cdm) {
        GenericEntityRepository repository = (GenericEntityRepository) ServiceLocator.lookup(GenericEntity.class);
        Map<String, Map<String, String>> productMappingCache = repository.readModelsByName(PRODUCT_MAPPING).stream()
                .filter(entity -> StringUtils.isNotEmpty(entity.getKey1()) && StringUtils.isNotEmpty(entity.getKey2()) && StringUtils.isNotEmpty(entity.getKey3()))
                .collect(Collectors.groupingBy(
                        GenericEntity::getKey1,
                        Collectors.toMap(GenericEntity::getKey2, GenericEntity::getKey3)
                ));

        JsonNode extendedAttributes = cdm.getExtendedAttributes();
        String itemType = cdm.getItemType();
        String brand = cdm.getBrand();
        String category = cdm.getCategory();
        String packType = cdm.getPieceSizeDesc();
        String subCategory = cdm.getSubCategory();
        String flavour = cdm.getFlavour();
        String cat7 = extendedAttributes.get("categorycode7").asText();
        String pieceSize = cdm.getPieceSize();
        String taxGroup = extendedAttributes.get("taxgroupcode").asText();

        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        Map<String, Integer> unitConversionMap = new HashMap<>();
        unitConversionMap.put("ML", 1);
        unitConversionMap.put("ml", 1);
        unitConversionMap.put("l", 1000);
        unitConversionMap.put("L", 1000);
        unitConversionMap.put("liter", 1000);
        unitConversionMap.put("LTR", 1000);
        unitConversionMap.put("ltr", 1000);
        unitConversionMap.put("KG", 1000);
        unitConversionMap.put("kg", 1000);


        String cc1 = getMappedValue("1", itemType, productMappingCache);
        if (cc1 == null || !isValidItemType(cc1)) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, " Invalid Item type: " + itemType + " Only Finished Goods and Raw Material are valid");
        }
        cdm.setItemType(cc1);
        cdm.setDisplay(String.valueOf(!isValidDisplay(cc1)));

        String cc2 = getMappedValue("2", brand, productMappingCache);
        cdm.setBrand(cc2 != null ? cc2 : "NA");

        String cc3 = getMappedValue("3", category, productMappingCache);
        cdm.setCategory(cc3 != null ? cc3 : "OTHERS");

        String cc4 = getMappedValue("4", packType, productMappingCache);
        cdm.setPieceSizeDesc(cc4 != null ? cc4 : "NA");

        String cc5 = getMappedValue("5", subCategory, productMappingCache);
        cdm.setSubCategory(cc5 != null ? cc5 : "NA");

        String cc6 = getMappedValue("6", flavour, productMappingCache);
        cdm.setFlavour(cc6 != null ? cc6 : "NA");

        String cc7 = getMappedValue("7", cat7, productMappingCache);
        if (cc7 != null) {
            ((ObjectNode) extendedAttributes).put("categorycode7", cc7);
        }

        String cc8 = getMappedValue("8", pieceSize, productMappingCache);
        setPieceSize(cdm, cc8, pattern, unitConversionMap);

        if (!taxGroup.isEmpty()) {
            String tax = null;

            if (taxGroup.equals("GST28CESS12")) {
                tax = "GST40";
            }
            else if (taxGroup.matches("^GST\\d+$")) {
                tax = taxGroup; // taxGroup is valid, so keep it as is
            }

            ((ObjectNode) extendedAttributes).put("taxgroupcode", tax);
        }

        String mothercode = generateParentCode(cdm.getBrand(), cdm.getFlavour(), cdm.getSubCategory(), cdm.getPieceSize());
        cdm.setMCode(mothercode);

        return new OperationResult.StepResult(OperationResult.Status.OK);
    }

    private static void setPieceSize(ProductDetails cdm, String cc8, Pattern pattern, Map<String, Integer> unitConversionMap) {
        if (cc8 == null || cc8.trim().isEmpty()) {
            cdm.setPieceSize("NA");
            return;
        }
        String[] parts = cc8.split(" ", 2); // Ensures only two splits
        if (parts.length != 2) {
            cdm.setPieceSize("");
            return;
        }

        String numericPart = parts[0].trim();
        String unit = parts[1].trim();

        if (!pattern.matcher(numericPart).matches()) {
            cdm.setPieceSize("");
            return;
        }

        try {
            double numericValue = Double.parseDouble(numericPart);
            Integer conversionFactor = unitConversionMap.get(unit);

            if (conversionFactor == null) {
                cdm.setPieceSize("");
                return;
            }

            int convertedValue = (int) Math.round(numericValue * conversionFactor);
            cdm.setPieceSize(String.valueOf(convertedValue));

        } catch (NumberFormatException e) {
            cdm.setPieceSize("");
        }
    }

    private String getMappedValue(String key1, String key2, Map<String, Map<String, String>> productMappingCache) {
        return productMappingCache.getOrDefault(key1, new HashMap<>()).get(key2);
    }

    private boolean isValidItemType(String cc1) {
        return cc1.equalsIgnoreCase("Finished Goods") || cc1.equalsIgnoreCase("Raw Material") || cc1.equalsIgnoreCase("Other FG");
    }

    private boolean isValidDisplay(String cc1) {
        return cc1.equalsIgnoreCase("Finished Goods") || cc1.equalsIgnoreCase("Raw Material") || cc1.equalsIgnoreCase("Other FG");
    }

    public static String generateParentCode(String brand, String flavour, String packType, String pieceSize) {
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
            } else if (size >= 500) {
                unq = 500;
            } else if (size >= 400) {
                unq = 400;
            } else if (size >= 300) {
                unq = 300;
            } else if (size >= 250) {
                unq = 250;
            } else {
                unq = 200;
            }
        } catch (NumberFormatException e) {
            // Handle the case when pieceSize is not an integer
            unq = -1;
        }

        return brand + "_" +  flavour + "_" + packType + "_" + (unq == -1 ? "NA" : unq);
    }
}
