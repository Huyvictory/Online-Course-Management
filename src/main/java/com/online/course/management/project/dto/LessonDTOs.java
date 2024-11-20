package com.online.course.management.project.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.LessonType;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public class LessonDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonResponseDto {
        private Long id;
        private Long chapterId;
        private String chapterTitle;
        private String title;
        private String content;
        private Integer order;
        private LessonType type;
        private CourseStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonDetailResponseDto {
        private Long id;
        private Long chapterId;
        private String chapterTitle;
        private Long courseId;
        private String courseTitle;
        private String title;
        private String content;
        private Integer order;
        private LessonType type;
        private CourseStatus status;
        private Integer completedByUsers;
        private Integer inProgressByUsers;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateLessonDTO {
        @NotBlank(message = "Lesson title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        private String title;

        @NotBlank(message = "Content is required")
        private String content;

        @NotNull(message = "Lesson order is required")
        @Min(value = 1, message = "Order must be greater than 0")
        private Integer order;

        @NotNull(message = "Lesson type is required")
        private LessonType type;

        private CourseStatus status = CourseStatus.DRAFT;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateLessonDTOWithChapterId extends CreateLessonDTO {
        @NotNull(message = "Chapter ID is required")
        private Long chapterId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateLessonDTO {
        @Size(max = 255, message = "Title must not exceed 255 characters")
        private String title;

        private String content;

        @Min(value = 1, message = "Order must be greater than 0")
        private Integer order;

        private LessonType type;

        private CourseStatus status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LessonSearchDTO extends PaginationDto.PaginationRequestDto {
        private String title;
        private CourseStatus status;
        private Long courseId;
        private Long chapterId;
        private LessonType type;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime fromDate;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime toDate;

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
    public static class BulkCreateLessonDTO {
        @NotNull(message = "Chapter ID is required")
        private Long chapterId;

        @NotEmpty(message = "At least one lesson is required")
        private List<CreateLessonDTO> lessons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUpdateLessonDTO {

        @NotNull(message = "At least one lesson Ids is required")
        private List<Long> lessonIds;

        @NotEmpty(message = "At least one lesson is required")
        private List<UpdateLessonDTO> lessons;
    }

    @Data
    @AllArgsConstructor
    public static class RestoreLessonResponseDTO {
        private String message;
    }
}