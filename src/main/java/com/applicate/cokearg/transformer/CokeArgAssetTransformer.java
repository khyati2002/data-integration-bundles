package com.applicate.cokearg.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CokeArgAssetTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> transform(Map<String, Object> inputMap) {
        Map<String, Object> responseMap = new HashMap<>();

        responseMap.put("outletCode",inputMap.get("cliente"));
        responseMap.put("loginId",inputMap.get("cliente"));
        responseMap.put("serialID", UUID.randomUUID().toString());
        responseMap.put("remarks","cooler");

        ObjectNode extendedAttributes = objectMapper.createObjectNode();
        extendedAttributes.put("clavemod", inputMap.get("clavemod").toString());
        extendedAttributes.put("modelo", inputMap.get("modelo").toString());
        extendedAttributes.put("serie",inputMap.get("serie").toString());
        responseMap.put("extendedAttributes", extendedAttributes);

        LocalDate localDate = LocalDate.of(1899, 12, 31)
                .plusDays(Integer.parseInt(inputMap.get("fecha").toString()));
        Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(date);
        responseMap.put("visitDate", formattedDate);
        
        return responseMap;
    }
    
}
