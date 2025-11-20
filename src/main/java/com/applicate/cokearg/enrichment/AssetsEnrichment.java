package com.applicate.cokearg.enrichment;

import com.applicate.services.channelkart.services.AbstractCDMService;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.OutletDetails;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.DSLContext;
import org.jooq.Record;

import static com.salescode.dim.jooq.generated.tables.CkOutletDetails.CK_OUTLET_DETAILS;

public class AssetsEnrichment extends AbstractEnrichment<OutletDetails> {

    @Override
    public OperationResult.StepResult apply(OutletDetails outletDetails) {
        if (outletDetails == null) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Asset object cannot be null");
        }

        String outletCode = outletDetails.getOutletcode();
        if (outletCode == null || outletCode.trim().isEmpty()) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "OutletCode cannot be null or empty");
        }

        try {
            DSLContext dsl = AbstractCDMService.getDslContext();
            
            Record result = dsl.select(CK_OUTLET_DETAILS.BEAT)
                    .from(CK_OUTLET_DETAILS)
                    .where(CK_OUTLET_DETAILS.OUTLETCODE.eq(outletCode))
                    .fetchOne();

            if (result == null) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "No parent found for the given outletCode " + outletCode);
            }

            String parentObj = result.get(CK_OUTLET_DETAILS.BEAT);

            if (parentObj == null) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "No parent login ID exists for the outletCode " + outletCode);
            }

            JsonNode extendedAttributes = outletDetails.getExtendedAttributes();
            if (extendedAttributes == null) {
                extendedAttributes = JSONUtils.getObjectMapper().createObjectNode();
                outletDetails.setExtendedAttributes(extendedAttributes);
            }

            if (extendedAttributes instanceof ObjectNode) {
                ((ObjectNode) extendedAttributes).put("loginId", parentObj);
            }

            return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully for Assets");

        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Error occurred while enriching asset data: " + e.getMessage());
        }
    }
}
