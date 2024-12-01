package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.UserLessonProgressDtos;

public interface IUserLessonProgressService {

    UserLessonProgressDtos.LessonProgressResponseDTO startLearningLesson(UserLessonProgressDtos.UpdateStatusLessonProgressDTO request);

    UserLessonProgressDtos.LessonProgressResponseDTO completeLearningLesson(UserLessonProgressDtos.UpdateStatusLessonProgressDTO request);
}
