package com.applicate.cokesa.transformer;

import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DeliveryInfoAddAdditionalData
 */
public class DeliveryInfoAddAdditionalData extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {
    private final Logger logger = LoggerFactory.getLogger(DeliveryInfoAddAdditionalData.class);
    private static final String DELIVERY_DATE = "deliveryDate";
    private static final String FEATURES = "features";

    private final EntityUtils em = SpringContext.getBean(EntityUtils.class);
    private final OutletDetailsService outletDetailsService = SpringContext.getBean(OutletDetailsService.class);
    private final UserService userService = SpringContext.getBean(UserService.class);

    @Override
    public Object transform(Map<String, Object> dataMap) {
        ObjectMapper mapper = new ObjectMapper();
        if (NullUtils.isNotNull(dataMap)) {
            try {
                ArrayList<Map<String, Object>> features = mapper.convertValue(dataMap.get(FEATURES), new TypeReference<ArrayList<Map<String, Object>>>() {
                });
                ArrayList<Map<String, Object>> result = new ArrayList<>();
                for (Map<String, Object> feature : features) {
                    String salR = String.valueOf(feature.getOrDefault("loginId", ""));
                    User user = userService.findByLoginId(salR);
                    feature.put("salesRepId", salR);
                    feature.put("salesRepName", user.getName());
                    result.add(transformNode(feature));
                }
                dataMap.put(FEATURES, result.toArray());
                return dataMap;
            } catch (Exception ex) {
                logger.error("Transformer Exception", ex);
            }
        } else {
            throw new NullPointerException("Either  specification/input json found null");
        }
        return dataMap;
    }

    private Map<String, Object> transformNode(Map<String, Object> dataMap) throws ParseException {
        AtomicReference<Double> totalCaseQuantity = new AtomicReference<>(0d);
        AtomicReference<Double> totalPieceQuantity = new AtomicReference<>(0d);
        AtomicReference<Double> totalOtherQuantity = new AtomicReference<>(0d);
        AtomicReference<Integer> lineCount = new AtomicReference<>(0);

        List<Map<String, Object>> salesDetails;
        try {
            salesDetails = (List<Map<String, Object>>) Optional.ofNullable(dataMap.get("salesDetails")).orElse(new HashMap<>());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return dataMap;
        }

        String outletCode = dataMap.get("outletCode").toString();
        if(outletCode != null && !outletCode.isEmpty()) {
            // Get outlet details using outletCode
            OutletDetails outletDetails = outletDetailsService.findByOutletCode(outletCode);
            dataMap.put("outletName", outletDetails != null ? outletDetails.getOutletName() : null);
        }

        // Calculate total quantities by iterating over sales details
        salesDetails.forEach(salesDetail -> {
            double initialCaseQuantity = getSafeDoubleValue(salesDetail, "initialCaseQuantity");
            double initialPieceQuantity = getSafeDoubleValue(salesDetail, "initialPieceQuantity");
            double initialOtherQuantity = getSafeDoubleValue(salesDetail, "initialOtherUnitQuantity");

            if(initialCaseQuantity + initialPieceQuantity + initialOtherQuantity > 0) {
                lineCount.updateAndGet(v -> v + 1);
            }

            totalCaseQuantity.updateAndGet(v -> v + initialCaseQuantity);
            totalPieceQuantity.updateAndGet(v -> v + initialPieceQuantity);
            totalOtherQuantity.updateAndGet(v -> v + initialOtherQuantity);
        });

        dataMap.put("totalCaseQuantity", totalCaseQuantity.get());
        dataMap.put("totalPieceQuantity", totalPieceQuantity.get());
        dataMap.put("totalOtherUnitQuantity", totalOtherQuantity.get());
        dataMap.put("lineCount", lineCount.get());

        dataMap.put("totalAmount", getSafeDoubleValue(dataMap, "initialAmount"));

        return dataMap;
    }

    // Helper method to safely get double value or return 0 if field doesn't exist
    private double getSafeDoubleValue(Map<String, Object>  node, String fieldName) {
        return node.containsKey(fieldName) ? Double.parseDouble(Optional.ofNullable(node.get(fieldName)).orElse("0").toString()) : 0d;
    }
}
