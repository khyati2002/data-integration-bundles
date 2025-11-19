//package com.applicate.simamy.enrichment;
//
//import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
//import com.applicate.services.channelkart.enrichments.EnrichmentResult;
//import com.applicate.services.channelkart.models.Order;
//import com.applicate.services.channelkart.models.OrderDetails;
//import com.applicate.services.channelkart.models.ProductDetails;
//import com.applicate.services.channelkart.services.OrderService;
//import com.applicate.services.channelkart.services.ProductDetailsService;
//import com.applicate.services.channelkart.services.SpringContext;
//import com.applicate.services.channelkart.utils.JSONUtils;
//import com.applicate.services.channelkart.utils.NullUtils;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//
//public class SimaOrderDetailsEnrichment extends AbstractEnrichment <Order>{
//
//    public static final String PACK_PTR = "packPtr";
//    public static final String CASE_TO_PIECE_QUNATITY = "caseToPieceQuantity";
//    public static final String MRP = "mrp";
//    OrderService orderService = SpringContext.getBean(OrderService.class);
//
//    @Override
//    public EnrichmentResult apply(Order order) {
//        ProductDetailsService productDetailsService = SpringContext.getBean(ProductDetailsService.class);
//        var orderDetails = order.getOrderDetails();
//        for (OrderDetails orderDetail : orderDetails) {
//            ProductDetails productDetails = productDetailsService.getLoadedProductDetails(orderDetail.getBatchCode());
//            setCaseToPieceQuantity(orderDetail, productDetails);
//        }
//        return EnrichmentResult.OK;
//    }
//    private void setCaseToPieceQuantity(OrderDetails orderDetails, ProductDetails productDetails) {
//        if(orderDetails.getExtendedAttributes()!=null) {
//            if(NullUtils.isNull(orderDetails.getExtendedAttributes().get(CASE_TO_PIECE_QUNATITY))) {
//                ((ObjectNode) orderDetails.getExtendedAttributes()).put(CASE_TO_PIECE_QUNATITY, productDetails.getCaseToPieceQuantity());
//                ((ObjectNode) orderDetails.getExtendedAttributes()).put("blobKey", productDetails.getBlobKey() != null ? productDetails.getBlobKey() : "");
//            }
//        }else {
//            orderDetails.setExtendedAttributes(JSONUtils.getObjectMapper().createObjectNode().put(CASE_TO_PIECE_QUNATITY, productDetails.getCaseToPieceQuantity()).put("blobKey", productDetails.getBlobKey() != null ? productDetails.getBlobKey() : ""));
//        }
//    }
//
//
//}
