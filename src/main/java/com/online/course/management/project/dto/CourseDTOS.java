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

        @AssertTrue(message = "Sort direction must be 'asc' or 'desc'")
        private boolean isSortValid() {
            if (sort == null || sort.isEmpty()) {
                return true;
            }
            return sort.values().stream()
                    .allMatch(direction -> direction == null ||
                            direction.equalsIgnoreCase("asc") ||
                            direction.equalsIgnoreCase("desc"));
        }

        @AssertTrue(message = "Sortable fields are 'createdAt' and 'updatedAt'")
        private boolean isSortFieldValid() {
            if (sort == null || sort.isEmpty()) {
                return true;
            }
            return sort.keySet().stream()
                    .allMatch(field -> field.equals("createdAt") || field.equals("updatedAt"));
        }

        @Override
        public org.springframework.data.domain.Pageable toPageable() {
            List<Sort.Order> orders = new ArrayList<>();

            if (sort != null && !sort.isEmpty()) {
                sort.forEach((field, direction) -> {
                    Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ?
                            Sort.Direction.ASC : Sort.Direction.DESC;
                    orders.add(new Sort.Order(sortDirection, field));
                });
            } else {
                // Default sort by createdAt DESC if no sort specified
                orders.add(Sort.Order.desc("createdAt"));
            }

            return PageRequest.of(getPage() - 1, getLimit(), Sort.by(orders));
        }
    }


}
