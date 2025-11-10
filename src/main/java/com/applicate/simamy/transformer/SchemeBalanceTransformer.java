package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.converters.DateToClientTimeZoneStringConverter;
import com.applicate.services.channelkart.event.service.EntityOperation;
import com.applicate.services.channelkart.exceptions.CustomRuntimeException;
import com.applicate.services.channelkart.exceptions.TransformationException;
import com.applicate.services.channelkart.schemes.services.external.UserDeltaCache;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.sync.model.DeltaLatestInfo;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class SchemeBalanceTransformer extends AbstractTransformer<Map<String, Object>, List<Map<String, Object>>> {

    private static final String SCHEME_ID = "schemeId";
    private static final String OUTLET_CODE = "outletCode";
    private static final String TOTAL_BALANCE = "totalBalance";
    private static final String AVAILABLE_BALANCE = "availableBalance";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String BALANCE = "BALANCE";
    private static final String MAX_COUNT = "MAX_COUNT";
    private static final String PROMO_CD = "PROMO_CD";
    private static final String CUST_CD = "CUST_CD";

    private final Logger logger = LoggerFactory.getLogger(SchemeBalanceTransformer.class);
    UserDeltaCache userDeltaCache = SpringContext.getBean(UserDeltaCache.class);

    @Override
    public List<Map<String, Object>> transform(Map<String, Object> inMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<Map<String, Object>> resultList = new ArrayList<>();

        try {
            JsonNode schemeMap = objectMapper.valueToTree(inMap.get("MST_PROMO_MAXCOUNT"));

            if (NullUtils.isNotNull(schemeMap) && schemeMap.has("MST_PROMO_MAXCOUNTBAL")) {
                    JsonNode maxCountBalArray = schemeMap.get("MST_PROMO_MAXCOUNTBAL");

                    for (JsonNode maxCountBalMap : maxCountBalArray) {
                        if (NullUtils.isNotNull(maxCountBalMap)) {
                            validateRequiredFields(maxCountBalMap);

                            Map<String, Object> transformedData = new HashMap<>();
                            transformedData.put(SCHEME_ID, maxCountBalMap.get(PROMO_CD).asText());
                            transformedData.put(OUTLET_CODE, maxCountBalMap.get(CUST_CD).asText());

                            int balance = getIntValue(maxCountBalMap, BALANCE);
                            int maxCount = getIntValue(maxCountBalMap, MAX_COUNT);

                            transformedData.put(AVAILABLE_BALANCE, balance);
                            transformedData.put(TOTAL_BALANCE, maxCount);

                            resultList.add(transformedData);
                        }
                    }
            }

            if (resultList.isEmpty()) {
                throw new TransformationException("No valid items found in MST_PROMO_MAXCOUNTBAL for SchemeBalanceTransformer");
            }

        } catch (Exception e) {
            logger.error("Error while transforming scheme balance data: {}", e.getMessage(), e);
            throw new TransformationException("Transformation failed for SchemeBalanceTransformer");
        }
        Map<String, Object> cacheData = new HashMap<>();
        for (Map<String, Object> data : resultList) {
            DeltaLatestInfo deletaInfo = null;
            try {
                deletaInfo = new DeltaLatestInfo("SchemeDefination", new SimpleDateFormat(DATE_TIME_FORMAT).parse(new DateToClientTimeZoneStringConverter().convert(new Date())), EntityOperation.INSERT);
            } catch (ParseException e) {
                throw new CustomRuntimeException(e);
            }
            cacheData.put("SchemeDefination_" + data.get(OUTLET_CODE).toString(), deletaInfo);
        }
        logger.info("adding to user_delta_changes");
        userDeltaCache.addToCache("user_delta_changes", cacheData);
        logger.info("added to user_delta_changes");
        return resultList;
    }

    private void validateRequiredFields(JsonNode maxCountBalNode) {
        if (!maxCountBalNode.has(PROMO_CD) || maxCountBalNode.get(PROMO_CD).isNull() || maxCountBalNode.get(PROMO_CD).asText().trim().isEmpty()) {
            throw new TransformationException("PROMO_CD is missing or empty");
        }

        if (!maxCountBalNode.has(CUST_CD) || maxCountBalNode.has(CUST_CD) && (maxCountBalNode.get(CUST_CD).isNull() || maxCountBalNode.get(CUST_CD).asText().trim().isEmpty())) {
            throw new TransformationException("CUST_CD is missing or empty");
        }

        if (!maxCountBalNode.has(MAX_COUNT) || maxCountBalNode.has(MAX_COUNT) && (maxCountBalNode.get(MAX_COUNT).isNull() || maxCountBalNode.get(MAX_COUNT).asText().trim().isEmpty())) {
            throw new TransformationException("MAX_COUNT is missing or empty");
        }

        if (!maxCountBalNode.has(BALANCE) || maxCountBalNode.has(BALANCE) && (maxCountBalNode.get(BALANCE).isNull() || maxCountBalNode.get(BALANCE).asText().trim().isEmpty())) {
            throw new TransformationException("BALANCE is missing or empty");
        }
    }

    private int getIntValue(JsonNode node, String key) {
        if (node.has(key) && node.get(key).isTextual()) {
            String value = node.get(key).asText().trim();
            if (!value.isEmpty()) {
                return Integer.parseInt(value);
            }
        }
        return 0;
    }

}
