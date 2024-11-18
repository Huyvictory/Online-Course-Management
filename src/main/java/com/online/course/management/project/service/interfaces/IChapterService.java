package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.ChapterDTOs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IChapterService {

    ChapterDTOs.ChapterDetailResponseDto createChapter(ChapterDTOs.CreateChapterDTO request);

    List<ChapterDTOs.ChapterDetailResponseDto> bulkCreateChapters(ChapterDTOs.BulkCreateChapterDTO request);

    ChapterDTOs.ChapterDetailResponseDto updateChapter(Long id, ChapterDTOs.UpdateChapterDTO request);

    List<ChapterDTOs.ChapterResponseDto> bulkUpdateChapters(List<Long> ids, List<ChapterDTOs.UpdateChapterDTO> chapters);

    void deleteChapter(Long id);

    void bulkDeleteChapters(List<Long> ids);

    void restoreChapter(Long id);

    void bulkRestoreChapters(List<Long> ids);


    ChapterDTOs.ChapterResponseDto getChapterById(Long id);

    ChapterDTOs.ChapterDetailResponseDto getChapterWithLessons(Long id);

    List<ChapterDTOs.ChapterResponseDto> getAllChaptersByCourseId(Long courseId);

    Page<ChapterDTOs.ChapterResponseDto> searchChapters(
            ChapterDTOs.ChapterSearchDTO request,
            Pageable pageable
    );

    void reorderChapters(Long courseId);
}