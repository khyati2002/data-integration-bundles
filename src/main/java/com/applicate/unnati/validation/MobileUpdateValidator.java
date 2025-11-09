package com.applicate.unnati.validation;


import com.applicate.services.channelkart.models.diff.Change;
import com.applicate.services.channelkart.services.ApprovalInfoService;
import com.salescode.dim.jooq.impl.ApprovalInfo;
import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.utils.StringUtils;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.generated.tables.pojos.Metadata;
import com.salescode.dim.jooq.impl.User;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class MobileUpdateValidator extends AbstractValidationRule<User> {

	ApprovalInfoService approvalInfoService = (ApprovalInfoService) ServiceLocator.lookup(ApprovalInfo.class);


	@Override
	public OperationResult.StepResult apply(User cdm) {
		if (!StringUtils.isNullOrBlank(cdm.getChanges())) {
			for (Change<Serializable> change : cdm.getChanges()) {
				if (change.getName().equalsIgnoreCase("mobile") && !(change.getCurrent().equals(change.getPrevious()))) {
					if (isOutletStatusPending(cdm.getLoginid())) {
						return new OperationResult.StepResult(OperationResult.Status.ERROR, "The outlet is in Pending Status Mobile Number cannot be updated");
					} else {
						return OperationResult.StepResult.OK;
					}
				}
			}
		}
		return OperationResult.StepResult.OK;
	}

	public boolean isOutletStatusPending(String loginId) {
		boolean isPending = false;
		List<ApprovalInfo> approvalInfoList = approvalInfoService.findByReferenceIdList(Arrays.asList(loginId));
		for (ApprovalInfo approvalInfo : approvalInfoList) {
			if (approvalInfo != null && approvalInfo.getStatus() != null && approvalInfo.getStatus().toString().equalsIgnoreCase("PENDING")) {
				isPending = true;
			} else if (approvalInfo != null && approvalInfo.getStatus() != null && !approvalInfo.getStatus().toString().equalsIgnoreCase("PENDING")) {
				return false;
			}
		}
		return isPending;
	}
}
