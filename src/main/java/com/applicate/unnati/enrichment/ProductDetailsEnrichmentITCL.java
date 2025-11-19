package com.applicate.unnati.enrichment;

import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.ProductDetails;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ProductDetailsEnrichmentITCL extends AbstractEnrichment<ProductDetails> {

	@Override
	public EnrichmentResult apply(ProductDetails cdm) {
		String s = StringUtils.isEmpty(cdm.getSkuDescription()) ? cdm.getSkuName() : cdm.getSkuDescription() + "~~" + cdm.getMarketSku() + "~~" + cdm.getCategory() + "~~" + cdm.getSubCategory() + "~~" + (String.valueOf(cdm.getMrp()) == null ? "0" : formatMrp(cdm.getMrp()));
		float cMrp = Float.parseFloat(String.valueOf(cdm.getMrp()) == null ? "0" : String.valueOf(cdm.getMrp())) * Float.parseFloat(cdm.getCaseToPieceQuantity() == null ? "0" : String.valueOf(cdm.getCaseToPieceQuantity()));
		cdm.setCaseMrp(BigDecimal.valueOf(cMrp));
		cdm.setSuggestionText(s);
		return OperationResult.StepResult.OK;
	}

	/**
	 * Format MRP value: round to max 5 decimal places, remove trailing zeros, but keep at least 1 decimal place
	 * Examples: 12.050000 -> 12.05, 12.0502899 -> 12.05029, 12.0 -> 12.0
	 */
	private String formatMrp(BigDecimal mrp) {
		if (mrp == null) {
			return "0";
		}
		BigDecimal rounded = mrp.setScale(5, RoundingMode.HALF_UP);
		rounded = rounded.stripTrailingZeros();
		if (rounded.scale() < 1) {
			rounded = rounded.setScale(1, RoundingMode.UNNECESSARY);
		}
		return rounded.toPlainString();
	}
}
