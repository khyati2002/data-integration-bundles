package com.applicate.simamy.transformer;

import com.applicate.services.channelkart.transformers.AbstractTransformer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.applicate.services.channelkart.taskexecutors.integrationtaskservices.service.IntegrationTaskService.mapper;

public class UserTransformer extends AbstractTransformer<Map<String, Object>, Map<String, Object>> {
    private static final String LOGINID = "loginId";
    private static final String NAME = "name";
    private static final String MOBILE = "mobile";
    private static final String EMAIL = "email";
    private static final String IMMEDIATE_PARENT = "immediateParent";
    private static final String SM_LOGIN_ID = "sm_loginid";
    private static final String STL_LOGIN_ID = "stl_loginid";
    private static final String TSM_LOGIN_ID = "tsm_loginid";
    private static final String RSM_LOGIN_ID = "rsm_loginid";
    private static final String DIS_LOGIN_ID = "dis_loginid";
    private static final String USER_ACCOUNT_ID = "userAccountId";
    private static final String DESIGNATION = "designation";
    private static final String SM_MOBILE = "sm_mobile";

    private static final String N_A = "NA";


    @Override
    public Object transform(Map<String, Object> input) {
        List<Map<String, Object>> responseList = new ArrayList<>();
        Map<String, Object> sm = getSaleRepMgr(input);
        if (!sm.isEmpty()) responseList.add(sm);
        Map<String, Object> stl = getStl(input);
        if (!stl.isEmpty()) responseList.add(stl);
        Map<String, Object> tsm = getTsm(input);
        if (!tsm.isEmpty()) responseList.add(tsm);
        Map<String, Object> rsm = getRsm(input);
        if (!rsm.isEmpty()) responseList.add(rsm);
        Map<String, Object> dis = getSupplier(input);
        if (!dis.isEmpty()) responseList.add(dis);
        return responseList;
    }
/*
* Distributor is Supplier, and it is directly connected to admin
* SalesRep is MGR
* */
    private Map<String, Object> getSupplier(Map<String, Object> input) {
        if (ObjectUtils.isEmpty(input.get(DIS_LOGIN_ID))) {
            return new HashMap<>();
        }
        Map<String, Object> dis = new HashMap<>();
        dis.put(LOGINID, input.get(DIS_LOGIN_ID));
        dis.put(USER_ACCOUNT_ID, input.get(DIS_LOGIN_ID));
        dis.put(NAME, ObjectUtils.isEmpty(input.get("dis_name")) ? N_A : input.get("dis_name").toString());
        if (input.get(STL_LOGIN_ID).equals(input.get(DIS_LOGIN_ID))) return new HashMap<>();
        dis.put(IMMEDIATE_PARENT, "admin@applicate.in");
        Integer max = Integer.valueOf(input.getOrDefault("max", "10000").toString());
        Integer min = Integer.valueOf(input.getOrDefault("min", "5").toString());
        String type = input.getOrDefault("type", "amount").toString();
        String level = input.getOrDefault("level", "total").toString();
        ObjectNode supplierMetaData = mapper.createObjectNode();
        supplierMetaData.put("max", max);
        supplierMetaData.put("min", min);
        supplierMetaData.put("type", type);
        supplierMetaData.put("level", level);
        dis.put("supplierMetaData", supplierMetaData);
        dis.put(DESIGNATION, "supplier");
        dis.put("mobile","0000000000");
        return dis;
    }

    private Map<String, Object> getRsm(Map<String, Object> input) {
        if (ObjectUtils.isEmpty(input.get(RSM_LOGIN_ID))) {
            return new HashMap<>();
        }
        Map<String, Object> dis = new HashMap<>();
        dis.put(LOGINID, input.get(RSM_LOGIN_ID));
        dis.put(USER_ACCOUNT_ID, input.get(RSM_LOGIN_ID));
        dis.put(NAME, ObjectUtils.isEmpty( input.get("rsm_name")) ? N_A : input.get("rsm_name").toString());
        dis.put(EMAIL, input.getOrDefault("rsm_email", null));
        dis.put(IMMEDIATE_PARENT, "admin@applicate.in");
        dis.put(DESIGNATION, "rsm");
        dis.put("mobile","0000000000");
        return dis;
    }

    private Map<String, Object> getTsm(Map<String, Object> input) {
        if (ObjectUtils.isEmpty(input.get(TSM_LOGIN_ID))) {
            return new HashMap<>();
        }
        Map<String, Object> dis = new HashMap<>();
        dis.put(LOGINID, input.get(TSM_LOGIN_ID));
        dis.put(USER_ACCOUNT_ID, input.get(TSM_LOGIN_ID));
        dis.put(NAME,ObjectUtils.isEmpty( input.get("tsm_name")) ? N_A : input.get("tsm_name").toString());
        dis.put(EMAIL, input.getOrDefault("tsm_email", null));
        if (input.get(RSM_LOGIN_ID).equals(input.get(TSM_LOGIN_ID))) return new HashMap<>();
        dis.put(IMMEDIATE_PARENT, getImmediateParentForTsm(input));
        dis.put(DESIGNATION, "tsm");
        dis.put("mobile","0000000000");
        return dis;
    }

    private Object getImmediateParentForTsm(Map<String, Object> input) {
        if (ObjectUtils.isNotEmpty(input.get(RSM_LOGIN_ID))) return input.get(RSM_LOGIN_ID);
        return "admin@applicate.in";
    }

    private Map<String, Object> getStl(Map<String, Object> input) {
        if (ObjectUtils.isEmpty(input.get(STL_LOGIN_ID))) {
            return new HashMap<>();
        }
        Map<String, Object> dis = new HashMap<>();
        dis.put(LOGINID, input.get(STL_LOGIN_ID));
        dis.put(USER_ACCOUNT_ID, input.get(STL_LOGIN_ID));
        dis.put(NAME, ObjectUtils.isEmpty(input.get("stl_name")) ? N_A : input.get("stl_name").toString());
        dis.put(EMAIL, input.getOrDefault("stl_email", null));
        if (input.get(TSM_LOGIN_ID).equals(input.get(STL_LOGIN_ID))) return new HashMap<>();
        dis.put(IMMEDIATE_PARENT, getImmediateParentForStl(input));
        dis.put(DESIGNATION, "stl");
        dis.put("mobile","0000000000");
        return dis;
    }

    private Object getImmediateParentForStl(Map<String, Object> input) {
        if (ObjectUtils.isNotEmpty(input.get(TSM_LOGIN_ID))) return input.get(TSM_LOGIN_ID);
        if (ObjectUtils.isNotEmpty(input.get(RSM_LOGIN_ID))) return input.get(RSM_LOGIN_ID);
        return "admin@applicate.in";
    }

    private Map<String, Object> getSaleRepMgr(Map<String, Object> input) {
        if (ObjectUtils.isEmpty(input.get(SM_LOGIN_ID))) {
            return new HashMap<>();
        }
        Map<String, Object> dis = new HashMap<>();
        dis.put(LOGINID, input.get(SM_LOGIN_ID));
        dis.put(USER_ACCOUNT_ID, input.get(SM_LOGIN_ID));
        dis.put(NAME, ObjectUtils.isEmpty(input.get("sm_name")) ? N_A : input.get("sm_name").toString());
        if (ObjectUtils.isNotEmpty(input.get(SM_MOBILE).toString())) {
            String mobileNumber = ObjectUtils.isEmpty(input.get(SM_MOBILE)) ? "" : validateMobile(input);
            dis.put(MOBILE, mobileNumber);
            dis.put("registeredNumber", mobileNumber);
        }
        dis.put(EMAIL, input.getOrDefault("sm_email", null));
        if (input.get(STL_LOGIN_ID).equals(input.get(SM_LOGIN_ID))) return new HashMap<>();
        dis.put(IMMEDIATE_PARENT, getImmediateParentForSalesRep(input));
        dis.put(DESIGNATION, "mgr");
        return dis;
    }

    private String validateMobile(Map<String, Object> input) {
        if (input.get(SM_MOBILE).toString().startsWith("+60")) {
            return input.get(SM_MOBILE).toString().substring(3);
        }else if (input.get(SM_MOBILE).toString().startsWith("60")) {
            return input.get(SM_MOBILE).toString().substring(2);
        }else if (input.get(SM_MOBILE).toString().startsWith("0")) {
            return input.get(SM_MOBILE).toString().substring(1);
        }
        return input.get(SM_MOBILE).toString();
    }

    private Object getImmediateParentForSalesRep(Map<String, Object> input) {
        if (ObjectUtils.isNotEmpty(input.get(STL_LOGIN_ID))) return input.get(STL_LOGIN_ID);
        if (ObjectUtils.isNotEmpty(input.get(TSM_LOGIN_ID))) return input.get(TSM_LOGIN_ID);
        if (ObjectUtils.isNotEmpty(input.get(RSM_LOGIN_ID))) return input.get(RSM_LOGIN_ID);
        return "admin@applicate.in";
    }

}
