//package com.applicate.simamy.enrichment;
//
//import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
//import com.applicate.services.channelkart.enrichments.EnrichmentResult;
//import com.applicate.services.channelkart.enrichments.Status;
//import com.applicate.services.channelkart.exceptions.ExecutionInteruptedException;
//import com.applicate.services.channelkart.models.MetaData;
//import com.applicate.services.channelkart.models.User;
//import com.applicate.services.channelkart.services.MetaDataService;
//import com.applicate.services.channelkart.services.SpringContext;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
///**
// * This checks if the Designation is retailer then gets the domainValue from ck_metadata and sets the encoded password as password;
// */
//public class UserPasswordEnrichment extends AbstractEnrichment<User> {
//
//    private final MetaDataService metadataservice = SpringContext.getBean(MetaDataService.class);
//    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
//    private static final String CUSTOM_PASSWORD_DOMAIN_NAME = "password";
//
//    private static final String RETAILER = "retailer";
//
//    @Override
//    public EnrichmentResult apply(User cdm) {
//
//        if (cdm.getDesignation().stream().anyMatch(r -> r.equalsIgnoreCase(RETAILER))) {
//            try {
//                MetaData metadata = metadataservice.fetchByValue(CUSTOM_PASSWORD_DOMAIN_NAME, RETAILER);
//                if (metadata == null) {
//                    return new EnrichmentResult(Status.ERROR, "Metadata configuration not present for retailer password.");
//                }
//                ArrayNode arraynode = metadata.getDomainValues();
//                if (arraynode == null || arraynode.size() == 0) {
//                    return new EnrichmentResult(Status.ERROR, "System has found metadata resource for retailer password but seems misconfigured. Please check configuration.");
//                }
//                JsonNode node = arraynode.get(0);
//                if (!node.has("default")) {
//                    return new EnrichmentResult(Status.ERROR, "System has found metadata resource for retailer password but 'default' key not found. Please check configuration.");
//                }
//                String rawPassword = node.get("default").textValue();
//                cdm.setPassword(encoder.encode(rawPassword));
//                return EnrichmentResult.OK;
//            } catch (Exception ex) {
//                throw new ExecutionInteruptedException(ex, "Some error occurred while setting retailer password");
//            }
//        }
//        return new EnrichmentResult(Status.OK, "Custom password Enrichment Skipped.");
//    }
//}
