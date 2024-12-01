package com.online.course.management.project.utils.user;

import com.online.course.management.project.entity.User;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.exception.business.UnauthorizedException;
import com.online.course.management.project.repository.IUserRepository;
import com.online.course.management.project.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class UserSecurityUtils {

    private final IUserRepository userRepository;

    @Autowired
    public UserSecurityUtils(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return (CustomUserDetails) authentication.getPrincipal();
    }

    public User getCurrentUser() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public boolean isAdmin() {
        return getCurrentUserDetails().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
