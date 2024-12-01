package com.online.course.management.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserLessonProgressDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusLessonProgressDTO {
        @NotNull(message = "Lesson progress ID is required")
        private Long id;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LessonProgressResponseDTO {
        private Integer id;
        private Integer courseId;
        private Integer chapterId;
        private Integer lessonId;
        private String status;
        private String lastAccessedAt;
        private String completionDate;
    }
}
