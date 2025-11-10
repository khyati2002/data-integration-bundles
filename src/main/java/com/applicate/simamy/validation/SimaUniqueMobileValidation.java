package com.applicate.simamy.validation;

import com.applicate.services.channelkart.models.User;
import com.applicate.services.channelkart.repository.UserRepository;
import com.applicate.services.channelkart.services.SpringContext;
import com.applicate.services.channelkart.validations.AbstractRule;
import com.applicate.services.channelkart.validations.RuleResult;
import com.applicate.services.channelkart.validations.Status;

import java.util.List;

public class SimaUniqueMobileValidation  extends AbstractRule<User> {

    private final UserRepository userRepository = SpringContext.getBean(UserRepository.class);

    @Override
    public RuleResult apply(User user) {
        // Use UserService instead of Repository. Create new method in UserService
        String newMobileNumber = user.getMobile();
        if(newMobileNumber.equals("0000000000")) return RuleResult.OK;
        List<User> userList = userRepository.findByMobile(newMobileNumber);
        if (userList.isEmpty() || isSameUser(userList, user)) {
            return RuleResult.OK;
        }
        return new RuleResult(Status.ERROR, "Mobile number already in use. Please use a different mobile number.");
    }

    /**
     * Check if the userList contains only a single object and the object is of the provided user.
     * @param userList list of users having same mobile number
     * @param user user to update
     * @param newMobileNumber is default phone number
     * @return boolean true or false
     */
    private boolean isSameUser(List<User> userList, User user){
        return userList.size()==1 && userList.get(0).getLoginId().equalsIgnoreCase(user.getLoginId());
    }
}
