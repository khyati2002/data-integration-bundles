package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.enrichments.Status;
import com.applicate.services.channelkart.models.DeliveryPJP;
import com.applicate.services.channelkart.models.GenericEntity;
import com.applicate.services.channelkart.models.RouteInfo;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.RouteInfoService;
import com.applicate.services.channelkart.services.SpringContext;

import java.util.List;

public class OutletSalesManEnrichment extends AbstractEnrichment<DeliveryPJP> {

    @Override
    public EnrichmentResult apply(DeliveryPJP deliveryPJP) {
        try {
            RouteInfoService routeInfoService = SpringContext.getBean(RouteInfoService.class);
            GenericEntityService genericEntityService = SpringContext.getBean(GenericEntityService.class);

            String routeCode = deliveryPJP.getBeat();
            String routeType = null;

            if (deliveryPJP.getExtendedAttributes() != null &&
                    deliveryPJP.getExtendedAttributes().get("routeType") != null) {
                routeType = deliveryPJP.getExtendedAttributes().get("routeType").asText();
            }

            if (routeCode == null || routeCode.isEmpty() || routeType == null) {
                return new EnrichmentResult(Status.ERROR, "Missing routeCode or routeType");
            }

            List<GenericEntity> entities = genericEntityService
                    .readModelsByNameAndKey1AndKey2("OM16_Route", routeType, routeCode);

            if (entities == null || entities.isEmpty()) {
                return new EnrichmentResult(Status.ERROR, "Salesman code not found");
            }

            String salesmanCode = entities.get(0).getKey3();

            if (salesmanCode == null || salesmanCode.isEmpty()) {
                return new EnrichmentResult(Status.ERROR, "Salesman code is empty");
            }
            deliveryPJP.setLoginId(salesmanCode);

            RouteInfo routeInfo = new RouteInfo();
            routeInfo.setRouteCode(routeCode);
            routeInfo.setRouteType(routeType);
            routeInfo.setSalesmanCode(salesmanCode);
            routeInfo.setOutletCode(deliveryPJP.getOutletCode());
            routeInfo.setSupplier(deliveryPJP.getExtendedAttributes().get("depo").asText());
            try{
                routeInfoService.save(routeInfoService.refresh(routeInfo));
            } catch (Exception e){
                return new EnrichmentResult(Status.ERROR, "Unable to save route info while forming delivery pjp");
            }

            return new EnrichmentResult(Status.OK, "Salesman code enriched successfully.");

        } catch (Exception e) {
            return new EnrichmentResult(Status.ERROR, e.getMessage());
        }
    }

}
