package com.online.course.management.project.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Autowired
    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String message;
        if (request.getAttribute("expired") != null) {
            message = "JWT token has expired";
        } else if (request.getAttribute("invalid") != null) {
            message = "Invalid JWT token";
        } else if (authException != null && authException.getMessage() != null) {
            message = authException.getMessage();
        } else {
            message = "Authentication required: Please provide a valid JWT token";
        }

        ErrorResponseDTO errorResponse = new ErrorResponseDTO(
                message,
                HttpStatus.UNAUTHORIZED.value()
        );

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
