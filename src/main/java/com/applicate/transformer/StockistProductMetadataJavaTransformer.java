package com.applicate.transformer;

import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.ProductDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * Transformer for Stockist Product Metadata.
 */
public class StockistProductMetadataJavaTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

	public static final String CFCPTR = "CFCPTR";
	private static final Logger logger = LoggerFactory.getLogger(StockistProductMetadataJavaTransformer.class);
	private static final String SYS_SKU_CODE = "SysSkuCode";
	public static final String PACPTR = "PACPTR";

	@Override
	public List<Map<String, Object>> transform(Map<String, Object> input) {
		List<Map<String, Object>> data = new ArrayList<>();

		try {
			if (input == null || input.get(SYS_SKU_CODE) == null) {
				logger.warn("Input map or SysSkuCode is null — skipping transformation.");
				return data;
			}

			// Look up ProductDetailsService and fetch product details
			ProductDetailsService productDetailsService =
					(ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);
			ProductDetails productDetails = productDetailsService.findBySkuCode(input.get(SYS_SKU_CODE).toString());

			Map<String, Object> output = new LinkedHashMap<>();

			// --- Core Field Mappings ---
			putIfPresent(input, output, "MRP", "mrp");
			putIfPresent(input, output, "Tax", "tax");
			putIfPresent(input, output, "WDDEST", "supplier");

			// SysSkuCode maps to both skuCode and batchCode
			String sysSkuCode = input.get(SYS_SKU_CODE).toString();
			output.put("skuCode", sysSkuCode);
			output.put("batchCode", sysSkuCode);

			// Add channel (constant)
			output.put("channel", "All");

			// --- Extended Attributes (from JOLT spec) ---
			putIfPresent(input, extendedAttributes, "SBU", "SBU");
			putIfPresent(input, extendedAttributes, "UOM", "UOM");
			putIfPresent(input, extendedAttributes, "CatCode", "CatCode");
			putIfPresent(input, extendedAttributes, "CatName", "CatName");
			putIfPresent(input, extendedAttributes, "BrandCode", "BrandCode");
			putIfPresent(input, extendedAttributes, "BrandName", "BrandName");
			putIfPresent(input, extendedAttributes, "PACIN1CFC", "PACIn1CFC");
			putIfPresent(input, extendedAttributes, "MktSkuCode", "MktSkuCode");
			putIfPresent(input, extendedAttributes, "MktSkuName", "MktSkuName");
			putIfPresent(input, extendedAttributes, "SubCatCode", "SubCatCode");
			putIfPresent(input, extendedAttributes, "SubCatName", "SubCatName");
			putIfPresent(input, extendedAttributes, "SysSkuName", "SysSkuName");

			extractPackAndCasePointers(output, extendedAttributes, input);

			extendedAttributes.put("source_key", "integration");

			if (productDetails != null && input.containsKey("MRP")) {
				BigDecimal caseToPieceQtyBD = productDetails.getCaseToPieceQuantity();
				if (caseToPieceQtyBD != null) {
					Float caseToPieceQty = caseToPieceQtyBD.floatValue();
					Float mrpPerPiece = Float.parseFloat(input.get("MRP").toString());
					output.put("caseMrp", caseToPieceQty * mrpPerPiece);
				}
			}

			output.put("extendedAttributes", extendedAttributes);
			data.add(output);

		} catch (Exception ex) {
			logger.error("Unexpected error transforming product metadata for SysSkuCode: {}",
					input != null ? input.get(SYS_SKU_CODE) : "unknown", ex);
		}

		return data;
	}

	/**
	 * Helper: Copy a field from input to output if present.
	 */
	private void putIfPresent(Map<String, Object> input, Map<String, Object> output, String sourceKey, String targetKey) {
		if (input.containsKey(sourceKey) && input.get(sourceKey) != null) {
			output.put(targetKey, input.get(sourceKey));
		}
	}

	/**
	 * Extracts PACPTR/CFCPTR values and places them in both direct and extended attributes.
	 */
	private void extractPackAndCasePointers(Map<String, Object> targetMap,
											Map<String, Object> extendedAttributes,
											Map<String, Object> sourceInput) {
		try {
			if (sourceInput.containsKey(PACPTR) && sourceInput.get(PACPTR) != null) {
				String pacPtr = sourceInput.get(PACPTR).toString();
				targetMap.put("packPtr", pacPtr);
				extendedAttributes.put(PACPTR, pacPtr);
				extendedAttributes.put("PACPTS", pacPtr);
			}
			if (sourceInput.containsKey(CFCPTR) && sourceInput.get(CFCPTR) != null) {
				String cfcPtr = sourceInput.get(CFCPTR).toString();
				targetMap.put("casePtr", cfcPtr);
				extendedAttributes.put(CFCPTR, cfcPtr);
				extendedAttributes.put("CFCPTS", cfcPtr);
			}
		} catch (Exception e) {
			logger.error("Error extracting PACPTR/CFCPTR for SysSkuCode: {}", sourceInput.get(SYS_SKU_CODE), e);
		}
	}
}
