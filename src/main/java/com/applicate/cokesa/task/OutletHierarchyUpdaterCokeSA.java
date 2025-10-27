package com.applicate.cokesa.task;

import com.applicate.services.channelkart.models.DeliveryPJP;
import com.applicate.services.channelkart.models.HierarchyMetaData;
import com.applicate.services.channelkart.models.OutletDetails;
import com.applicate.services.channelkart.models.Task;
import com.applicate.services.channelkart.security.SecurityContextUtils;
import com.applicate.services.channelkart.services.OutletDetailsService;
import com.applicate.services.channelkart.taskexecutors.AbstractTaskExecutor;
import com.applicate.services.channelkart.utils.JSONUtils;
import com.applicate.services.channelkart.utils.NullUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OutletHierarchyUpdaterCokeSA extends AbstractTaskExecutor {

    private final DeliveryPJPService deliveryPJPService = SpringContext.getBean(DeliveryPJPService.class);
    private final OutletDetailsService outletDetailsService = SpringContext.getBean(OutletDetailsService.class);
    private final HierarchySynchronizer userHierarchySynchronizer = SpringContext.getBean(HierarchySynchronizer.class);
    private final OutletHierarchySynchronizer outletHierarchySynchronizer = SpringContext.getBean(OutletHierarchySynchronizer.class);
    private final Logger logger = LoggerFactory.getLogger(OutletHierarchyUpdaterCokeSA.class);

    private static final int BATCH_SIZE = 1000;

    @Override
    public void runTask(Task task) {
        String lob = SecurityContextUtils.getLob();
        SecurityContextUtils.setTempLOB(lob);

        try {
            logger.info("Starting OutletHierarchyUpdater for LOB: {}", lob);

            List<DeliveryPJP> pjpList = deliveryPJPService.findAll();

            if (pjpList.isEmpty()) {
                logger.info("No PJP data found for hierarchy update.");
                return;
            }

            Set<String> updatedOutletIds = new HashSet<>();

            Map<String, DeliveryPJP> latestPJPMap = pjpList.stream()
                    .filter(p -> p.getOutletCode() != null && !p.getOutletCode().isBlank()
                            && p.getLoginId() != null && !p.getLoginId().isBlank())
                    .collect(Collectors.toMap(
                            DeliveryPJP::getOutletCode,
                            p -> p,
                            (p1, p2) -> p1.getPjpDate().after(p2.getPjpDate()) ? p1 : p2
                    ));

            List<String> outletCodes = new ArrayList<>(latestPJPMap.keySet());

            for (List<String> batch : partitionList(outletCodes, BATCH_SIZE)) {
                List<OutletDetails> outletDetailsList = outletDetailsService.findByOutletCodeIn(batch);

                for (OutletDetails outlet : outletDetailsList) {
                    DeliveryPJP pjp = latestPJPMap.get(outlet.getOutletCode());
                    if (pjp == null) continue;

                    String salesRepCode = pjp.getLoginId();
                    if (NullUtils.isNull(salesRepCode)) continue;



                    if (outlet.getUserName() == null) {
                        logger.error("UserName is null for outletCode: {}", outlet.getOutletCode());
                        continue;
                    }

                    Set<String> currentParents = outlet.getUserName().getImmediateParent() == null
                            ? new HashSet<>()
                            : outlet.getUserName().getImmediateParent()
                            .stream()
                            .map(HierarchyMetaData::getImmediateParent)
                            .collect(Collectors.toSet());


                    if (!currentParents.contains(salesRepCode)) {

                        HierarchyMetaData hMeta = new HierarchyMetaData();
                        hMeta.setImmediateParent(salesRepCode);

                        outlet.getUserName().setImmediateParent(List.of(hMeta));
                        outlet.setImmediateParent(List.of());
                        outletDetailsService.save(outlet);

                        updatedOutletIds.add(outlet.getId());
                    }
                }
            }

            userHierarchySynchronizer.synchronize(true, true, null);
            outletHierarchySynchronizer.synchronize(new ArrayList<>(updatedOutletIds));

            logger.info("Outlet hierarchy updated successfully for {} outlets.", updatedOutletIds.size());

        } catch (Exception exception) {
            logger.error("Error running task to synchronize hierarchy", exception);

            ObjectNode responseNode = JSONUtils.getObjectMapper().createObjectNode();
            responseNode.put("errorSummary", exception.getMessage());
            responseNode.put("stackTrace", ExceptionUtils.getStackTrace(exception));

            task.setTaskResponse(responseNode);
            task.setStatus(Task.TaskStatus.FAILURE);
        }


    }

    private <T> List<List<T>> partitionList(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }


    @Override
    public String getTaskType() {
        return "CokeSAOutletHierarchyUpdater";
    }
}