package com.online.course.management.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.online.course.management.project.enums.CourseStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ChapterDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterResponseDto {
        private Long id;
        private Long courseId;
        private String courseTitle;
        private String title;
        private String description;
        private Integer order;
        private CourseStatus status;
        private int totalLessons;
        private int completedLessons;
        private int inProgressLessons;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateChapterDTO {
        @NotNull(message = "Course ID is required")
        private Long courseId;

        @NotBlank(message = "Chapter title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        private String title;

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        private String description;

        @Min(value = 1, message = "Order must be greater than 0")
        private Integer order;

        @Valid
        private List<LessonDTOs.CreateLessonDTO> lessons;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateChapterDTO {
        @Size(max = 255, message = "Title must not exceed 255 characters")
        private String title;

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        private String description;

        @Min(value = 1, message = "Order must be greater than 0")
        private Integer order;

        private CourseStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChapterSearchDTO extends PaginationDto.PaginationRequestDto {
        private String title;
        private CourseStatus status;
        private Long courseId;

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
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChapterDetailResponseDto {
        private Long id;
        private Long courseId;
        private String courseTitle;
        private CourseStatus courseStatus;
        private String title;
        private String description;
        private Integer order;
        private CourseStatus status;
        private List<LessonDTOs.LessonResponseDto> lessons;
        private int completedLessons;
        private int inProgressLessons;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkCreateChapterDTO {
        @NotEmpty(message = "At least one chapter is required")
        private List<CreateChapterDTO> chapters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUpdateChapterDTO {

        @NotNull(message  = "Chapter ids are required")
        private List<Long> chapterIds;

        @NotEmpty(message = "At least one chapter is required")
        private List<UpdateChapterDTO> chapters;
    }

    @Data
    @AllArgsConstructor
    public static class ChapterOrderDTO {
        @NotNull(message = "Chapter ID is required")
        private Long id;

        @NotNull(message = "Order is required")
        @Min(value = 1, message = "Order must be greater than 0")
        private Integer order;
    }

    @Data
    @AllArgsConstructor
    public static class RestoreChapterResponseDTO {
        private String message;
    }
}