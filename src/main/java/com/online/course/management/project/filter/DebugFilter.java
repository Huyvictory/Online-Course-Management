package com.online.course.management.project.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DebugFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(DebugFilter.class);

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        logger.info("Incoming request: {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
