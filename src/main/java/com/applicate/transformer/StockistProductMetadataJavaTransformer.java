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

			output.put(SYS_SKU_CODE, input.get(SYS_SKU_CODE));
			output.put("productName", input.get("ProductName"));
			output.put("brand", input.get("Brand"));
			output.put("category", input.get("Category"));
			output.put("channel", "All");

			extractPackAndCasePointers(output, input);

			if (productDetails != null && input.containsKey("MRP")) {
				BigDecimal caseToPieceQuantityBD = productDetails.getCaseToPieceQuantity();
				Float caseToPieceQuantity = (caseToPieceQuantityBD != null)
						? caseToPieceQuantityBD.floatValue()
						: null;

				Float mrpPerPiece = Float.parseFloat(input.get("MRP").toString());
				if (caseToPieceQuantity != null) {
					Float caseMrp = caseToPieceQuantity * mrpPerPiece;
					output.put("caseMrp", caseMrp);
				}
			}

			Map<String, Object> extendedAttributes = getExtendedAttributesObj(input);
			output.put("extendedAttributes", extendedAttributes);

			data.add(output);
		} catch (Exception ex) {
			logger.error("Unexpected error transforming product metadata for SysSkuCode: {}",
					input != null ? input.get(SYS_SKU_CODE) : "unknown", ex);
		}

		return data;
	}

	/**
	 * Extracts packPtr (PACPTR) and casePtr (CFCPTR) from the input map.
	 */
	private void extractPackAndCasePointers(Map<String, Object> targetMap, Map<String, Object> sourceInput) {
		try {
			if (sourceInput.containsKey(PACPTR) && sourceInput.get(PACPTR) != null) {
				targetMap.put("packPtr", sourceInput.get(PACPTR).toString());
			}
			if (sourceInput.containsKey(CFCPTR) && sourceInput.get(CFCPTR) != null) {
				targetMap.put("casePtr", sourceInput.get(CFCPTR).toString());
			}
		} catch (Exception e) {
			logger.error("Error extracting PACPTR/CFCPTR for SysSkuCode: {}", sourceInput.get(SYS_SKU_CODE), e);
		}
	}

	/**
	 * Builds the extendedAttributes section with extra metadata.
	 */
	private Map<String, Object> getExtendedAttributesObj(Map<String, Object> input) {
		Map<String, Object> extAttr = new LinkedHashMap<>();
		extAttr.put("division", input.get("Division"));
		extAttr.put("subCategory", input.get("SubCategory"));
		extAttr.put("variant", input.get("Variant"));
		extAttr.put("uom", input.get("UOM"));
		return extAttr;
	}
}
