package com.datapath.web.util;

import com.datapath.persistence.entities.monitoring.User;
import com.datapath.web.dto.ApplicationUser;
import org.springframework.beans.BeanUtils;

public class UserUtils {

    public static ApplicationUser convertToDTO(User user) {
        ApplicationUser applicationUser = new ApplicationUser();
        BeanUtils.copyProperties(user, applicationUser);
        applicationUser.setPassword(null);
        applicationUser.setRole(user.getRole().getRole());
        return applicationUser;
    }
}
