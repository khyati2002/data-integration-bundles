package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.DeliveryPJP;
import com.salescode.dim.jooq.impl.GenericEntity;

import java.util.List;

public class OutletSalesManEnrichment extends AbstractEnrichment<DeliveryPJP> {

    @Override
    public OperationResult.StepResult apply(DeliveryPJP deliveryPJP) {
        try {
            RouteInfoService routeInfoService = (RouteInfoService) ServiceLocator.lookup(RouteInfo.class);
            GenericEntityService genericEntityService = (GenericEntityService)ServiceLocator.lookup(GenericEntity.class);

            String routeCode = deliveryPJP.getBeat();
            String routeType = null;

            if (deliveryPJP.getExtendedAttributes() != null &&
                    deliveryPJP.getExtendedAttributes().get("routeType") != null) {
                routeType = deliveryPJP.getExtendedAttributes().get("routeType").asText();
            }

            if (routeCode == null || routeCode.isEmpty() || routeType == null) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "Missing routeCode or routeType");
            }

            List<GenericEntity> entities = genericEntityService
                    .readModelsByNameAndKey1AndKey2("OM16_Route", routeType, routeCode);

            if (entities == null || entities.isEmpty()) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "Salesman code not found");
            }

            String salesmanCode = entities.get(0).getKey3();

            if (salesmanCode == null || salesmanCode.isEmpty()) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "Salesman code is empty");
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
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "Unable to save route info while forming delivery pjp");
            }

            return new OperationResult.StepResult(OperationResult.Status.OK, "Salesman code enriched successfully.");

        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage());
        }
    }

}
