package com.applicate.cokearg.enrichment;

import com.applicate.services.channelkart.models.CommonDataModel;
import com.applicate.services.channelkart.services.AbstractCDMService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.List;

import static com.salescode.dim.jooq.generated.tables.CkUser.CK_USER;
import static com.salescode.dim.jooq.generated.tables.CkUserParent.CK_USER_PARENT;

public class CokeArgKpiDataEnrichment extends AbstractEnrichment<CommonDataModel> {

    @Override
    public OperationResult.StepResult apply(CommonDataModel kpiData) {
        try {
            JsonNode extendedAttributes = kpiData.getExtendedAttributes();
            if (extendedAttributes == null) {
                 return new OperationResult.StepResult(OperationResult.Status.OK, "Skipping enrichment: extendedAttributes is null");
            }

            String name = extendedAttributes.has("name") ? extendedAttributes.get("name").asText() : null;
            if (name == null) {
                 return new OperationResult.StepResult(OperationResult.Status.OK, "Skipping enrichment: name is null");
            }

            DSLContext dsl = AbstractCDMService.getDslContext();

            if(List.of("kpi_ratio_adherencia_ruta",
                    "kpi_cliente_compradores_ruta",
                    "kpi_share_of_reference_ruta_categoria",
                    "kpi_share_of_reference_ruta","kpi_recuperacion_ruta","kpi_convivencia_ruta",
                    "kpi_cliente_compradores_ruta"
            ).contains(name)) {
                String value = extendedAttributes.has("value") ? extendedAttributes.get("value").asText() : null;
                if (value != null) {
                    // select loginid from ck_user where extended_attributes->>'$[0].ruta'=?
                    Record result = dsl.select(CK_USER.LOGINID)
                            .from(CK_USER)
                            .where(DSL.field("extended_attributes->>'$[0].ruta'").eq(value))
                            .fetchOne();

                    if(result == null) {
                        return new OperationResult.StepResult(OperationResult.Status.ERROR,"No salesrep found for the given Ruta "+value);
                    }
                    String loginid = result.get(CK_USER.LOGINID);
                    if (loginid != null) {
                        if (extendedAttributes instanceof ObjectNode) {
                            ((ObjectNode) extendedAttributes).put("loginId", loginid);
                        }
                    }
                }
            }
            
            if(List.of("kpi_ratio_adherencia_cliente",
                    "kpi_ratio_adherencia_cliente","kpi_share_of_reference_cliente").contains(name)) {
                String outletcode = extendedAttributes.has("outletCode") ? extendedAttributes.get("outletCode").asText() : null;
                if (outletcode != null) {
                    // select up.parent as loginid from ck_user_parent up inner join ck_userdesignation ud on ud.login_id=up.parent and designation='preseller' and userloginid=?
                    Record result = dsl.select(CK_USER_PARENT.PARENT.as("loginid"))
                            .from(CK_USER_PARENT)
                            .innerJoin(DSL.table("ck_userdesignation").as("ud"))
                            .on(DSL.field("ud.login_id").eq(CK_USER_PARENT.PARENT))
                            .and(DSL.field("designation").eq("preseller"))
                            .and(CK_USER_PARENT.USERLOGINID.eq(outletcode))
                            .fetchOne();

                    if(result == null) {
                        return new OperationResult.StepResult(OperationResult.Status.ERROR,"No salesrep found for the given outletCode "+outletcode);
                    }
                    Object loginid = result.get("loginid");
                    if (loginid != null) {
                        if (extendedAttributes instanceof ObjectNode) {
                            ((ObjectNode) extendedAttributes).put("loginId", loginid.toString());
                        }
                    }
                }
            }
            
            return new OperationResult.StepResult(OperationResult.Status.OK, "Data enriched successfully for KPI");

        } catch (Exception e) {
            return new OperationResult.StepResult(OperationResult.Status.ERROR, "Error occurred while enriching KPI data: " + e.getMessage());
        }
    }
}
