package com.applicate.transformer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.jooq.impl.ProductDetails;

public class StockistProductMetadataJavaTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

	private static final String SYS_SKU_CODE = "SysSkuCode";
	public static final String PACPTR = "PACPTR";
	public static final String CFCPTR = "CFCPTR";

	@Override
	public List<Map<String, Object>> transform(Map<String, Object> s) {

		ProductDetailsService productDetailsService = (ProductDetailsService) ServiceLocator.lookup(ProductDetails.class);
		ProductDetails productDetails = productDetailsService.findBySkuCode(s.get(SYS_SKU_CODE).toString());

		Map<String, Object> map = new HashMap<>();
		map.put("mrp", s.get("MRP"));
		map.put("tax", s.get("Tax"));
		map.put("supplier", s.get("WDDEST"));

		Map<String, Object> extendedAttributes = new HashMap<>();
		String[] keys = {"SBU", "UOM", "CatCode", "CatName", "BrandCode", "BrandName",
				"PACIN1CFC", "MktSkuCode", "MktSkuName", "SubCatCode", "SubCatName", "SysSkuName", "#integration"};
		for (String key : keys) {
			extendedAttributes.put(key.equals("#integration") ? "source_key" : key, s.get(key));
		}

		// CFCPTR / PACPTR
		if (s.containsKey(CFCPTR) && s.get(CFCPTR) != null) {
			extendedAttributes.put(CFCPTR, s.get(CFCPTR));
			extendedAttributes.put("CFCPTS", s.get(CFCPTR));
			map.put("casePtr", s.get(CFCPTR));
		}

		if (s.containsKey(PACPTR) && s.get(PACPTR) != null) {
			extendedAttributes.put(PACPTR, s.get(PACPTR));
			extendedAttributes.put("PACPTS", s.get(PACPTR));
			map.put("packPtr", s.get(PACPTR));
		}

		map.put("extendedAttributes", extendedAttributes);

		// SKU codes
		map.put("skuCode", s.get(SYS_SKU_CODE));
		map.put("batchCode", s.get(SYS_SKU_CODE));
		map.put("channel", "All");

		// Case MRP
		if (productDetails != null && s.get("MRP") != null) {
			BigDecimal caseToPieceQuantity = productDetails.getCaseToPieceQuantity();
			BigDecimal mrpPerPiece = new BigDecimal(s.get("MRP").toString());
			BigDecimal caseMrp = caseToPieceQuantity.multiply(mrpPerPiece);
			map.put("caseMrp", caseMrp.floatValue());
		}

		List<Map<String, Object>> data = new ArrayList<>();
		data.add(map);
		return data;
	}
}
