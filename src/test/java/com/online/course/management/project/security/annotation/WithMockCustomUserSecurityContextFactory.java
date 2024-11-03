package com.online.course.management.project.security.annotation;

import com.online.course.management.project.entity.User;
import com.online.course.management.project.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        User user = new User();
        user.setId(1L);
        user.setUsername(annotation.username());
        user.setEmail(annotation.username() + "@example.com");

        Set<GrantedAuthority> authorities = Arrays.stream(annotation.roles())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());

        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password", authorities);
        context.setAuthentication(auth);
        return context;
    }
}