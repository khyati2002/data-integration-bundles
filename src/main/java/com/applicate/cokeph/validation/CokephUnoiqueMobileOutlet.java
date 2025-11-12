package com.applicate.cokeph.validation;


import com.applicate.services.channelkart.services.ServiceLocator;
import com.applicate.services.channelkart.services.UserService;
import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.validation.AbstractValidationRule;
import com.salescode.dim.jooq.generated.tables.pojos.User;

import java.util.List;

public class CokephUnoiqueMobileOutlet extends AbstractValidationRule<User> {

    final UserService userRepository = (UserService) ServiceLocator.lookup(com.salescode.dim.jooq.impl.User.class);

    @Override
    public OperationResult.StepResult apply(User user) {
        // Use UserService instead of Repository. Create new method in UserService
        String newMobileNumber = user.getMobile();
        if(newMobileNumber.equals("0000000000")) return OperationResult.StepResult.OK;
        List<com.salescode.dim.jooq.impl.User> userList = userRepository.findByMobile(newMobileNumber);

        if (userList.isEmpty() || isSameUser(userList, user) ) {
            return OperationResult.StepResult.OK;
        }

        return new OperationResult.StepResult(OperationResult.Status.ERROR, "Mobile number already in use. Please use a different mobile number.");
    }

    /**
     * Check if the userList contains only a single object and the object is of the provided user.
     * @param userList list of users having same mobile number
     * @param user user to update
     * @return boolean true or false
     */
    private boolean isSameUser(List<com.salescode.dim.jooq.impl.User> userList, User user){
        return userList.size()==1 && userList.get(0).getLoginid().equalsIgnoreCase(user.getLoginid());
    }
}