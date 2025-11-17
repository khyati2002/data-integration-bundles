package com.applicate.kgbpl.enrichment;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.User;

public class UserEnrichmentKgpl extends AbstractEnrichment<User> {

    @Override
    public OperationResult.StepResult apply(User cdm) {

        CommonDataModel oldModel = cdm.getOldModel();

        if (oldModel instanceof User) {
            User oldUser = (User) oldModel;

            String oldName = safeString(oldUser.getName());
            String oldLoginId = safeString(oldUser.getLoginid());
            String newName = safeString(cdm.getName());
            String newLoginId = safeString(cdm.getLoginid());

            boolean oldWasCustom = isCustomName(oldName, oldLoginId);
            boolean newIsDefault = isDefaultName(newName, newLoginId);
            boolean newIsCustom = isCustomName(newName, newLoginId);

            // --- RULE 1: Old was custom → but new is default → revert to old custom name
            if (oldWasCustom && newIsDefault) {
                cdm.setName(oldName);
            }

            // --- RULE 2: New is meaningful/custom and changed → allow update
            else if (newIsCustom && !newName.equals(oldName)) {
                cdm.setName(newName);
            }

            // --- RULE 3: otherwise do nothing
        }

        return new OperationResult.StepResult(OperationResult.Status.OK, "User data enriched successfully");
    }

    // A name is custom if it's not null, not empty, and not equal to loginId
    private boolean isCustomName(String name, String loginId) {
        return !name.isEmpty() && !name.equalsIgnoreCase(loginId);
    }

    // Default name = loginId (case-insensitive)
    private boolean isDefaultName(String name, String loginId) {
        return name.equalsIgnoreCase(loginId);
    }

    // Null-safe
    private String safeString(String value) {
        return value != null ? value.trim() : "";
    }
}