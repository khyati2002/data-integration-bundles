package com.applicate.alsafi.enrichment;

import com.applicate.services.channelkart.services.CategoryInfoService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.CategoryInfo;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class UserEnrichment extends AbstractEnrichment<User> {

    @Override
    public OperationResult.StepResult apply(User cdm) {

        CategoryInfoService categoryInfoService = (CategoryInfoService) ServiceLocator.lookup(CategoryInfo.class);


        String currentLoginId = cdm.getLoginid();

        if (StringUtils.isNotBlank(currentLoginId)) {

            List<CategoryInfo> categoryInfo = categoryInfoService.findByCategoryCodeAndFeature(currentLoginId, "loginIdSwap");

            if (categoryInfo != null && !categoryInfo.isEmpty() && StringUtils.isNotBlank(categoryInfo.get(0).getCategoryValue())) {

                cdm.setExtendedAttributes(
                        JSONUtils.getObjectMapper().createObjectNode().put("employeeId", currentLoginId));
                cdm.setLoginid(categoryInfo.get(0).getCategoryValue());
            }
        }

        return new OperationResult.StepResult(OperationResult.Status.OK, "No changes applied in the table entry");
    }
}