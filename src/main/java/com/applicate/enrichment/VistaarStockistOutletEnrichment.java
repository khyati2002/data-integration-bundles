package com.applicate.enrichment;

import com.applicate.services.channelkart.exceptions.EnrichmentFailException;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.HierarchyMetadata;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class VistaarStockistOutletEnrichment extends AbstractEnrichment<OutletDetails> {

    @Override
    public OperationResult.StepResult apply(OutletDetails outletDetails) {
        if (shouldSkipEnrichment(outletDetails)) {
            return new OperationResult.StepResult(OperationResult.Status.OK, "hierarchy enrichment skipped");
        }

        outletDetails.setChannel("STOCKIST");

        try {
            boolean enriched = enrichHierarchy(outletDetails.getImmediateParent());
            return new OperationResult.StepResult(OperationResult.Status.OK,
                    enriched ? "Data enriched successfully" : "Data enrichment skipped");
        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getLocalizedMessage());
        }
    }

    private boolean shouldSkipEnrichment(OutletDetails outletDetails) {
        return StringUtils.isBlank(outletDetails.getOutletType())
                || !outletDetails.getOutletType().equalsIgnoreCase("stockist");
    }

    private boolean enrichHierarchy(List<HierarchyMetadata> hierarchyList) {
        if (hierarchyList == null || hierarchyList.isEmpty()) {
            return false;
        }

        boolean enriched = false;
        for (HierarchyMetadata metadata : hierarchyList) {
            validateParentId(metadata.getImmediateParent());
            if (StringUtils.isNotBlank(metadata.getHierarchy())) {
                metadata.setHierarchy(replaceIDInHierarchy(metadata.getHierarchy()));
                enriched = true;
            }
        }
        return enriched;
    }

    private void validateParentId(String parentId) {
        if (StringUtils.isBlank(parentId)) {
            throw new EnrichmentFailException("UID cannot be empty");
        }
    }

    String replaceIDInHierarchy(String hierarchy) {
        StringBuilder sb = new StringBuilder();
        for (String segment : hierarchy.split(",")) {
            sb.append(transformHierarchySegment(segment)).append(", ");
        }
        return sb.substring(0, sb.length() - 2);
    }

    private String transformHierarchySegment(String segment) {
        StringBuilder result = new StringBuilder();
        String[] parts = segment.split(">");
        for (int i = 0; i < parts.length; i++) {
            String value = parts[i].trim();
            if (i != 0) {
                value = getValidatedLoginId(value);
            }
            result.append("> ").append(value).append(" ");
        }
        return result.substring(2).trim();
    }

    private String getValidatedLoginId(String value) {
        UserService userService = (UserService) ServiceLocator.lookup(User.class);
        User user = userService.findById(value);
        if (user != null && !user.getLoginId().equalsIgnoreCase(value)) {
            return user.getLoginId();
        }
        if (userService.findByLoginId(value) == null) {
            throw new EnrichmentFailException(
                    com.applicate.services.channelkart.utils.StringUtils.format("{} is not a valid user", value));
        }
        return value;
    }
}
