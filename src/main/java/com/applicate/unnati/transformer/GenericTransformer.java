package com.applicate.unnati.transformer;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.etl.transformation.AbstractTransformer;

import java.util.Map;

public class GenericTransformer extends AbstractTransformer<Map<String,String>, CommonDataModel> {

    @Override
    public CommonDataModel transform(Map<String, String> stringStringMap) {
        return null;
    }
}
