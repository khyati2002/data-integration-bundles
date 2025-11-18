package com.applicate.kgbpl.enrichment;

import com.applicate.services.channelkart.services.CategoryInfoService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.*;

import java.util.List;
import java.util.Set;

public class CustomerEnrichmentKgbpl extends AbstractEnrichment<OutletDetails> {

    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {

        // Ensure Location is not null
        if (cdm.getLocation() == null) {
            cdm.setLocation(new Location());
        }

        // Ensure User is not null for Distributor creation
        if (cdm.getUserName() == null) {
            cdm.setUserName(new User());
        }

        CategoryInfoService categoryInfoService =
                (CategoryInfoService) ServiceLocator.lookup(CategoryInfo.class);

        UserService userService = (UserService) ServiceLocator.lookup(User.class);

        // ---------------------------
        // Area enrichment
        // ---------------------------
        List<CategoryInfo> areaList = categoryInfoService.findByCategoryCodeAndFeature(
                cdm.getLocation().getArea(), "area"
        );
        if (!areaList.isEmpty()) {
            String areaValue = areaList.get(0).getCategoryValue();
            if (!StringUtils.isEmpty(areaValue)) {
                cdm.getLocation().setArea(areaValue);
            }
        }

        // Customer Category enrichment
        List<CategoryInfo> customerCategoryList = categoryInfoService.findByCategoryCodeAndFeature(
                cdm.getOutletCategory(), "customercategory"
        );
        if (!customerCategoryList.isEmpty()) {
            String categoryValue = customerCategoryList.get(0).getCategoryValue();
            if (!StringUtils.isEmpty(categoryValue)) {
                cdm.setOutletCategory(categoryValue);
            }
        }

        // VPO enrichment
        List<CategoryInfo> vpoList = categoryInfoService.findByCategoryCodeAndFeature(
                cdm.getOutletClass(), "VPO"
        );
        if (!vpoList.isEmpty()) {
            String vpoValue = vpoList.get(0).getCategoryValue();
            if (!StringUtils.isEmpty(vpoValue)) {
                cdm.setOutletClass(vpoValue);
            }
        }

        // SubChannel enrichment
        List<CategoryInfo> subChannelList = categoryInfoService.findByCategoryCodeAndFeature(
                cdm.getSubChannel(), "SubChannel"
        );
        if (!subChannelList.isEmpty()) {
            String subChannelValue = subChannelList.get(0).getCategoryValue();
            if (!StringUtils.isEmpty(subChannelValue)) {
                cdm.setSubChannel(subChannelValue);
            }
        }

        // Segment enrichment
        List<CategoryInfo> segmentList = categoryInfoService.findByCategoryCodeAndFeature(
                cdm.getSegment(), "businessSegment"
        );
        if (!segmentList.isEmpty()) {
            String segmentValue = segmentList.get(0).getCategoryValue();
            if (!StringUtils.isEmpty(segmentValue)) {
                cdm.setSegment(segmentValue);
            }
        }

        // ---------------------------
        // User creation for Distributor
        // ---------------------------
        if ("Distributor".equalsIgnoreCase(cdm.getOutletType())) {
            User user = cdm.getUserName();
            if (user == null) {
                user = new User();
            }
            user.setLoginId(cdm.getOutletcode());
            user.setSource(cdm.getSource());
            user.setUserAccountId(cdm.getOutletcode());
            user.setActiveStatus(cdm.getActiveStatus());
            user.setLocationHierarchy(cdm.getLocationHierarchy());
            user.setMobile(cdm.getContactno());
            user.setAddress(cdm.getAddress());
            user.setName(StringUtils.isEmpty(cdm.getOutletName()) ? cdm.getOutletcode() : cdm.getOutletName());
            user.setDesignation(Set.of("supplier"));
            cdm.setUserName(user);
        } else {
            User user = cdm.getUserName();
            if (user != null) {
                user.setSource(cdm.getSource());
                cdm.setUserName(user);
            }
        }

        // SubSegment enrichment
        List<CategoryInfo> subSegmentList = categoryInfoService.findByCategoryCodeAndFeature(
                cdm.getOutletDivision(), "subsegmentEntity"
        );
        if (!subSegmentList.isEmpty()) {
            String subSegmentValue = subSegmentList.get(0).getCategoryValue();
            if (!StringUtils.isEmpty(subSegmentValue)) {
                cdm.setOutletDivision(subSegmentValue);
            }
        }

        // Channel enrichment
        List<CategoryInfo> channelList = categoryInfoService.findByCategoryCodeAndFeature(
                cdm.getChannel(), "channel"
        );
        if (!channelList.isEmpty()) {
            String channelValue = channelList.get(0).getCategoryValue();
            if (!StringUtils.isEmpty(channelValue)) {
                cdm.setChannel(channelValue);
            }
        }

        return new OperationResult.StepResult(OperationResult.Status.OK, "Customer enriched successfully");
    }
}