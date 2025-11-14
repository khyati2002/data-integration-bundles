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
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;

public class CokephWholesalerTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

    public OutletDetailsService outletDetailsService ;
    public OutletMetadataService outletMetadataService;
    public UserService userService ;
    private static final String CONTACTNO_STRING="contactno";
    private static final String OUTLET_CODE_STRING="outletCode";
    private static final String WHOLESALER_STRING="wholesaler";
    private static final String REGION="region";
    private static final String OUTLET_NAME_STRING="outletName";
    private static final String LONGITUDE_STRING="longitude";
    private static final String LATITUDE_STRING="latitude";
    private static final String ACTIVE_STATUS_STRING="activeStatus";
    private static final String IMMEDIATE_PARENT_STRING="immediateParent";
    private static final String DESIGNATION_STRING="designation";

    @Override
    public Map<String, Object> transform(Map<String, Object> input) {
        outletDetailsService = (OutletDetailsService) ServiceLocator.lookup(OutletDetails.class);
        outletMetadataService = (OutletMetadataService) ServiceLocator.lookup(OutletMetadata.class);
        userService = (UserService) ServiceLocator.lookup(com.salescode.dim.jooq.impl.User.class);
        ObjectNode extended = new ObjectMapper().createObjectNode();
        Map<String, Object> transformed = new HashMap<>();
        String mobile= extractRequiredAndPut(input,transformed,CONTACTNO_STRING);
        String outletCode=extractRequiredAndPut(input,transformed,OUTLET_CODE_STRING);

        OutletDetails outletDetails= outletDetailsService.findByOutletCode(outletCode);
        Optional<List<com.salescode.dim.jooq.generated.tables.pojos.User>> contactOutletDetailsOptional=userService.findByMobileSafelyLimit(mobile,0,2);
        List<com.salescode.dim.jooq.generated.tables.pojos.User> mobileOutletDetails= contactOutletDetailsOptional.orElseGet(ArrayList::new);
        if(mobileOutletDetails.size()>1) throw new DataTransformationService.TransformationException("Multiple outlets mapped with this mobileNumber");


        outletDetails=resolveOutletFromMetadataIfNeeded(outletDetails,outletCode,transformed);
        mofifyParentsIfNeeded(outletDetails,transformed);
        if(!mobileOutletDetails.isEmpty()) {
            validateUniqueMobile(mobileOutletDetails, outletDetails,mobile);
        }
        validateDesignation(outletDetails);

        extractRequiredAndPut(input,transformed,OUTLET_NAME_STRING);
        extractRequiredAndPut(input,transformed,"address");
        extractRequiredAndPut(input,transformed,"channel");
        extractRequiredAndPut(input,transformed,"subChannel");
        validateAndAddGeoCoordinates(input);
        extractRequiredAndPut(input,transformed,LATITUDE_STRING);
        extractRequiredAndPut(input,transformed,LONGITUDE_STRING);
        setDefaultValue("NA","outletClass",transformed);
        setDefaultValue("NA","outletCategory",transformed);
        setDefaultValue("NA","outletDivision",transformed);
        setDefaultValue("NA","outletType",transformed);
        setDefaultValue("NA","marketId",transformed);

        setActiveStatus(input,transformed);
        transformed.put("location", createLocation(input));
        createExtended(extended,input);
        transformed.put("distributionChannel", WHOLESALER_STRING);
        transformed.put("extendedAttributes", JSONUtils.getObjectMapper().convertValue(extended, JsonNode.class));

        createUser(transformed);
        return transformed;

    }
    private void setDefaultValue(String defaultValue,String property,Map<String, Object> target){
        target.put(property,defaultValue);
    }
    private void mofifyParentsIfNeeded(OutletDetails outletDetails,Map<String,Object> transformed)
    {
        if(outletDetails!=null){
            List<Map<Object, Object>> parentList = new ArrayList<>();
            List<com.salescode.dim.jooq.impl.User> supplierLoginId=outletDetailsService.getRetailerParent(outletDetails, "supplier");
            supplierLoginId.forEach(x->parentList.add(Map.of(IMMEDIATE_PARENT_STRING, x.getLoginid())));

            if (parentList.isEmpty()) {
                parentList.add(Map.of(IMMEDIATE_PARENT_STRING, "admin@applicate.in"));
            }
            transformed.put(IMMEDIATE_PARENT_STRING,parentList);
        }

    }

    private void validateDesignation(OutletDetails outletDetails)
    {
        if(outletDetails!=null &&
                outletDetails.getUserName()!=null
                && outletDetails.getUserName().getDesignation()!=null){
            Set<String> designationSet=outletDetails.getUserName().getDesignation();
            if(!(designationSet.contains("retailer") || designationSet.contains(WHOLESALER_STRING))){
                throw new DataTransformationService.TransformationException("User is neither a retailer not a wholesaler");
            }

        }
    }
    private void validateUniqueMobile(List<com.salescode.dim.jooq.generated.tables.pojos.User> mobileOutletDetails,OutletDetails outletDetails,String contactNo)
    {
        if((outletDetails==null || outletDetails.getContactno()==null) && !mobileOutletDetails.isEmpty()){
            throw new DataTransformationService.TransformationException("Outlet mapped with mobile number already exists");
        }
        if(outletDetails!=null && outletDetails.getContactno()!=null && !outletDetails.getContactno().equalsIgnoreCase(contactNo)){
            throw new DataTransformationService.TransformationException("Outlet mapped with mobile number already exists");
        }
    }
    private OutletDetails resolveOutletFromMetadataIfNeeded(OutletDetails outletDetails, String outletCode, Map<String, Object> target) {
        if (outletDetails == null) {
            Optional<String> outletCodeOpt = outletMetadataService.getOutletCodeIfExists(outletCode);
            if (outletCodeOpt.isPresent()) {
                String mappedOutletCode = outletCodeOpt.get();
                target.put(OUTLET_CODE_STRING, mappedOutletCode);
                return outletDetailsService.findByOutletCode(mappedOutletCode);
            }
        }
        return outletDetails;
    }
    private void createExtended(ObjectNode extended,Map<String,Object> stringObjectMap)
    {
        String servingRadiusInMeters=getIfPropertyExists(stringObjectMap,"servingRadius");
        setServingRadius(servingRadiusInMeters,extended);
        extended.put("owner_name",getIfPropertyExists(stringObjectMap,"ownerName"));
        extended.put("town",getIfPropertyExists(stringObjectMap,"town"));
        extended.put(REGION,getIfPropertyExists(stringObjectMap,REGION));
        extended.put(DESIGNATION_STRING,WHOLESALER_STRING);
    }

    private void createUser(Map<String,Object> finalTransformedObject)
    {
        Map<String,Object> userMap=new HashMap<>();
        userMap.put(ACTIVE_STATUS_STRING,finalTransformedObject.get(ACTIVE_STATUS_STRING).toString());
        userMap.put("loginId",finalTransformedObject.get(OUTLET_CODE_STRING).toString());
        userMap.put("userAccountId",finalTransformedObject.get(OUTLET_CODE_STRING).toString());
        userMap.put("locationHierarchy",finalTransformedObject.get("location"));
        userMap.put("mobile",finalTransformedObject.get(CONTACTNO_STRING).toString());
        userMap.put("name",finalTransformedObject.get(OUTLET_NAME_STRING).toString());
        userMap.put(DESIGNATION_STRING,WHOLESALER_STRING);
        finalTransformedObject.put("userName",userMap);
    }

    private void setServingRadius(String servingRadius,ObjectNode objectNode)
    {
        try{
            long servingRadiusInMeters=Long.parseLong(servingRadius)*1000;
            if(servingRadiusInMeters<0){
                throw new DataTransformationService.TransformationException("Invalid value of Serving Radius");
            }
            objectNode.put("servingRadiusInMeters",servingRadiusInMeters+"");
        }
        catch (Exception ex)
        {
            throw new DataTransformationService.TransformationException("Invalid value of Serving Radius");
        }
    }
    private Map<String,Object> createLocation(Map<String,Object> stringObjectMap)
    {
        String state=getIfPropertyExists(stringObjectMap,"state");
        String city=getIfPropertyExists(stringObjectMap,"city");
        String town=getIfPropertyExists(stringObjectMap,"town");
        String zip=getIfPropertyExists(stringObjectMap,"pincode");
        String region=getIfPropertyExists(stringObjectMap,REGION);
        return createLocationMap(state,city,town,zip,region);


    }
    private Map<String, Object> createLocationMap(String state, String city, String town,String zip,String region) {
        Map<String, Object> location = new HashMap<>();
        location.put("state", state);
        location.put("country", "Philippines");
        location.put("city",city);
        location.put("pincode", zip);
        location.put("town",town);
        location.put(REGION,region);
        return location;
    }
    private void setActiveStatus(Map<String,Object> input,Map<String,Object> target)
    {
        String status = String.valueOf(input.get(ACTIVE_STATUS_STRING));
        if (status.equalsIgnoreCase("1") || status.equalsIgnoreCase("active")) {
            target.put(ACTIVE_STATUS_STRING, "active");
        } else if (status.equalsIgnoreCase("0") || status.equalsIgnoreCase("inactive")) {
            target.put(ACTIVE_STATUS_STRING, "inactive");
        } else {
            throw new DataTransformationService.TransformationException("Invalid value for Status");
        }

    }
    private void validateAndAddGeoCoordinates(Map<String, Object> stringObjectMap) {
        String latStr=getIfPropertyExists(stringObjectMap,LATITUDE_STRING);
        String lonStr=getIfPropertyExists(stringObjectMap,LONGITUDE_STRING);

        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);

            if (lat < -90 || lat > 90) {
                throw new DataTransformationService.TransformationException("Latitude is out of valid range (-90 to 90)");
            }

            if (lon < -180 || lon > 180) {
                throw new DataTransformationService.TransformationException("Longitude is out of valid range (-180 to 180)");
            }
        } catch (NumberFormatException e) {
            throw new DataTransformationService.TransformationException("Latitude or Longitude is not a valid number");
        }
    }
    private String extractRequiredAndPut(Map<String, Object> input, Map<String, Object> target, String key) {
        String value = getIfPropertyExists(input, key);
        target.put(key, value);
        return value;
    }
    private String getIfPropertyExists(Map<String,Object> stringObjectMap,String propertyName)
    {
        if(NullUtils.isNull(stringObjectMap.get(propertyName)) || ObjectUtils.isEmpty(stringObjectMap.get(propertyName))){
            throw  new DataTransformationService.TransformationException(propertyName+" cannot be null or empty");
        }
        return stringObjectMap.get(propertyName).toString();
    }
}
