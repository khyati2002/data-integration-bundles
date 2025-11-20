package com.applicate.cokearg.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.Map;

public class AssetsEnrichment extends AbstractEnrichment<Map<String, Object>> {

    private static final String USER_PARENT_QUERY = "select beat from ck_outlet_details where outletcode=?";

    @Override
    public OperationResult.StepResult apply(Map<String, Object> assetMap) {
        if (assetMap == null) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Asset object cannot be null");
        }

        String outletCode = (String) assetMap.get("outletCode");
        if (outletCode == null || outletCode.trim().isEmpty()) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "OutletCode cannot be null or empty");
        }

        try {
            DSLContext dsl = (DSLContext) ServiceLocator.lookup(DSLContext.class);
            
            // Using raw SQL since we don't have generated classes guaranteed
            Record result = dsl.fetchOne(USER_PARENT_QUERY, outletCode);

            if (result == null) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "No parent found for the given outletCode " + outletCode);
            }

            Object parentObj = result.get("beat");

            if (parentObj == null) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, "No parent login ID exists for the outletCode " + outletCode);
            }

            assetMap.put("loginId", parentObj.toString());

            return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully for Assets");

        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Error occurred while enriching asset data: " + e.getMessage());
        }
    }
}
