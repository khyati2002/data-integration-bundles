package com.applicate.kgbpl.enrichment;

import com.applicate.services.channelkart.services.CategoryInfoService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.CategoryInfo;
import com.salescode.dim.jooq.impl.Location;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.impl.User;

import java.util.List;
import java.util.Set;

public class CustomerEnrichmentKgbpl extends AbstractEnrichment<OutletDetails> {

    @Override
    public OperationResult.StepResult apply(OutletDetails cdm) {

        CategoryInfoService categoryInfoService =
                (CategoryInfoService) ServiceLocator.lookup(CategoryInfo.class);

        UserService userService = (UserService) ServiceLocator.lookup(User.class);

        // ---------------------------
        // Area enrichment
        // ---------------------------
        Location location = cdm.getLocation();
        if (location != null && location.getArea() != null) {
            List<CategoryInfo> areaList = categoryInfoService.findByCategoryCodeAndFeature(location.getArea(), "area");
            if (!areaList.isEmpty() && !StringUtils.isEmpty(areaList.get(0).getCategoryValue())) {
                location.setArea(areaList.get(0).getCategoryValue());
            }
        }

        // ---------------------------
        // Customer Category enrichment
        // ---------------------------
        if (cdm.getOutletCategory() != null) {
            List<CategoryInfo> customerCategoryList = categoryInfoService.findByCategoryCodeAndFeature(
                    cdm.getOutletCategory(), "customercategory"
            );
            if (!customerCategoryList.isEmpty() && !StringUtils.isEmpty(customerCategoryList.get(0).getCategoryValue())) {
                cdm.setOutletCategory(customerCategoryList.get(0).getCategoryValue());
            }
        }

        // ---------------------------
        // VPO enrichment
        // ---------------------------
        if (cdm.getOutletClass() != null) {
            List<CategoryInfo> vpoList = categoryInfoService.findByCategoryCodeAndFeature(
                    cdm.getOutletClass(), "VPO"
            );
            if (!vpoList.isEmpty() && !StringUtils.isEmpty(vpoList.get(0).getCategoryValue())) {
                cdm.setOutletClass(vpoList.get(0).getCategoryValue());
            }
        }

        // ---------------------------
        // SubChannel enrichment
        // ---------------------------
        if (cdm.getSubChannel() != null) {
            List<CategoryInfo> subChannelList = categoryInfoService.findByCategoryCodeAndFeature(
                    cdm.getSubChannel(), "SubChannel"
            );
            if (!subChannelList.isEmpty() && !StringUtils.isEmpty(subChannelList.get(0).getCategoryValue())) {
                cdm.setSubChannel(subChannelList.get(0).getCategoryValue());
            }
        }

        // ---------------------------
        // Segment enrichment
        // ---------------------------
        if (cdm.getSegment() != null) {
            List<CategoryInfo> segmentList = categoryInfoService.findByCategoryCodeAndFeature(
                    cdm.getSegment(), "businessSegment"
            );
            if (!segmentList.isEmpty() && !StringUtils.isEmpty(segmentList.get(0).getCategoryValue())) {
                cdm.setSegment(segmentList.get(0).getCategoryValue());
            }
        }

        // ---------------------------
        // User creation for Distributor
        // ---------------------------
        User user;
        if ("Distributor".equalsIgnoreCase(cdm.getOutletType())) {
            user = new User();
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
            user = cdm.getUserName();
            if (user == null) {
                user = new User();
            }
            user.setSource(cdm.getSource());
            cdm.setUserName(user);
        }

        // ---------------------------
        // SubSegment enrichment
        // ---------------------------
        if (cdm.getOutletDivision() != null) {
            List<CategoryInfo> subSegmentList = categoryInfoService.findByCategoryCodeAndFeature(
                    cdm.getOutletDivision(), "subsegmentEntity"
            );
            if (!subSegmentList.isEmpty() && !StringUtils.isEmpty(subSegmentList.get(0).getCategoryValue())) {
                cdm.setOutletDivision(subSegmentList.get(0).getCategoryValue());
            }
        }

        // ---------------------------
        // Channel enrichment
        // ---------------------------
        if (cdm.getChannel() != null) {
            List<CategoryInfo> channelList = categoryInfoService.findByCategoryCodeAndFeature(
                    cdm.getChannel(), "channel"
            );
            if (!channelList.isEmpty() && !StringUtils.isEmpty(channelList.get(0).getCategoryValue())) {
                cdm.setChannel(channelList.get(0).getCategoryValue());
            }
        }
// ---------------------------
        // AccountInfo enrichment block (commented)
        // ---------------------------
//        AccountInfoService accountInfoService =
//                (AccountInfoService) ServiceLocator.lookup(AccountInfo.class);
//
//        String loginId = cdm.getOutletcode();
//        AccountInfo existingAccount = accountInfoService.findByLoginId(loginId);
//
//        AccountInfo decrypted;
//        boolean isUpdated = false;
//        boolean isNewAccount = false;
//
//        if (existingAccount != null) {
//            decrypted = accountInfoService.decrypt(existingAccount);
//        } else {
//            decrypted = new AccountInfo();
//            decrypted.setLoginId(loginId);
//            isNewAccount = true;
//        }
//
//        if (!StringUtils.isEmpty(cdm.getGstNo())) {
//            decrypted.setGstin(cdm.getGstNo());
//            isUpdated = true;
//        }
//
//        if (!StringUtils.isEmpty(cdm.getOutletAttr4())) {
//            decrypted.setPan(cdm.getOutletAttr4());
//            isUpdated = true;
//        }
//
//        if (isNewAccount || isUpdated) {
//            accountInfoService.save(decrypted);
//        }

        return new OperationResult.StepResult(OperationResult.Status.OK, "Customer enriched successfully");
    }
}