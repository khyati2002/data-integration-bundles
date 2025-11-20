package com.applicate.kbpl.enrichment;

import com.applicate.services.channelkart.models.enums.ActiveStatus;
import com.applicate.services.channelkart.services.DeliveryPJPService;
import com.applicate.services.channelkart.services.GenericEntityService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.StringUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.EnrichmentResult;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.DeliveryPjp;
import com.salescode.dim.jooq.impl.GenericEntity;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PreTransformPJPEnrichment extends AbstractEnrichment<DeliveryPjp> {

    private static final String TENANT_CODE = "tenantcode";
    private static final String NAME = "RouteDetails";

    GenericEntityService genericEntityService ;
    UserService userService ;
    DeliveryPJPService pjpService ;


    @Override
    public EnrichmentResult apply(DeliveryPjp deliveryPJP) {
        genericEntityService = (GenericEntityService) ServiceLocator.lookup(GenericEntity.class);
        userService = (UserService) ServiceLocator.lookup(User.class);
        pjpService = (DeliveryPJPService) ServiceLocator.lookup(DeliveryPjp.class);

        try {
            if (!deliveryPJP.getExtendedAttributes().has(TENANT_CODE) || deliveryPJP.getExtendedAttributes().get(TENANT_CODE).asText().equalsIgnoreCase("null")) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "Cannot persist PJP without supplier value");

            } else if (ObjectUtils.isEmpty(deliveryPJP.getBeat())) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "Cannot persist PJP without journeyPlanCode");
            } else {
                mergePayloadToExtendedAttributes(deliveryPJP);
                updateLoginIdActiveStatus(deliveryPJP);
                updateMonthYearValues(deliveryPJP);
                EnrichmentResult result = new OperationResult.StepResult(OperationResult.Status.OK, "Data Enriched Successfully!!");
                //DeliveryPjp refreshedRecord = pjpService.refresh(deliveryPJP);
                result.setStepResultData(List.of(deliveryPJP));
                return result;
            }
        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage());
        }

    }


    /**
     * Updating Month and year value of PJP records from Current Date.
     *
     * @param deliveryPJP
     */
    private void updateMonthYearValues(DeliveryPjp deliveryPJP) {
        deliveryPJP.setMonth(LocalDate.now().getMonth().name());
        deliveryPJP.setYear(String.valueOf(LocalDate.now().getYear()));
    }


    /**
     * Enriching the pjpRecord with RouteMaster data.
     *
     * @param deliveryPJP pjpRecord
     */
    private void mergePayloadToExtendedAttributes(DeliveryPjp deliveryPJP) {
        GenericEntity genericEntity = findRecordByBeatCodeAndDistributor(deliveryPJP.getBeat(), deliveryPJP.getSupplierid(),deliveryPJP.getSource());

        JsonNode payload = genericEntity.getPayload();
        ObjectNode extendedAttributes = NullUtils.isNotNull(deliveryPJP.getExtendedAttributes()) ? (ObjectNode) deliveryPJP.getExtendedAttributes() : JSONUtils.getObjectMapper().createObjectNode();

        extendedAttributes.put("RouteCode", payload.get("routecode").asText());
        extendedAttributes.put("MGR", payload.get("salesmancode").asText());
        extendedAttributes.put("authorizeditemcode", payload.get("authorizeditemcode").asText());
        extendedAttributes.put("JPCode", payload.get("journeyplancode").asText());

        deliveryPJP.setExtendedAttributes(extendedAttributes);
    }


    /**
     * Finds Route Master record on the basis of routeCode and supplier and active status.
     *
     * @param beatCode    routeCode
     * @param distributor supplier
     * @return genericEntity record based on filtered keys.
     */

    private GenericEntity findRecordByBeatCodeAndDistributor(String beatCode, String distributor,String source) {
        List<GenericEntity> genericEntityList = genericEntityService.findByNameAndKey1AndKey2(NAME, beatCode, distributor);
        if (genericEntityList.isEmpty()) {
            throw new IllegalArgumentException(StringUtils.format("No record found in route Master for the given distributor {} journeyPlanCode {} ", distributor, beatCode));
        }
        Optional<GenericEntity> activeRouteMasterObject = genericEntityList.stream()
                .filter(genericEntity ->
                        genericEntity.getPayload().has("routetype") &&
                                (
                                        "4".equals(genericEntity.getPayload().get("routetype").asText()) ||
                                                ("enrich".equalsIgnoreCase(source) &&
                                                        "11".equals(genericEntity.getPayload().get("routetype").asText()))
                                ) &&
                                genericEntity.getActiveStatus() != null &&
                                genericEntity.getActiveStatus() == ActiveStatus.ACTIVE
                )
                .findFirst();
        return activeRouteMasterObject.orElseThrow(() -> {
            String allowedTypes = "enrich".equalsIgnoreCase(source) ? "4 or 11" : "4";
            return new IllegalArgumentException(StringUtils.format(
                    "No active route data found for supplier {} and beatCode {} with routeType {}",
                    distributor, beatCode, allowedTypes));
        });
    }

    /**
     * Finds Route Master record on the basis of routeCode and supplier and active status.
     *
     * @param beatCode    routeCode
     * @param distributor supplier
     * @return genericEntity record based on filtered keys.
     * in enrich two routeTypes are valid i.e 4 and 11
     */

    /**
     * Adds loginId(MGR) to pjpRecord based on beatCode and Supplier from RouteMaster data.
     *
     * @param deliveryPJP pjpRecord to be enriched.
     */
    private void updateLoginIdActiveStatus(DeliveryPjp deliveryPJP) {
        String beatCode = deliveryPJP.getBeat();
        String distributor = deliveryPJP.getExtendedAttributes().get(TENANT_CODE).asText();

        GenericEntity genericEntity = findRecordByBeatCodeAndDistributor(deliveryPJP.getBeat(), deliveryPJP.getSupplierid(),deliveryPJP.getSource());

        String loginId = genericEntity.getLoginId();

        if (ObjectUtils.isEmpty(loginId)) {
            throw new IllegalArgumentException("No mgr information associated with journeyPlanCode : " + beatCode);
        }
        User mgrUser = userService.findByLoginId(loginId);
        if (!(mgrUser != null && mgrUser.getActiveStatus() != null && mgrUser.getActiveStatus() == ActiveStatus.ACTIVE)) {
            throw new IllegalArgumentException("Inactive mgr with loginId : " + loginId + "and supplier : " + distributor);
        }

        deliveryPJP.setLoginid(loginId);
    }
}