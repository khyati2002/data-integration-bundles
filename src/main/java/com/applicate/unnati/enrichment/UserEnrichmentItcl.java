package com.applicate.unnati.enrichment;

import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.salescode.dim.jooq.impl.User;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;

public class UserEnrichmentItcl extends AbstractEnrichment<User> {

    public OperationResult.StepResult apply(User cdm) {

        if (!cdm.getDesignation().contains("supplier")) {
            return OperationResult.StepResult.OK;
        }

        List<SupplierMetadata> supplierMetaDataList = cdm.getSupplierMetaData();
        if (supplierMetaDataList == null || supplierMetaDataList.isEmpty()) {
            return OperationResult.StepResult.OK;
        }

        SupplierMetadata metaData = supplierMetaDataList.get(0);
        try {
            metaData.setExtendedAttributes(updateExtendedAttributes(metaData.getExtendedAttributes()));
        } catch (IOException e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage());
        }

        if (metaData.getMin() == null) {
            metaData.setMin(0);
        }

        return OperationResult.StepResult.OK;
    }

    private ObjectNode updateExtendedAttributes(JsonNode currentAttr) throws IOException {
        ObjectNode defaultAttr = JSONUtils.getObjectMapper().createObjectNode();
        defaultAttr.put("orderFunction", "Y");
        defaultAttr.put("orderFulfillmentTime", "2");

        if (currentAttr == null) {
            return defaultAttr;
        }

        ObjectNode updates = JSONUtils.getObjectMapper().createObjectNode();
        if (!currentAttr.has("orderFunction") ||
                (currentAttr.has("orderFunction") && "NULL".equalsIgnoreCase(currentAttr.get("orderFunction").asText()))) {
            updates.put("orderFunction", "Y");
        }
        if (!currentAttr.has("orderFulfillmentTime") ||
                (currentAttr.has("orderFulfillmentTime") && "NULL".equalsIgnoreCase(currentAttr.get("orderFulfillmentTime").asText()))) {
            updates.put("orderFulfillmentTime", "2");
        }

        return (ObjectNode) JSONUtils.mergeJsonNodes(updates, currentAttr);
    }

}