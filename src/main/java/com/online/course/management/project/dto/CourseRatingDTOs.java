package com.online.course.management.project.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

public class CourseRatingDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseRatingCreateDTO {
        @NotNull(message = "Course ID is required")
        private Long courseId;

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        private Integer rating;

        private String reviewText;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseRatingUpdateDTO {
        @NotNull(message = "Id course rating is required")
        private Long id;

        @NotNull(message = "Course ID is required")
        private Long courseId;

        private Integer rating;
        private String reviewText;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseRatingResponseDTO {
        private Long id;
        private Long userId;
        private String reviewerName;
        private Long courseId;
        private Integer rating;
        private String reviewText;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseRatingSearchDTO extends PaginationDto.PaginationRequestDto {
        @NotNull(message = "Course ID is required")
        private Long courseId;

        private Integer minRating;
        private Integer maxRating;

        private Map<String, String> sort;
    }
}
