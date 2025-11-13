package com.applicate.unnati.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.commons.collections.MapUtils.*;

public class OrdersTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(OrdersTransformer.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_STATUS = "PENDING";
    private static final String DEFAULT_PROCESSING_STATUS = "0";
    private static final String ACTIVE = "active";

    @Override
    public Map<String, Object> transform(Map<String, Object> responseEnvelope) {
        Map<String, Object> output = new LinkedHashMap<>();

        try {
            // Map order number (primary identifier)
            String orderNumber = getString(responseEnvelope, "orderNumber");
            output.put("orderNumber", orderNumber);

            // Map status fields
            String status = getString(responseEnvelope, "status", DEFAULT_STATUS);
            output.put("status", status);

            String processingStatus = getString(responseEnvelope, "processingStatus", DEFAULT_PROCESSING_STATUS);
            output.put("processingStatus", processingStatus);

            output.put("statusReason", getString(responseEnvelope, "statusReason"));
            output.put("billAmount", 0);
            output.put("lineCount",0);
            output.put("netAmount",0);
            output.put("totalAmount",0);
            output.put("totalMrp",0);
            output.put("totalQuantity",0);
            output.put("normalizedQuantity",0);
            output.put("initialNormalizedQuantity",0);
            output.put("normalizedVolume",0);
            output.put("totalInitialAmount",0);

            // Map date fields
            output.put("deliveryDate", getFormattedDate(responseEnvelope, "deliveryDate"));
            output.put("salesDate", getFormattedDate(responseEnvelope, "salesDate"));

            // Map numeric fields
            output.put("salesValue", getDouble(responseEnvelope, "salesValue", 0.0));

            // Map beat and location fields
            output.put("groupId", getString(responseEnvelope, "groupId"));
            output.put("beat", getString(responseEnvelope, "beat"));
            output.put("beatName", getString(responseEnvelope, "beatName"));
            output.put("subType", getString(responseEnvelope, "subType"));

            // Map boolean flags
            output.put("inBeat", getBoolean(responseEnvelope, "inBeat", false));
            output.put("inRange", getBoolean(responseEnvelope, "inRange", false));

            // Map integration fields
            output.put("orderIntegrationToken", getString(responseEnvelope, "orderIntegrationToken"));

            // Set active status
            output.put("activeStatus", ACTIVE);
            output.put("activeStatusReason", ACTIVE);

            output.put("changed", 1);

            logger.debug("Successfully transformed Order for orderNumber: {}", orderNumber);

        } catch (Exception e) {
            logger.error("Failed to transform Order data", e);
            throw new RuntimeException("Order transformation failed", e);
        }

        return output;
    }

    private String getFormattedDate(Map<String, Object> map, String key) {
        try {
            Object dateObj = map.get(key);
            if (dateObj == null) {
                return null;
            }

            if (dateObj instanceof String) {
                return (String) dateObj;
            } else if (dateObj instanceof Date) {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                return sdf.format((Date) dateObj);
            } else if (dateObj instanceof Long) {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                return sdf.format(new Date((Long) dateObj));
            }
            return dateObj.toString();
        } catch (Exception e) {
            logger.warn("Failed to format date for key: {}", key, e);
            return null;
        }
    }
}