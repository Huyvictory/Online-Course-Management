package com.online.course.management.project.aspect;

import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.UnauthorizedException;
import com.online.course.management.project.security.RequiredRole;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class RoleAuthorizationAspect {

    private static final Logger logger = LoggerFactory.getLogger(RoleAuthorizationAspect.class);

    @Around("@annotation(requiredRole)")
    public Object authorizeRole(ProceedingJoinPoint joinPoint, RequiredRole requiredRole) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();


        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthorized access attempt: User is not authenticated");
            throw new UnauthorizedException("User is not authenticated");
        }

        logger.info("Required roles: {}", Arrays.toString(requiredRole.value()));
        logger.info("User authorities: {}", authentication.getAuthorities());

        // Get authorities and strip "ROLE_" prefix if present
        Set<String> userRoles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());

        boolean hasRequiredRole = Arrays.stream(requiredRole.value())
                .anyMatch(userRoles::contains);

        if (!hasRequiredRole) {
            logger.warn("Forbidden access attempt: User {} does not have any of the required roles: {}",
                    authentication.getName(), Arrays.toString(requiredRole.value()));
            throw new ForbiddenException("User does not have the required role");
        }

        return joinPoint.proceed();
    }
}
