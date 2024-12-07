package com.online.course.management.project.constants;

import java.util.Arrays;
import java.util.List;

public class PathConstants {
    public static final List<String> SWAGGER_PATHS = Arrays.asList(
            "/swagger-ui/",
            "/swagger-ui.html",
            "/swagger-ui/index.html",
            "/swagger-ui/swagger-ui.css",
            "/swagger-ui/swagger-ui-bundle.js",
            "/swagger-ui/swagger-ui-standalone-preset.js",
            "/swagger-ui/swagger-initializer.js",
            "/v3/api-docs",
            "/v3/api-docs/",
            "/v3/api-docs/swagger-config",
            "/swagger-resources",
            "/swagger-resources/",
            "/webjars/",
            "/swagger-ui/favicon-32x32.png",
            "/swagger-ui/favicon-16x16.png"
    );

    public static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/error",
            "/api/v1/courses/search",
            "/api/v1/courses/search-instructor",
            "/api/v1/courses/search-status",
            "/api/v1/courses/search-latest",
            "/api/v1/course-ratings/search",
            "/api/v1/course-ratings/get-rating-distribution/{id}"
    );
}
