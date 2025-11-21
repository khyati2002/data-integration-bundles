package com.applicate.cokeph.transformer;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.salescode.dim.etl.transformation.AbstractTransformer;
import com.salescode.dim.etl.transformation.service.DataTransformationService;
import com.salescode.dim.jooq.generated.tables.pojos.User;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

public class CokePhGeoDistributorTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {

     public UserService userService;
    @Override
    public Map<String, Object> transform(Map<String, Object> input) {
        userService = (UserService) ServiceLocator.lookup(com.salescode.dim.jooq.impl.User.class);
//        ObjectNode extended = new ObjectMapper().createObjectNode();
        Map<String, Object> extended = new HashMap<>();
        Map<String, Object> transformed = new HashMap<>();
        String outletCode= extractRequiredAndPut(input,transformed,"outletCode");
        User user=userService.findByLoginId(outletCode);
        if(user==null || ((com.salescode.dim.jooq.impl.User) user).getDesignation()==null || !((com.salescode.dim.jooq.impl.User) user).getDesignation().contains("supplier")){
            throw new DataTransformationService.TransformationException("No such distributor exists");
        }
        transformed.put("activeStatus", "active");
        transformed.put("outletName",user.getName());
        transformed.put("contactno",user.getMobile());
        validateAndAddGeoCoordinates(input);
        extractRequiredAndPut(input,transformed,"latitude");
        extractRequiredAndPut(input,transformed,"longitude");
        createExtended(extended,input);
        transformed.put("extendedAttributes", extended);
        createUser(transformed);
        transformed.put("distributionChannel", "supplier");
        return transformed;


    }
    private void createUser(Map<String,Object> finalTransformedObject)
    {
        Map<String,Object> userMap=new HashMap<>();
        userMap.put("loginId",finalTransformedObject.get("outletCode").toString());
        userMap.put("designation", "supplier");
        finalTransformedObject.put("userName",userMap);
    }
    private void createExtended(Map<String, Object> extended ,Map<String,Object> stringObjectMap)
    {
        String servingRadiusInMeters=getIfPropertyExists(stringObjectMap,"servingRadius");
        setServingRadius(servingRadiusInMeters,extended);
    }
    private void setServingRadius(String servingRadius,Map<String, Object> objectNode )
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
    private void validateAndAddGeoCoordinates(Map<String, Object> stringObjectMap) {
        String latStr=getIfPropertyExists(stringObjectMap,"latitude");
        String lonStr=getIfPropertyExists(stringObjectMap,"longitude");

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