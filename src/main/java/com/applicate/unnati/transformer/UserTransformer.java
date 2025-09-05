package com.applicate.unnati.transformer;

import com.salescode.dim.etl.transformation.AbstractTransformer;
import java.util.*;

    public class UserTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

        @Override
        public Map<String, Object> transform(Map<String, Object> rawFeatureMap) {
            if (rawFeatureMap == null) return Collections.emptyMap();

            Map<String, Object> features = new LinkedHashMap<>();

            // ---- Basic Info ----
            features.put("userAccountId", rawFeatureMap.get("userAccountId"));
            features.put("loginId", rawFeatureMap.get("loginId"));
            features.put("name", rawFeatureMap.get("name"));
            features.put("email", rawFeatureMap.get("email"));
            features.put("mobile", rawFeatureMap.get("mobile"));
            features.put("alternateId", rawFeatureMap.get("alternateId"));
            features.put("registeredNumber", rawFeatureMap.get("registeredNumber"));
            features.put("contactType", rawFeatureMap.get("contactType"));
            features.put("ssoId", rawFeatureMap.get("ssoId"));
            features.put("deviceId", rawFeatureMap.get("deviceId"));

            // ---- Dates ----
            features.put("dob", rawFeatureMap.get("dob"));
            features.put("doa", rawFeatureMap.get("doa"));
            features.put("lastPasswordResetDate", rawFeatureMap.get("lastPasswordResetDate"));

            // ---- Status ----
            features.put("verified", rawFeatureMap.get("verified"));
            features.put("blocked", rawFeatureMap.get("blocked"));
            features.put("activeStatus", rawFeatureMap.get("activeStatus"));
            features.put("activeStatusReason", rawFeatureMap.get("activeStatusReason"));

            // ---- Address & Location ----
            features.put("address", rawFeatureMap.get("address"));
            features.put("locationHierarchy", rawFeatureMap.get("locationHierarchy"));
            features.put("countryCode", rawFeatureMap.get("countryCode"));
            features.put("dialCode", rawFeatureMap.get("dialCode"));
            features.put("webContext", rawFeatureMap.get("webContext"));

            // ---- Hierarchy ----
            features.put("immediateParent", rawFeatureMap.get("immediateParent"));
            features.put("hierarchy", rawFeatureMap.get("hierarchy"));
            features.put("normalizedHierarchy", rawFeatureMap.get("normalizedHierarchy"));
            features.put("assignedHierarchy", rawFeatureMap.get("assignedHierarchy"));

            // ---- Roles & Organization ----
            features.put("roles", rawFeatureMap.get("roles"));
            features.put("designation", rawFeatureMap.get("designation"));
            features.put("supplierMetaData", rawFeatureMap.get("supplierMetaData"));

            // ---- Messaging & Notifications ----
            features.put("messengerInfo", rawFeatureMap.get("messengerInfo"));
            features.put("activeNotificationChannels", rawFeatureMap.get("activeNotificationChannels"));

            // ---- Security & Passwords  ----
            features.put("lastUsedPasswords", rawFeatureMap.get("lastUsedPasswords"));
            features.put("reportPassword", rawFeatureMap.get("reportPassword"));
            features.put("password", rawFeatureMap.get("password"));

            // ---- External / Misc ----
            features.put("externalReferenceId", rawFeatureMap.get("externalReferenceId"));
            features.put("userContext", rawFeatureMap.get("userContext"));
            features.put("prodauthcode", rawFeatureMap.get("prodauthcode"));
            features.put("facebookPSID", rawFeatureMap.get("facebookPSID"));

            // ---- Additional Collections / Maps ----
            features.put("EXCLUDED_PROPERTIES", rawFeatureMap.get("EXCLUDED_PROPERTIES"));
            features.put("division", rawFeatureMap.get("division"));
            features.put("source", rawFeatureMap.get("source"));
            features.put("extendedAttributes", rawFeatureMap.get("extendedAttributes"));
            features.put("preProcessPipelineException", rawFeatureMap.get("preProcessPipelineException"));

            return features;
        }
    }
