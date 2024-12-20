package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.LessonDTOs;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ILessonService {

    LessonDTOs.LessonResponseDto createLesson(LessonDTOs.CreateLessonDTOWithChapterId request);

    List<LessonDTOs.LessonResponseDto> bulkCreateLessons(LessonDTOs.BulkCreateLessonDTO request);

    LessonDTOs.LessonResponseDto updateLesson(Long id, LessonDTOs.UpdateLessonDTO request);

    List<LessonDTOs.LessonResponseDto> bulkUpdateLessons(LessonDTOs.BulkUpdateLessonDTO request);

    void deleteSingleLesson(Long id);

    void bulkDeleteLessons(List<Long> ids);

    void restoreLesson(Long id);

    void bulkRestoreLessons(List<Long> ids);

    Page<LessonDTOs.LessonDetailResponseDto> searchLessons(LessonDTOs.LessonSearchDTO request);

    void reorderLessons(Long chapterId);
}
