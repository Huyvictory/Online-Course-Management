package com.online.course.management.project.security;

import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.UnauthorizedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class RoleAuthorizationAspect {
    @Around("@annotation(requiredRole)")
    public Object authorizeRole(ProceedingJoinPoint joinPoint, RequiredRole requiredRole) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("User is not authenticated");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Set<String> userRoles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        boolean hasRequiredRole = Arrays.stream(requiredRole.value())
                .anyMatch(userRoles::contains);

        if (!hasRequiredRole) {
            throw new ForbiddenException("User does not have the required role");
        }

        return joinPoint.proceed();
    }
}
