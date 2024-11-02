package com.online.course.management.project.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.dto.ErrorResponseDTO;
import com.online.course.management.project.security.CustomUserDetailsService;
import com.online.course.management.project.security.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        try {
            // Skip authentication for permitted paths
            if (shouldSkipAuthentication(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = getJwtFromRequest(request);

            // If no token is present, handle accordingly
            if (jwt == null) {
                handleMissingToken(response);
                return;
            }

            try {
                if (jwtUtil.validateToken(jwt)) {
                    String username = jwtUtil.extractUsername(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    filterChain.doFilter(request, response);
                }
            } catch (ExpiredJwtException e) {
                handleTokenError(response, "JWT token has expired", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                handleTokenError(response, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            handleTokenError(response, "Authentication failed", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String path = request.getServletPath();
        log.info("Checking if request should be skipped: {}", path);

        // Define patterns for public endpoints
        List<String> publicEndpoints = Arrays.asList("/api/v1/users/login", "/api/v1/users/register", "/error");

        // Check exact matches first
        if (publicEndpoints.contains(path) || request.getMethod().equals("OPTIONS")) {
            return true;
        }

        // Check course-related patterns
        if (path.matches("/api/v1/courses/\\d+") ||    // Matches /courses/{id}
                path.equals("/api/v1/courses/search") || path.equals("/api/v1/courses/search-instructor") || path.equals("/api/v1/courses/search-status") || path.equals("/api/v1/courses/search-latest")) {    // Matches /courses/search
            return request.getMethod().equals("POST");   // Only allow POST requests
        }

        return false;
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void handleMissingToken(HttpServletResponse response) throws IOException {
        handleTokenError(response, "Authentication required", HttpStatus.UNAUTHORIZED);
    }

    private void handleTokenError(HttpServletResponse response, String message, HttpStatus status) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(message, status.value());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
