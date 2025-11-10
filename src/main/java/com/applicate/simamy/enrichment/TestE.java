package com.applicate.simamy.enrichment;

import com.applicate.services.channelkart.enrichments.AbstractEnrichment;
import com.applicate.services.channelkart.enrichments.EnrichmentResult;
import com.applicate.services.channelkart.models.User;

public class TestE extends AbstractEnrichment<User> {

  @Override
  public EnrichmentResult apply(User user) {
    return null;
  }
}
