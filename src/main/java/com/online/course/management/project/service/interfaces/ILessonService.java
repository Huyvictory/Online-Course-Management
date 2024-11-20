package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.LessonDTOs;

import java.util.List;

public interface ILessonService {

    LessonDTOs.LessonResponseDto createLesson(LessonDTOs.CreateLessonDTOWithChapterId request);

    List<LessonDTOs.LessonResponseDto> bulkCreateLessons(LessonDTOs.BulkCreateLessonDTO request);

    LessonDTOs.LessonResponseDto updateLesson(Long id, LessonDTOs.UpdateLessonDTO request);

    List<LessonDTOs.LessonResponseDto> bulkUpdateLessons(LessonDTOs.BulkUpdateLessonDTO request);
}
