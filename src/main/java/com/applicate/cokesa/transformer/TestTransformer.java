/*
 * Copyright (c) 2020. All rights reserved.
 * APPLICATE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * 
 */
package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.transformers.TransformerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TestTransformer extends AbstractTransformer<Map<String,Object>,Map<String,Object>> {

	private static Logger logger = LoggerFactory.getLogger(TestTransformer.class);

	@Override
	public Map<String,Object> transform(Map<String,Object> jsonobj) {
		TransformerInfo  transformerInfo= this.getTransformerInfo();
		return new HashMap<>();
	}

}
