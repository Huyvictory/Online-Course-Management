package com.online.course.management.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.online.course.management.project.enums.EnrollmentStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Map;

public class UserCourseDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCourseResponseDto {
        private Long id;
        private Long userId;
        private Long courseId;
        private String courseTitle;
        private String instructorName;
        private EnrollmentStatus status;
        private LocalDateTime enrollmentDate;
        private LocalDateTime completionDate;
        private Integer processingLessons;
        private Integer completedLessons;
        private Integer totalLessons;
        private Double averageRating;
        private Double averageCompletionTime;
        private Double completionRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCourseRequestDTO {
        @NotNull(message = "Course ID is required")
        private Long courseId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UserCourseSearchDTO extends PaginationDto.PaginationRequestDto {
        private String name;
        private EnrollmentStatus status;
        private String instructorName;
        private Double minRating;
        private Double maxRating;
        private Integer totalLessons;

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

        @AssertTrue(message = "maxRating must be greater than minRating")
        private boolean isRatingRangeValid() {
            if (minRating == null || maxRating == null) {
                return true;
            }
            return maxRating >= minRating;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateEnrollmentStatusDTO {
        @NotNull(message = "Status is required")
        private EnrollmentStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCourseStatisticsDTO {
        private Long totalInProgress;
        private Long totalCompleted;
        private Double averageCompletionTime;  // in days
        private Double completionRate;  // percentage
    }
}
