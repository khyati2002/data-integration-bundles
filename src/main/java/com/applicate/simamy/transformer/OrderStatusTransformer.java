package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.services.OrderService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.Orders;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OrderStatusTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {


    OrderService orderService = (OrderService) ServiceLocator.lookup(Orders.class);
    private Logger logger = LoggerFactory.getLogger(OrderStatusTransformer.class);

    @Override
    public Map<String,Object> transform(Map<String, Object> ObjectMap) {
        ArrayNode notebookMessage = JSONUtils.getObjectMapper().convertValue(ObjectMap.get("NotebookMessage"), ArrayNode.class);
        String orderNumber = notebookMessage.get(0).asText();

        Orders ord = orderService.findFullLoadedOrder(orderNumber);
        if (ObjectUtils.isEmpty(ord)) {

            logger.error("found no order record for order number : {}", orderNumber);
            throw new DataTransformationService.TransformationException("Not a cokebuddy order status");
        } else {

            if (ObjectMap.get("MainProcessingStatus") == null) {

                String SubProcessingStatus1 = ObjectMap.get("SubProcessingStatus1").toString();
                if (ObjectUtils.isNotEmpty(ord)) {
                    switch (SubProcessingStatus1) {
                        case "UNP":
                        case "REJECTED":
                            ord.setStatus("REJECTED");
                            break;
                        case "HLD":
                        case "HOLD":
                            ord.setStatus("Hold");
                            break;

                    }
                    ObjectNode extended = JSONUtils.getObjectMapper().convertValue(ord.getExtendedAttributes(), ObjectNode.class);
                    extended.put("TransactionNumber", String.valueOf(ObjectMap.get("TransactionNumber")));
                    extended.put("nr", "1");
                    ord.setExtendedAttributes(JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class));
                    logger.info("data transformed successfully for : {}", orderNumber);
                    return JSONUtils.getObjectMapper().convertValue(ord,Map.class);
                }
            }

            String mainProcessingStatus = ObjectMap.get("MainProcessingStatus").toString();

            switch (mainProcessingStatus) {
                case "OEB":
                case "CONFIRMED":
                    ord.setStatus("CONFIRMED");
                    break;
                case "INF":
                case "OUT FOR DELIVERY":
                    ord.setStatus("out for delivery");
                    break;
                case "SMF":
                case "DELIVERED":
                    ord.setStatus("Delivered");
                    break;

            }
            ObjectNode extended = JSONUtils.getObjectMapper().convertValue(ord.getExtendedAttributes(), ObjectNode.class);
            extended.put("nr", "1");
            extended.put("TransactionNumber", String.valueOf(ObjectMap.get("TransactionNumber")));
            ord.setExtendedAttributes(JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class));
            return JSONUtils.getObjectMapper().convertValue(ord,Map.class);
        }
    }
}








