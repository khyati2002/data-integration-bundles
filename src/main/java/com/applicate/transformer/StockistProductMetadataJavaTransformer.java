package com.applicate.transformer;

import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.ProductDetails;

import static com.applicate.services.channelkart.models.enums.ActiveStatus.ACTIVE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

/**
 * Transformer for Stockist Product Metadata (pure Java version of JOLT spec).
 * Converts an input map into a normalized Product Metadata structure with extended attributes.
 */
public class StockistProductMetadataJavaTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

	public static final String CFCPTR = "CFCPTR";
	public static final String PACPTR = "PACPTR";
	private static final String SYS_SKU_CODE = "SysSkuCode";
	private static final Logger logger = LoggerFactory.getLogger(StockistProductMetadataJavaTransformer.class);

	/**
	 * Transforms raw input map into enriched Product Metadata format.
	 * - Validates presence of required SysSkuCode
	 * - Pulls product details from DB
	 * - Maps fields according to specification
	 * - Constructs extendedAttributes block
	 * - Performs business calculations (caseMrp)
	 * - Returns list containing a single transformed record
	 */
	@Override
	public List<Map<String, Object>> transform(Map<String, Object> input) {
		List<Map<String, Object>> data = new ArrayList<>();

		try {
			if (input == null || input.get(SYS_SKU_CODE) == null) {
				logger.warn("Input map or SysSkuCode is null — skipping transformation.");
				return data;
			}

			ProductDetailsService productDetailsService =
					(ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);
			ProductDetails productDetails = productDetailsService.findBySkuCode(input.get(SYS_SKU_CODE).toString());

			Map<String, Object> output = new LinkedHashMap<>();
			Map<String, Object> extendedAttributes = new LinkedHashMap<>();

			putIfPresent(input, output, "MRP", "mrp");
			putIfPresent(input, output, "Tax", "tax");
			putIfPresent(input, output, "WDDEST", "supplier");

			// SysSkuCode -> skuCode, batchCode
			String sysSkuCode = input.get(SYS_SKU_CODE).toString();
			output.put("skuCode", sysSkuCode);
			output.put("batchCode", sysSkuCode);

			// CFCPTR -> casePtr + extendedAttributes.CFCPTR + extendedAttributes.CFCPTS
			if (input.containsKey(CFCPTR) && input.get(CFCPTR) != null) {
				String cfcPtr = input.get(CFCPTR).toString();
				output.put("casePtr", cfcPtr);
				extendedAttributes.put(CFCPTR, cfcPtr);
				extendedAttributes.put("CFCPTS", cfcPtr);
			}

			// PACPTR -> packPtr + extendedAttributes.PACPTR + extendedAttributes.PACPTS
			if (input.containsKey(PACPTR) && input.get(PACPTR) != null) {
				String pacPtr = input.get(PACPTR).toString();
				output.put("packPtr", pacPtr);
				extendedAttributes.put(PACPTR, pacPtr);
				extendedAttributes.put("PACPTS", pacPtr);
			}

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

			extendedAttributes.put("source_key", "integration");

			if (productDetails != null && input.containsKey("MRP")) {
				BigDecimal caseToPieceQtyBD = productDetails.getCaseToPieceQuantity();
				if (caseToPieceQtyBD != null) {
					Float caseToPieceQty = caseToPieceQtyBD.floatValue();
					Float mrpPerPiece = Float.parseFloat(input.get("MRP").toString());
					output.put("caseMrp", caseToPieceQty * mrpPerPiece);
				}
			}

			output.put("channel", "All");
			output.put("extendedAttributes", extendedAttributes);
			output.put("activeStatus", ACTIVE);
			output.put("loginid",input.get("WDDEST").toString());
			data.add(output);

		} catch (Exception ex) {
			logger.error("Unexpected error transforming product metadata for SysSkuCode: {}",
					input != null ? input.get(SYS_SKU_CODE) : "unknown", ex);
		}

		return data;
	}

	/**
	 * Helper: copy field if present.
	 */
	private void putIfPresent(Map<String, Object> input, Map<String, Object> output, String sourceKey, String targetKey) {
		if (input.containsKey(sourceKey) && input.get(sourceKey) != null) {
			output.put(targetKey, input.get(sourceKey));
		}
	}
}
