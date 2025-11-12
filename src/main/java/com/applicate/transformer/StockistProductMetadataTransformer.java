package com.applicate.transformer;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import com.applicate.services.channelkart.models.ProductDetails;
import com.applicate.services.channelkart.services.ProductDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.transformers.TransformerInfo;
import com.applicate.services.channelkart.transformers.impl.JoltTransformer;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ArrayNode;


public class StockistProductMetadataTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

	private static final String SYS_SKU_CODE = "SysSkuCode";
	private static Logger logger = LoggerFactory.getLogger(JoltTransformer.class);

	@Override
	public Object transform(Map<String, Object> s) {

		ProductDetailsService productDetailsService= SpringContext.getBean(ProductDetailsService.class);
		if(s.get(SYS_SKU_CODE)==null){
			throw new TransformationException("SysSkuCode cannot be empty or null");
		}
		ProductDetails productDetails=productDetailsService.findBySkuCode(s.get(SYS_SKU_CODE).toString());

		TransformerInfo transformerInfo= this.getTransformerInfo();
		ArrayNode codeNode= transformerInfo.getCode();
		if(NullUtils.isNotNull(codeNode) && NullUtils.isNotNull(s)) {
			try {
				Object spec= JsonUtils.jsonToObject(String.valueOf(codeNode));
				Chainr chainr = Chainr.fromSpec(spec);
				Object transformedOutput = chainr.transform(s);
				Map<String, Object> map = JSONUtils.getObjectMapper().readValue(JsonUtils.toPrettyJsonString(transformedOutput),new TypeReference<Map<String, Object>>(){});

				//Method call to add packPtr and casePtr
				extractPackAndCasePointers(map, s);

				map.put("channel", "All");

				if(productDetails!=null){
					Float caseToPieceQuantity=productDetails.getCaseToPieceQuantity();
					Float mrpPerPiece=Float.parseFloat((s.get("MRP").toString()));
					Float caseMrp=caseToPieceQuantity*mrpPerPiece;
					map.put("caseMrp",caseMrp);
				}

				List<Map<String, Object>> data = new ArrayList<>();
				data.add(map);

				return data;


			}catch(Exception ex) {
				throw new TransformationException("Failed to transform product metadata for SysSkuCode: " + s.get(SYS_SKU_CODE), ex);
			}
		}
		return null;
	}

	/**
	 * Extracts packPtr (PACPTR) and casePtr (CFCPTR) directly from the sourceInput
	 * map and adds them to the targetMap.
	 *
	 * @param targetMap The map to which the new fields will be added.
	 * @param sourceInput The original input map which contains the top-level keys.
	 */
	private void extractPackAndCasePointers(Map<String, Object> targetMap, Map<String, Object> sourceInput) {
		try {
			// Fetch packPtr from the "PACPTR" key in the source input.
			if (sourceInput.containsKey("PACPTR")) {
				Object packPtrValue = sourceInput.get("PACPTR");
				if (packPtrValue != null) {
					targetMap.put("packPtr", packPtrValue.toString());
				}
			}

			// Fetch casePtr from the "CFCPTR" key in the source input.
			if (sourceInput.containsKey("CFCPTR")) {
				Object casePtrValue = sourceInput.get("CFCPTR");
				if (casePtrValue != null) {
					targetMap.put("casePtr", casePtrValue.toString());
				}
			}
		} catch (Exception e) {
			logger.error("Error extracting PACPTR/CFCPTR for SysSkuCode: {}", sourceInput.get(SYS_SKU_CODE), e);
		}
	}

}