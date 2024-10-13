package com.online.course.management.project.utils.user;

import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.exception.InvalidRoleInfoException;
import com.online.course.management.project.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
@Slf4j
public class UserControllerUtils {

    private final AuthenticationManager authenticationManager;

    public UserControllerUtils(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public Authentication authenticate(String usernameOrEmail, String password) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usernameOrEmail, password));
        } catch (AuthenticationException e) {
            log.error("Error during authentication", e);
            throw new UnauthorizedException("Invalid username/email or password");
        }
    }

    public Set<RoleType> validateRoles(Set<String> roles) {
        Set<RoleType> validRoles = new HashSet<>();
        Set<String> invalidRoles = new HashSet<>();

        for (String role : roles) {
            try {
                validRoles.add(RoleType.valueOf(role.toUpperCase()));
            } catch (IllegalArgumentException e) {
                invalidRoles.add(role);
            }
        }

        if (!invalidRoles.isEmpty()) {
            throw new InvalidRoleInfoException("Invalid role(s) provided: " + String.join(", ", invalidRoles));
        }

        return validRoles;
    }
}
