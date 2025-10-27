package com.applicate.cokesa.transformer;


import com.applicate.services.channelkart.models.Location;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.services.LocationService;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.SupplierInfoService;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.applicate.services.channelkart.utils.TimerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


/**
 *  transformer for outlet details to add whole location object in response
 *
 */
public class OutletLocationTransformer extends AbstractTransformer<Map<String,Object>, Map<String,Object>> {

    private static final String LOCATION = "location";
    public static final String SUPPLIER = "supplier";
    private UserService userRepository = SpringContext.getBean(UserService.class);
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private SupplierInfoService supplierInfoService = SpringContext.getBean(SupplierInfoService.class);

    @SuppressWarnings("unchecked")
    @Override
    public Object transform(Map<String, Object> s) {
        List<Map<String,Object>> outletList = (List<Map<String, Object>>) s.get("features");
        Set<String> hierarhcies = outletList.stream().filter(outlet->outlet.containsKey(LOCATION)).map(outlet->outlet.get(LOCATION).toString()).collect(Collectors.toSet());
        LocationService service = (LocationService) ServiceLocator.lookup(Location.class);
        List<Location> locations = service.findByLocationHierarchy(hierarhcies);
        Map<String, Location> collect = locations.stream().collect(Collectors.toMap(Location::getLocationHierarchy,location->location));
        Map<String, String> suppliersList = new HashMap<>();
        outletList.forEach(outlet -> {
                    outlet.put(LOCATION, collect.getOrDefault(outlet.get(LOCATION), new Location()));
                    outlet.put("SupplierInfo", TimerUtils.withTime("Time taken to getSupplierInfo",
                            () -> getSupplierInfo(outlet,suppliersList)));
                }
        );
        s.put("features", outletList);
        return s;
    }
    private List<String> getSupplierFromOutletCode(String outletCode) {
        return supplierInfoService.findSuppliersUsingOutletCode(outletCode);
    }
    private List<Map<String, String>> getSupplierInfo(Map<String, Object> outlet,Map<String, String> allSuppliersList) {
        Set<String> currentSuppliersSet = extractSupplierSet(outlet);
        return getSupplierMap(currentSuppliersSet,allSuppliersList) ;
    }

    private List<Map<String, String>> getSupplierMap(Set<String> currentSuppliersSet,Map<String, String> allSuppliersList) {
        if(!allSuppliersList.keySet().containsAll(currentSuppliersSet)) {
            List<User> suppliersList= userRepository.findByLoginIdIn( new ArrayList<>(currentSuppliersSet));
            allSuppliersList.putAll( suppliersList.stream()
                    .collect(Collectors.toMap(User::getLoginId, User::getName)));
        }
        return currentSuppliersSet.stream().map(entry -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("supplierCode", entry);
                    map.put("supplierName", allSuppliersList.get(entry));
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Set<String> extractSupplierSet(Map<String, Object> outlet) {
        String outletCode = outlet.get("outletCode").toString();
        Set<String> suppliersSet =   new HashSet<>();
        if(outlet.containsKey(SUPPLIER)){
            suppliersSet.addAll((Collection<String>) outlet.get(SUPPLIER));
        }
        var extendedAttributes= ((LinkedHashMap)outlet.get("extendedAttributes"));
        if(extendedAttributes.containsKey(SUPPLIER)){
            suppliersSet.addAll((List<String>) extendedAttributes.get(SUPPLIER));
        }
        if(extendedAttributes.containsKey("parent-0-1-id")){
            suppliersSet.add(String.valueOf(extendedAttributes.get("parent-0-1-id")));
        }

        if (suppliersSet.isEmpty()){
            List<String> suppliers = TimerUtils.withTime("Time taken to getSupplierFromOutletCode",
                    () -> getSupplierFromOutletCode(outletCode));
            suppliersSet.addAll(suppliers);
        }
        if (suppliersSet.isEmpty()){
            log.debug("No Supplier found for outlet code {} ", outletCode);
        }else{
            log.debug("Supplier for outlet code {} are {}", outletCode, suppliersSet);
        }
        return suppliersSet;
    }


}