package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.exceptions.CustomRuntimeException;
import com.applicate.services.channelkart.models.*;
import com.applicate.services.channelkart.services.*;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.DateUtils;
import com.applicate.services.channelkart.utils.EntityUtils;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

public class SimaOrders extends AbstractTransformer<List<Map<String, Object>>, Map<String, Object>>{

    private static final String COUPON = "COUPON";
    private static final String DISCOUNT = "discount";
    private static final String LINE_ITEM = "LineItem";
    private static final String LINE_ITEM_VALUE = "LineItemValue";
    private EntityUtils entityUtils = SpringContext.getBean(EntityUtils.class);

    public static final MetaDataService metaDataService = SpringContext.getBean(MetaDataService.class);
    public static final OutletDetailsService outletDetailsService = SpringContext.getBean(OutletDetailsService.class);
    public static final OrderDetailsService orderDetailsService = SpringContext.getBean(OrderDetailsService.class);
    public static final OrderService orderService = SpringContext.getBean(OrderService.class);
    public static final DeliveryPJPService deliveryPJPService = SpringContext.getBean(DeliveryPJPService.class);
    public static final GenericEntityService genericEntityService = SpringContext.getBean(GenericEntityService.class);
    private static final CustomerAccountsService customerAccountsService = SpringContext.getBean(CustomerAccountsService.class);

    @Override
    public Map<String, Object> transform(List<Map<String, Object>> dataMap) {
        MetaData supplier = metaDataService.fetchByValue("MigratedSuppliers","orderSubmit");
        Map<String,String> supplierList = JSONUtils.toStringMap(supplier.getDomainValues().get(0));
        Map<String, Object> exchangeObject = new HashMap<>();
        Map<String, Object> finalObject = new HashMap<>();

        dataMap.forEach(orderRecord -> {
            if(!supplierList.containsKey(orderRecord.get("supplierid"))) {
                Map<String, Object> orderRequest = new HashMap<>();
                Map<String, Object> skuInfo = new HashMap<>();
                Map<String, Object> orderHeaderInformation = new HashMap<>();
                Map<String, Object[]> orderTupleTable = new HashMap<>();
                Object[] notebookMessage = new Object[3];
                ArrayList<Object> tuple = new ArrayList<>();
                ArrayNode discountInfoList;

                OutletDetails outletRecord = outletDetailsService.findByOutletCode(orderRecord.get("outletcode").toString());

                String salesRoute = outletRecord.getExtendedAttributes().get("sales_route").asText();

                Object orderNumber = orderRecord.get("order_number");

                notebookMessage[0] = orderNumber;
                notebookMessage[1] = orderNumber;
                notebookMessage[2] = "CokeBuddy";

                orderHeaderInformation.put("CallingLocation", orderRecord.get("supplierid"));
                orderHeaderInformation.put("CallingDate", getCreationDate(orderRecord));
                orderHeaderInformation.put("CallingRoute", "");
                orderHeaderInformation.put("SalesmanNumber", salesRoute);
                orderHeaderInformation.put("OutletNumber", orderRecord.get("outletcode"));
                orderHeaderInformation.put("NotebookMessage", notebookMessage);
                orderRequest.put("OrderHeaderInformation", orderHeaderInformation);

                List<OrderDetails> orderDetailsList = orderDetailsService.findByOrderId(orderNumber.toString() + "-" + orderRecord.get("supplierid"));
                orderDetailsList.forEach(orderDetail -> {
                    tuple.add(Map.of(LINE_ITEM, orderDetail.getSkuCode(), LINE_ITEM_VALUE, orderDetail.getCaseQuantity()));
                    if (orderDetail.getDiscountInfo() != null) {
                        orderDetail.getDiscountInfo().forEach(discountInfo -> {
                            tuple.add(Map.of(LINE_ITEM, ".51", LINE_ITEM_VALUE, discountInfo.get("discountId").asText()));
                        });
                    }
                });

                try {
                    discountInfoList = JSONUtils.getObjectMapper().readValue(orderRecord.get("discount_info").toString(), ArrayNode.class);
                } catch (JsonProcessingException e) {
                    throw new CustomRuntimeException(e);
                }
                BigDecimal couponValue = BigDecimal.ZERO;
                if (NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()) {
                    for (JsonNode discountInfo : discountInfoList) {
                        String discountType = discountInfo.has("discountType") ? discountInfo.get("discountType").asText() : "";
                        String programLevel = discountInfo.has("programLevel") ? discountInfo.get("programLevel").asText() : "";
                        if (!discountType.equalsIgnoreCase("Summary") && programLevel.equalsIgnoreCase(COUPON)) {
                            couponValue = couponValue.add(BigDecimal.valueOf(Double.parseDouble(discountInfo.has(DISCOUNT) ? discountInfo.get(DISCOUNT).asText() : "0")));
                            tuple.add(Map.of(LINE_ITEM, ".93", LINE_ITEM_VALUE, couponValue.toString()));
                        }
                    }
                }
                orderTupleTable.put("Tuple", tuple.toArray());
                orderRequest.put("OrderTupleTable", orderTupleTable);
                exchangeObject.put("OrderRequest", orderRequest);
                finalObject.put("ExchangeObject", exchangeObject);
            }
            else{
                OutletDetails outletRecord = outletDetailsService.findByOutletCode(orderRecord.get("outletcode").toString());
                Map<String, Object> orderHeader = new HashMap<>();
                Object orderNumber = orderRecord.get("order_number").toString();

                orderHeader.put("DIST_CD", orderRecord.get("supplierid").toString());
                orderHeader.put("PO_NUMBER",orderNumber.toString() );
                String originalTimestamp = orderRecord.get("creation_time").toString();
                String date = getDate(originalTimestamp);

                orderHeader.put("TXN_DT", date);

                JsonNode exAtt = outletDetailsService.findByOutletCode(orderRecord.get("outletcode").toString()).getExtendedAttributes();
                String region = exAtt.get("sales_route").asText();
                List<GenericEntity> ldm = genericEntityService.readModelsByName("LeadTimeMapping");
                for(GenericEntity ent : ldm){
                    if(ent.getKey4()!=null)
                    if(region.charAt(0)==ent.getKey4().charAt(0))
                    {
                        region=ent.getKey2();
                        break;
                    }
                }

                String delDt = JSONUtils.convert(deliveryPJPService.getDeliveryDate("region",region).getBody(),Map.class).get("deliveryDate").toString();
                orderHeader.put("DELIVERY_DT", delDt);
                orderHeader.put("REMARK",orderRecord.get("remarks"));
                orderHeader.put("SLSMAN_CD", outletRecord.getExtendedAttributes().get("sales_route").asText());
                orderHeader.put("CUST_CD", orderRecord.get("outletcode").toString());
                orderHeader.put("TXN_NO", orderNumber.toString());
                orderHeader.put("ORDER_SRC", "CokeBuddy");
                ObjectNode ea = JSONUtils.convert(orderRecord.get("extended_attributes").toString(), ObjectNode.class);
                orderHeader.put("RECO_ID",ea.get("recommendationId")!=null?ea.get("recommendationId").asText():null);
                BigDecimal couponValue = BigDecimal.ZERO;
                ArrayNode discountInfoList;


                try {
                    discountInfoList = JSONUtils.getObjectMapper().readValue(orderRecord.get("discount_info").toString(), ArrayNode.class);
                } catch (JsonProcessingException e) {
                    throw new CustomRuntimeException(e);
                }
                if (NullUtils.isNotNull(discountInfoList) && !discountInfoList.isEmpty()) {

                    for (JsonNode discountInfo : discountInfoList) {
                        String discountType = discountInfo.has("discountId") ? discountInfo.get("discountId").asText() : "";
                        String programLevel = discountInfo.has("programLevel") ? discountInfo.get("programLevel").asText() : "";
                        if (!(discountType.equalsIgnoreCase("Summary")) && programLevel.equalsIgnoreCase(COUPON)) {
                            couponValue = couponValue.add(BigDecimal.valueOf(Double.parseDouble(discountInfo.has(DISCOUNT) ? discountInfo.get(DISCOUNT).asText() : "0")));
                            orderHeader.put("COUPON_CD",discountType);
                            orderHeader.put("COUPON_DESC",discountInfo.get("discountInfo"));
                            orderHeader.put("TOTAL_COUPON_AMT",Float.valueOf(String.valueOf(couponValue)));
                        }
                    }
                }
//                int i=1;
                List<OrderDetails> orderDetailsList = orderDetailsService.findByOrderId(orderNumber.toString() + "-" + orderRecord.get("supplierid"));
                List<Map> listOfSKU = new ArrayList<>();
                List<Map> listOfPromo = new ArrayList<>();
                int[] i = {1};
                orderDetailsList.forEach(orderDetail -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("PRD_CD",orderDetail.getSkuCode());
                    details.put("UOM_CD","CS");
                    details.put("PRD_QTY",(int) orderDetail.getInitialCaseQuantity());
                    details.put("UNIT_PRICE",orderDetail.getCasePrice());
                    details.put("GROSS_AMT",orderDetail.getInitialAmount());
                  details.put("PRD_INDEX",i[0]++);
//                    i++;
                    if(orderDetail.getNetAmount()== 0.0){
                        details.put("PRD_SLSTYPE","F");
                    }
                    else
                        details.put("PRD_SLSTYPE","S");

                    listOfSKU.add(details);
                    if (orderDetail.getDiscountInfo() != null) {

                        orderDetail.getDiscountInfo().forEach(discountInfo -> {
                            Map<String,Object> promodetails = new HashMap<>();
                            Map<String, Object> discInfo = new HashMap<>();
                            promodetails.put("PROMO_CD",discountInfo.get("discountId").asText());
                            promodetails.put("PRD_CD",orderDetail.getSkuCode().toString());
                            promodetails.put("PROMO_DISC_DTL",JSONUtils.convert(discountInfo.get("discount"), Float.class));
                            listOfPromo.add(promodetails);
                        });
                    }
                });
                orderHeader.put("ORDER_PRD",listOfSKU);
                orderHeader.put("ORDER_PROMO_DTL",listOfPromo);
                finalObject.put("ORDER_HDR",orderHeader);
            }


        });
        return finalObject;
    }

    private static @NotNull String getDate(String originalTimestamp) {
        String date ="";
        try {
            date =new SimpleDateFormat("yyyy-MM-dd").format(DateUtils.convertToTimeZone(DateUtils.parse(originalTimestamp), ZoneId.of(customerAccountsService.getTimeZone())));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return date;
    }

    private String getCreationDate(Map<String, Object> orderRecord){
        try{
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date creationDate = inputFormat.parse(orderRecord.get("creation_time").toString());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
            return outputFormat.format(creationDate);
        } catch (Exception e){
            throw new CustomRuntimeException(e.getMessage());
        }

    }

}

