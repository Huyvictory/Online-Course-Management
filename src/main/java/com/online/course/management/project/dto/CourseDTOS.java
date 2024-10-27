package com.online.course.management.project.dto;

import com.online.course.management.project.enums.CourseStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Set;

public class CourseDTOS {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseDetailsResponseDto {
        private Long id;
        private String title;
        private String description;
        private CourseStatus status;
        private Set<String> categoryNames;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private InstructorDto instructor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseListResponseDto {
        private Long id;
        private String title;
        private String description;
        private CourseStatus status;
        private Long instructorId;
        private Set<String> categoryNames;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructorDto {
        private Long id;
        private String username;
        private String email;
        private String realName;
        private Set<String> roles;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCourseRequestDTO {
        @NotBlank(message = "Course title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        private String title;

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        private String description;

        private Set<Long> categoryIds;

        private Long instructorId; // Optional, only for admin use

        private CourseStatus status = CourseStatus.DRAFT; // Default status
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCourseRequestDTO {
        @Size(max = 255, message = "Title must not exceed 255 characters")
        private String title;

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        private String description;

        private CourseStatus status;

        private Set<Long> categoryIds;

        private Long instructorId; // Optional, only for admin use
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchCourseRequestDTO {
        private String title;
        private CourseStatus status;
        private String instructorName;
        private Set<Long> categoryIds;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime fromDate;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime toDate;
    }
}
