package com.applicate.cokesa.task;


import com.applicate.services.channelkart.models.EntityParentMapping;
import com.applicate.services.channelkart.models.HierarchyMetaData;
import com.applicate.services.channelkart.models.Task;
import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.services.EntityParentMappingService;
import com.applicate.services.channelkart.services.HierarchySynchronizer;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.services.UserService;
import com.applicate.services.channelkart.taskexecutors.AbstractTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DepotHierarchyUpdaterCokeSA extends AbstractTaskExecutor {

    private final EntityParentMappingService entityParentMappingService = SpringContext.getBean(EntityParentMappingService.class);
    private final UserService userService = SpringContext.getBean(UserService.class);
    private final HierarchySynchronizer userHierarchySynchronizer = SpringContext.getBean(HierarchySynchronizer.class);
    @Override
    public void runTask(Task task){
        List<User> suppliersIds = userService.findByDesignation(List.of("supplier"), null, null, "sup");
        Map<String, User> loginIdSupplier = suppliersIds.stream().collect(Collectors.toMap(User::getLoginId, Function.identity()));
        List<EntityParentMapping> depotsParentMapping = entityParentMappingService.findAll();
        Map<String, List<String>> depotParentMap = depotsParentMapping.stream().collect(Collectors.groupingBy(EntityParentMapping::getUser, Collectors.mapping(EntityParentMapping::getParent, Collectors.toList())));

        for(String depotLoginId : depotParentMap.keySet()){
            List<HierarchyMetaData> immediateParentList = new ArrayList<>();
            for(String depotParent: depotParentMap.get(depotLoginId)){
                HierarchyMetaData hmd = new HierarchyMetaData();
                hmd.setImmediateParent(depotParent);
                immediateParentList.add(hmd);
            }
            User depot = loginIdSupplier.get(depotLoginId);
            depot.setImmediateParent(immediateParentList);
            userService.save(depot);

        }
        userHierarchySynchronizer.synchronize(true, true, null);
    }

    

    @Override
    public String getTaskType() {
        return "DepotHierarchyUpdaterCokeSA";
    }

}
