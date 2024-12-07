package com.online.course.management.project.dto;

import com.online.course.management.project.enums.CourseStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CourseDTOS {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseDetailsResponseDto {
        private Long id;
        private String title;
        private String description;
        private CourseStatus status;
        private InstructorDetailsDto instructor;
        private Set<String> categoryNames;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructorDetailsDto {
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
        @NotBlank(message = "Description is required")
        private String description;

        private Set<Long> categoryIds;

        private Long instructorId;

        private CourseStatus status = CourseStatus.DRAFT;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCourseRequestDTO {
        private String title;

        private String description;

        private CourseStatus status;

        private Set<Long> categoryIds;

        private Long instructorId; // Optional, only for admin use
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    public static class SearchCourseRequestDTO extends PaginationDto.PaginationRequestDto {
        private String title;
        private CourseStatus status;
        private String instructorName;
        private Set<Long> categoryIds;
        private Boolean includeArchived;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime fromDate;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime toDate;

        private Map<String, String> sort;

        @AssertTrue(message = "toDate must be after fromDate")
        private boolean isDateRangeValid() {
            if (fromDate == null || toDate == null) {
                return true;
            }
            return toDate.isAfter(fromDate);
        }

        @Override
        public org.springframework.data.domain.Pageable toPageable() {
            return PageRequest.of(getPage() - 1, getLimit());
        }
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class SearchInstructorCourseRequestDTO extends PaginationDto.PaginationRequestDto {
        @NotNull(message = "Instructor ID must not be null")
        private Long instructorId;

        private boolean includeArchived;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class SearchStatusRequestDTO extends PaginationDto.PaginationRequestDto {
        @NotNull(message = "Status must not be null")
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchLatestCoursesRequestDTO {
        private int limit = 10;
    }

}
