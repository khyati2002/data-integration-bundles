package com.applicate.cokearg.enrichment;

import com.applicate.services.channelkart.services.ServiceLocator;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.List;
import java.util.Map;

public class CokeArgKpiDataEnrichment extends AbstractEnrichment<Map<String, Object>> {

    private static final String USER_PARENT_QUERY = "select loginid from ck_user where extended_attributes->>'$[0].ruta'=?";
    private static final String USER_PARENT_QUERY_FOR_OUTLET = "select up.parent as loginid from ck_user_parent up inner join ck_userdesignation ud on ud.login_id=up.parent and designation='preseller' and userloginid=?";

    @Override
    public OperationResult.StepResult apply(Map<String, Object> kpiDataMap) {
        try {
            String name = (String) kpiDataMap.get("name");
            if (name == null) {
                 return new OperationResult.StepResult(OperationResult.Status.OK, "Skipping enrichment: name is null");
            }

            DSLContext dsl = (DSLContext) ServiceLocator.lookup(DSLContext.class);

            if(List.of("kpi_ratio_adherencia_ruta",
                    "kpi_cliente_compradores_ruta",
                    "kpi_share_of_reference_ruta_categoria",
                    "kpi_share_of_reference_ruta","kpi_recuperacion_ruta","kpi_convivencia_ruta",
                    "kpi_cliente_compradores_ruta"
            ).contains(name)) {
                String value = (String) kpiDataMap.get("value");
                if (value != null) {
                    Record result = dsl.fetchOne(USER_PARENT_QUERY, value);
                    if(result == null) {
                        return new OperationResult.StepResult(OperationResult.Status.ERROR,"No salesrep found for the given Ruta "+value);
                    }
                    Object loginid = result.get("loginid");
                    if (loginid != null) {
                        kpiDataMap.put("loginId", loginid.toString());
                    }
                }
            }
            
            if(List.of("kpi_ratio_adherencia_cliente",
                    "kpi_ratio_adherencia_cliente","kpi_share_of_reference_cliente").contains(name)) {
                String outletcode = (String) kpiDataMap.get("outletCode");
                if (outletcode != null) {
                    Record result = dsl.fetchOne(USER_PARENT_QUERY_FOR_OUTLET, outletcode);
                    if(result == null) {
                        return new OperationResult.StepResult(OperationResult.Status.ERROR,"No salesrep found for the given outletCode "+outletcode);
                    }
                    Object loginid = result.get("loginid");
                    if (loginid != null) {
                        kpiDataMap.put("loginId", loginid.toString());
                    }
                }
            }
            
            return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully for KPI");

        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Error occurred while enriching KPI data: " + e.getMessage());
        }
    }
}
