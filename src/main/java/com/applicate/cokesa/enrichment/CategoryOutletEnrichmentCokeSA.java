package com.applicate.cokesa.enrichment;

import com.applicate.services.channelkart.services.CategoryInfoService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.NullUtils;
import com.applicate.services.channelkart.utils.SecurityContextUtils;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.CategoryInfo;

import java.util.*;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CategoryOutletEnrichmentCokeSA extends AbstractEnrichment<OutletDetails> {

    @Override
    public OperationResult.StepResult apply(OutletDetails outlet) {
        String authToken = SecurityContextUtils.getPrincipal();
        if ("integration_user".equals(authToken) && !outlet.getExtendedAttributes().get("designation").toString().contains("Depot")) {
            CategoryInfoService categoryInfoService = (CategoryInfoService) ServiceLocator.lookup(CategoryInfo.class);
            try {
                List<String> distributionChannel = splitByHyphen(outlet.getDistributionChannel());
                List<CategoryInfo> disChannel = categoryInfoService.findByCategoryCodeAndFeature(distributionChannel.get(0), distributionChannel.get(1));
                if (NullUtils.isNotNull(disChannel))
                    outlet.setDistributionChannel(disChannel.get(0).getCategoryValue());

                List<String> outletClass = splitByHyphen(outlet.getOutletClass());
                List<CategoryInfo> oClass = categoryInfoService.findByCategoryCodeAndFeature(outletClass.get(0), outletClass.get(1));
                if (NullUtils.isNotNull(oClass)) outlet.setOutletClass(oClass.get(0).getCategoryValue());

                List<String> outletAttr2 = splitByHyphen(outlet.getOutletAttr2());
                List<CategoryInfo> oAttr2 = categoryInfoService.findByCategoryCodeAndFeature(outletAttr2.get(0), outletAttr2.get(1));
                if (NullUtils.isNotNull(oAttr2)) outlet.setOutletAttr2(oAttr2.get(0).getCategoryValue());

                List<String> channel = splitByHyphen(outlet.getChannel());
                List<CategoryInfo> channelCat = categoryInfoService.findByCategoryCodeAndFeature(channel.get(0), channel.get(1));
                if (NullUtils.isNotNull(channelCat)) outlet.setChannel(disChannel.get(0).getCategoryValue());
                return new OperationResult.StepResult(OperationResult.Status.OK, "Outlet Category Enriched Successfully");
            } catch (Exception e) {
                return new OperationResult.StepResult(OperationResult.Status.ERROR, e.getMessage() + ": Missing Category");
            }
        }
        else return OperationResult.StepResult.OK;
    }

    private List<String> splitByHyphen(String input) {
        List<String> result = new ArrayList<>(2);
        if (NullUtils.isNull(input)) {
            result.add("");
            result.add("");
            return result;
        }
        String[] parts = input.split("-", 2);
        result.add(parts[0]);
        result.add(parts.length > 1 ? parts[1] : "");
        return result;
    }
}