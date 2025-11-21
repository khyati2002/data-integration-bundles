package com.applicate.cokeph.transformer;

import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.services.OutletMetadataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.impl.OutletDetails;
import com.salescode.dim.jooq.generated.tables.pojos.OutletMetadata;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class CokephOutletDetailsTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    static final String OUTLET_STATUS = "outlet_status";

    static final String OUTLET_DIVISION = "category_code_1" ;

    static  final String OUTLET_CATEGORY = "category_code_2" ;

    static final String OUTLET_TYPE = "category_code_4" ;

    static final String CHANNEL_CODE = "category_code_8" ;

    static final String OUTLET_CLASS = "category_code_9" ;

    static final String SUB_CHANNEL = "category_code_6" ;

    static final String MARKET_ID = "category_code_5" ;

    private static final String MOBILE_STRING="mobile";

    private static final String IMMEDIATE_PARENT_STRING="immediateParent";

    private OutletDetailsService outletDetailsService;
    private OutletMetadataService outletMetadataService;

    private static final String OUTLET_CODE_STRING="outlet_code";
    

    @Override
    public Map<String, Object> transform(Map<String, Object> stringObjectMap) {

        UserService userService = (UserService) ServiceLocator.lookup(com.salescode.dim.jooq.impl.User.class);
        outletDetailsService= (OutletDetailsService) ServiceLocator.lookup(com.salescode.dim.jooq.impl.OutletDetails.class);
        outletMetadataService= (OutletMetadataService) ServiceLocator.lookup(OutletMetadata.class);

        ObjectNode extended = new ObjectMapper().createObjectNode();
        HashMap<String, Object> finalTransformedObj = new HashMap<>();

        List<Map<String, Object>> distributorOutletMappings = (List<Map<String, Object>>) stringObjectMap.get("distributor_outlet_mapping");
        Map<String, Object> firstMapping = distributorOutletMappings.get(0);

        String outletCode;
        String contactNo;

        if (NullUtils.isNull(stringObjectMap.get(OUTLET_CODE_STRING)) || ObjectUtils.isEmpty(stringObjectMap.get(OUTLET_CODE_STRING))) {
            throw new DataTransformationService.TransformationException("Outletcode cannot be null");
        } else{
            outletCode=stringObjectMap.get(OUTLET_CODE_STRING).toString();
            finalTransformedObj.put("outletCode", outletCode);
        }
        if(NullUtils.isNull(stringObjectMap.get(MOBILE_STRING)) || ObjectUtils.isEmpty(stringObjectMap.get(MOBILE_STRING)))
        {
            throw new DataTransformationService.TransformationException("Mobile is null or empty");
        } else{
            contactNo=validateMobile(stringObjectMap);
            finalTransformedObj.put("contactno", contactNo);
        }
        Optional<List<com.salescode.dim.jooq.generated.tables.pojos.User>> contactOutletDetailsOptional=userService.findByMobileSafelyLimit(contactNo,0,2);
        List<com.salescode.dim.jooq.generated.tables.pojos.User> contactOutletDetails= contactOutletDetailsOptional.orElseGet(ArrayList::new);
        if(contactOutletDetails.size()>1) throw new DataTransformationService.TransformationException("Multiple outlets mapped with this mobileNumber");

        OutletDetails outletDetails=getIfOutletExists(outletCode,finalTransformedObj);
        if(!contactOutletDetails.isEmpty()) {
            validateUniqueContactMapped(outletDetails, contactOutletDetails, contactNo);
        }
        validateDesignation(outletDetails);
        if (outletDetails != null && outletDetails.getUserName() != null && outletDetails.getUserName().getDesignation() != null && !outletDetails.getUserName().getDesignation().isEmpty()) {
            String designation = outletDetails.getUserName().getDesignation().iterator().next();
            finalTransformedObj.put("distributionChannel", designation);
        } else {
            finalTransformedObj.put("distributionChannel", "retailer");
        }


        Object outletDivisionValue = stringObjectMap.get(OUTLET_DIVISION);
        Object cbCodeValue = stringObjectMap.get("cb_code");
        if (cbCodeValue != null && !ObjectUtils.isEmpty(cbCodeValue)) {
            extended.put("preseller", cbCodeValue.toString());
        } else if (outletDivisionValue != null && !ObjectUtils.isEmpty(outletDivisionValue)) {
            finalTransformedObj.put("outletDivision", outletDivisionValue.toString());
        } else {
            throw new DataTransformationService.TransformationException("Either cb_code or outlet_division must be present");
        }

        Object channelCodeValue = stringObjectMap.get(CHANNEL_CODE);
        finalTransformedObj.put("channel",
                channelCodeValue != null && !ObjectUtils.isEmpty(channelCodeValue) ? channelCodeValue : "NA");

        Object outletCategoryValue = stringObjectMap.get(OUTLET_CATEGORY);
        finalTransformedObj.put("outletCategory",
                outletCategoryValue != null && !ObjectUtils.isEmpty(outletCategoryValue) ? outletCategoryValue : "NA");

        Object outletTypeValue = stringObjectMap.get(OUTLET_TYPE);
        finalTransformedObj.put("outletType",
                outletTypeValue != null && !ObjectUtils.isEmpty(outletTypeValue) ? outletTypeValue : "NA");

        Object outletClassValue = stringObjectMap.get(OUTLET_CLASS);
        finalTransformedObj.put("outletClass",
                outletClassValue != null && !ObjectUtils.isEmpty(outletClassValue) ? outletClassValue : "NA");

        Object subChannelValue = stringObjectMap.get(SUB_CHANNEL);
        finalTransformedObj.put("subChannel",
                subChannelValue != null && !ObjectUtils.isEmpty(subChannelValue) ? subChannelValue : "NA");

        Object marketIdValue = stringObjectMap.get(MARKET_ID);
        finalTransformedObj.put("marketId",
                marketIdValue != null && !ObjectUtils.isEmpty(marketIdValue) ? marketIdValue : "NA");

        if (stringObjectMap.get(OUTLET_STATUS).toString().equals("1")) {
            finalTransformedObj.put("activeStatus", "active");
        } else {
            finalTransformedObj.put("activeStatus", "inactive");
        }


        Object emailValue = stringObjectMap.get("email");
        finalTransformedObj.put("email", emailValue != null && !ObjectUtils.isEmpty(emailValue) ? emailValue : "NA" );
        // adding the fields in extended attributes
        Object ownerNameValue = stringObjectMap.get("owner_name");
        Object tenantcodeValue = stringObjectMap.get("tenant_code");
        Object tradeGroupValue = stringObjectMap.get("category_code_3");
        Object businessComplexTypeValue = stringObjectMap.get("category_code_7");
        Object salesModeValue = stringObjectMap.get("sales_mode");

        Object categorycode10Value = stringObjectMap.get("category_code_10");
        extended.put("owner_name", ownerNameValue != null && !ObjectUtils.isEmpty(ownerNameValue.toString()) ? ownerNameValue.toString() : "NA");
        extended.put("tenantcode",tenantcodeValue != null && !ObjectUtils.isEmpty(tenantcodeValue.toString()) ? tenantcodeValue.toString() : "NA");
        extended.put("trade_group",tradeGroupValue != null && !ObjectUtils.isEmpty(tradeGroupValue.toString()) ? tradeGroupValue.toString() : "NA");
        extended.put("sales_mode", salesModeValue != null && !ObjectUtils.isEmpty(salesModeValue.toString()) ? salesModeValue.toString() : "NA");
        extended.put("business_complex_type",businessComplexTypeValue != null && !ObjectUtils.isEmpty(businessComplexTypeValue.toString()) ? businessComplexTypeValue.toString() : "NA");
        extended.put("categorycode10",categorycode10Value != null && !ObjectUtils.isEmpty(categorycode10Value.toString()) ? categorycode10Value.toString() : "NA");
        // outlet attributes ends here
        List<String> validDistributorCodes = Arrays.asList("0502615941", "0503558429","0502359930","0502148945","0505353136","0502148915","0504905749","0505421116","0505285873");
        String distributorCode = (String) firstMapping.get("distributor_code");

        if (NullUtils.isNull(distributorCode) || StringUtils.isEmpty(distributorCode)) {
            throw new DataTransformationService.TransformationException("distributor_code is null");
        }
        if (!validDistributorCodes.contains(distributorCode)) {
            throw new DataTransformationService.TransformationException("This distributor does not exist as a part of our onboarding plan");
        }
        extended.put("DistributorCode", distributorCode);
//        JsonNode extendedAttributes = JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class);
        JsonNode extendedAttributes = extended;
        finalTransformedObj.put("extendedAttributes", extendedAttributes);
        if (distributorOutletMappings != null && !distributorOutletMappings.isEmpty()) {
            String supplierId =(String) firstMapping.get("distributor_code") ;
            Map<Object, Object> immediateParent1 = Map.of(IMMEDIATE_PARENT_STRING, supplierId);
            //Map<Object, Object> immediateParent2 = Map.of(IMM_PARENT, salesrepid.get(0));-- salesrep to be added when received
            List<Map<Object, Object>> parentList = new ArrayList<>();
            parentList.add(immediateParent1);
            if(outletDetails!=null && outletDetails.getUserName()!=null && outletDetails.getUserName().getDesignation().contains("retailer")){
                List<User> supplierLoginId=outletDetailsService.getRetailerParent(outletDetails, "wholesaler");
                supplierLoginId.forEach(x->parentList.add(Map.of(IMMEDIATE_PARENT_STRING, x.getLoginid())));
            }
            //parentList.add(immediateParent2); -- salesrep to be added when received
            finalTransformedObj.put(IMMEDIATE_PARENT_STRING, parentList);
        }
        else{
            throw new DataTransformationService.TransformationException("No Distributor To Outlet Mapping found in the outlet body");
        }


        return finalTransformedObj;

    }
    private void validateUniqueContactMapped(OutletDetails outletDetails,List<com.salescode.dim.jooq.generated.tables.pojos.User> contactOutletDetails,String contactNo)
    {
        if((outletDetails==null || outletDetails.getContactno()==null) && !contactOutletDetails.isEmpty()){
            throw new DataTransformationService.TransformationException("Outlet mapped with mobile number already exists");
        }
        if(outletDetails!=null && outletDetails.getContactno()!=null && !outletDetails.getContactno().equalsIgnoreCase(contactNo)){
            throw new DataTransformationService.TransformationException("Outlet mapped with mobile number already exists");
        }
    }
    private OutletDetails getIfOutletExists(String outletCode,HashMap<String, Object> finalTransformedObj )
    {
        OutletDetails outletDetails=outletDetailsService.findByOutletCode(outletCode);
        if(outletDetails==null){
            Optional<String> outletCodeOpt =outletMetadataService.getOutletCodeIfExists(outletCode);
            if (outletCodeOpt.isPresent()) {
                String mappedOutletCode = outletCodeOpt.get();
                finalTransformedObj.put("outletCode", mappedOutletCode);
                return outletDetailsService.findByOutletCode(mappedOutletCode);
            }
        }
        return outletDetails;
    }
    private void validateDesignation(OutletDetails outletDetails)
    {
        if(outletDetails!=null && outletDetails.getUserName()!=null &&
                ! (outletDetails.getUserName().getDesignation().contains("retailer") ||
                        outletDetails.getUserName().getDesignation().contains("wholesaler")))
        {
            throw new DataTransformationService.TransformationException("Designation can be retailer or wholesaler only");
        }
    }

    private String validateMobile(Map<String, Object> input) {
        String mobile = input.get(MOBILE_STRING).toString();

        // Remove +63 or 63 prefix if present
        if (mobile.startsWith("+63")) {
            mobile = mobile.substring(3);
        } else if (mobile.startsWith("63")) {
            mobile = mobile.substring(2);
        }

        // Replace non-numeric characters with zeroes
        mobile = mobile.replaceAll("[^0-9]", "0");

        // Trim or pad the mobile number to ensure it's exactly 10 digits long
        if (mobile.length() > 10) {
            mobile = mobile.substring(0, 10);
        } else if (mobile.length() < 10) {
            mobile = String.format("%-10s", mobile).replace(' ', '0');
        }

        return mobile;
    }
}
