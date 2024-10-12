package com.online.course.management.project.security;

import com.online.course.management.project.entity.User;
import com.online.course.management.project.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {
    private final IUserRepository userRepository;

    @Autowired
    public UserSecurity(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isUserOwner(Authentication authentication, Long userId) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        return user != null && user.getId().equals(userId);
    }
}
