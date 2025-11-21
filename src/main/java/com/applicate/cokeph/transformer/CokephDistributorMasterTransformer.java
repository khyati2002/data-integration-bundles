package com.applicate.cokeph.transformer;

import com.applicate.services.channelkart.services.OutletMetadataService;
import com.applicate.services.channelkart.services.ServiceLocator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;

import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.NullUtils;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.OutletMetadata;
import com.salescode.dim.jooq.generated.tables.pojos.SupplierMetadata;
import com.salescode.dim.jooq.impl.User;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class CokephDistributorMasterTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    private static final ObjectMapper mapper = new ObjectMapper();
    static final String IS_ACTIVE = "is_active";

    private UserService userService;
    private OutletMetadataService outletMetadataService;

    @Override
    public Map<String, Object> transform(Map<String, Object> stringObjectMap) {
        this.userService = (UserService) ServiceLocator.lookup(User.class);
        this.outletMetadataService = (OutletMetadataService) ServiceLocator.lookup(OutletMetadata.class);

        ObjectNode extended = new ObjectMapper().createObjectNode();
        HashMap<String, Object> finalTransformedObj = new HashMap<>();
        finalTransformedObj.put("loginId", stringObjectMap.get("distributor_code").toString());
        User user=userService.findByLoginId(stringObjectMap.get("distributor_code").toString());
        if(user==null){
            Optional<String> mappedOutletCode= outletMetadataService.getOutletCodeIfExists(stringObjectMap.get("distributor_code").toString());
            if(mappedOutletCode.isPresent()){
                throw new DataTransformationService.TransformationException("User exists and it is not a supplier");
            }
        }
        validateDesignation(user);
        finalTransformedObj.put("userAccountId", stringObjectMap.get("distributor_code").toString());
        finalTransformedObj.put("designation","supplier");
        Map<String, Object> parentObj = new HashMap<>();
        parentObj.put("parent", "admin@applicate.in");

        finalTransformedObj.put("immediateParent", parentObj);

//        finalTransformedObj.put("immediateParent", "admin@applicate.in");
        //adding supplier metadata

//        ObjectNode supplierMetaData = mapper.createObjectNode();
//        ObjectNode extendedSupplier = new ObjectMapper().createObjectNode();
//        extendedSupplier.put("minOrderValidation", "N") ;
//        supplierMetaData.set("extendedAttributes", extendedSupplier);
//        finalTransformedObj.put("supplierMetaData", supplierMetaData);

        SupplierMetadata supplierMeta = new SupplierMetadata();

        org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper shadedMapper =
                new org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper();

        ObjectNode extendedSupplier = shadedMapper.createObjectNode();
        extendedSupplier.put("minOrderValidation", "N");

        supplierMeta.setExtendedAttributes(extendedSupplier);
        finalTransformedObj.put("supplierMetaData", supplierMeta);


        if(NullUtils.isNotNull(stringObjectMap.get("distributor_name"))){
            finalTransformedObj.put("name", stringObjectMap.get("distributor_name").toString()) ;
        }
        if (stringObjectMap.get(IS_ACTIVE).toString().equals("1")) {
            finalTransformedObj.put("activeStatus", "active");
        } else {
            finalTransformedObj.put("activeStatus", "inactive");
        }
        if(!(NullUtils.isNull(stringObjectMap.get("email")) || ObjectUtils.isEmpty(stringObjectMap.get("email")))) {
            finalTransformedObj.put("email",stringObjectMap.get("email")) ;
        }
        if(!(NullUtils.isNull(stringObjectMap.get("address")) || ObjectUtils.isEmpty(stringObjectMap.get("address")))) {
            finalTransformedObj.put("address", stringObjectMap.get("address"));
        }
        if(NullUtils.isNull(stringObjectMap.get("mobile")) || ObjectUtils.isEmpty(stringObjectMap.get("mobile")))
        {
            throw new DataTransformationService.TransformationException("Mobile is null or empty");
        } else{
            String mobile=validateMobile(stringObjectMap);
            finalTransformedObj.put("mobile",mobile);
            validateUniqueUserMappedWithMobile(mobile,user);
        }
        if(NullUtils.isNull(stringObjectMap.get("state"))|| NullUtils.isNull(stringObjectMap.get("zip")) || NullUtils.isNull(stringObjectMap.get("city"))){
            Map<String, Object> location = new HashMap<>();
            location.put("country", "Philippines");
            finalTransformedObj.put("locationHierarchy", location);
        }
        else{
            String state = (String) stringObjectMap.get("state");
            String zip = (String) stringObjectMap.get("zip");
            String city = (String) stringObjectMap.get("city");
            Map<String, Object> location = createLocationSlmg(state, zip, city);
            finalTransformedObj.put("locationHierarchy", location);
        }
        return finalTransformedObj ;

    }
    private void validateDesignation(User user)
    {
        if(user!=null && user.getDesignation()!=null && !user.getDesignation().contains("supplier")){
            throw new DataTransformationService.TransformationException("User exists and it is not a supplier");
        }
    }
    private void validateUniqueUserMappedWithMobile(String mobile,User user)
    {
        Optional<List<com.salescode.dim.jooq.generated.tables.pojos.User>> contactOutletDetailsOptional=userService.findByMobileSafelyLimit(mobile,0,2);
        List<com.salescode.dim.jooq.generated.tables.pojos.User> contactOutletDetails= contactOutletDetailsOptional.orElseGet(ArrayList::new);
        if(contactOutletDetails.size()>1) throw new DataTransformationService.TransformationException("Multiple outlets mapped with this mobileNumber");
        if(!contactOutletDetails.isEmpty()) {
            if((user==null || user.getMobile()==null) && !contactOutletDetails.isEmpty()){
                throw new DataTransformationService.TransformationException("Outlet mapped with mobile number already exists");
            }
            if(user!=null && user.getMobile()!=null && !user.getMobile().equalsIgnoreCase(mobile)){
                throw new DataTransformationService.TransformationException("Outlet mapped with mobile number already exists");
            }
        }

    }
    private String validateMobile(Map<String, Object> stringObjectMap) {
        final String contact = "mobile";
        String mobile = stringObjectMap.get(contact).toString();

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

    private Map<String, Object> createLocationSlmg(String state, String zip, String city) {
        Map<String, Object> location = new HashMap<>();
        location.put("state", state);
        location.put("country", "Philippines");
        location.put("pincode", zip);
        location.put("city", city);
        return location;
    }

}
