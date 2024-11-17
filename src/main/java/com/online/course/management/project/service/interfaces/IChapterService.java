package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.ChapterDTOs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IChapterService {
    /**
     * Creates a new chapter for a course
     * Validates:
     * - Course exists and is not archived
     * - Order number is unique within the course
     * - Current user has permission to modify the course
     */
    ChapterDTOs.ChapterDetailResponseDto createChapter(ChapterDTOs.CreateChapterDTO request);

    /**
     * Creates multiple chapters at once for a course
     * Validates:
     * - Course exists and is not archived
     * - Order numbers are unique within the course
     * - Current user has permission to modify the course
     * - Maximum 50 chapters per request
     */
    List<ChapterDTOs.ChapterDetailResponseDto> bulkCreateChapters(ChapterDTOs.BulkCreateChapterDTO request);

    /**
     * Updates an existing chapter
     * Validates:
     * - Chapter exists
     * - Course is not archived
     * - Order number is unique within the course
     * - Current user has permission to modify the course
     */
    ChapterDTOs.ChapterResponseDto updateChapter(Long id, ChapterDTOs.UpdateChapterDTO request);

    /**
     * Updates multiple chapters at once
     * Validates:
     * - All chapters exist
     * - Course is not archived
     * - Order numbers are unique within the course
     * - Current user has permission to modify the course
     * - Maximum 50 chapters per request
     */
    List<ChapterDTOs.ChapterResponseDto> bulkUpdateChapters(List<Long> ids, ChapterDTOs.BulkUpdateChapterDTO request);

    /**
     * Soft deletes a chapter and its associated lessons
     * Validates:
     * - Chapter exists
     * - Course is not archived
     * - Current user has permission to modify the course
     */
    void deleteChapter(Long id);

    /**
     * Soft deletes multiple chapters and their associated lessons
     * Validates:
     * - All chapters exist
     * - Course is not archived
     * - Current user has permission to modify the course
     * - Maximum 50 chapters per request
     */
    void bulkDeleteChapters(List<Long> ids);

    /**
     * Restores a soft-deleted chapter
     * Validates:
     * - Chapter exists and is soft-deleted
     * - Course is not archived
     * - Current user has permission to modify the course
     */
    void restoreChapter(Long id);

    /**
     * Restores multiple soft-deleted chapters
     * Validates:
     * - All chapters exist and are soft-deleted
     * - Course is not archived
     * - Current user has permission to modify the course
     * - Maximum 50 chapters per request
     */
    void bulkRestoreChapters(List<Long> ids);

    /**
     * Retrieves a chapter by ID with basic details
     * - Accessible by anyone for non-archived courses
     * - Includes basic chapter information
     */
    ChapterDTOs.ChapterResponseDto getChapterById(Long id);

    /**
     * Retrieves a chapter by ID with full details including lessons
     * - Accessible by anyone for non-archived courses
     * - Includes detailed chapter information and all lessons
     */
    ChapterDTOs.ChapterDetailResponseDto getChapterWithLessons(Long id);

    /**
     * Gets all chapters for a course
     * - Returns chapters ordered by their order number
     * - Excludes soft-deleted chapters
     * - Includes basic chapter information
     */
    List<ChapterDTOs.ChapterResponseDto> getAllChaptersByCourseId(Long courseId);

    /**
     * Searches chapters with filtering and pagination
     * Supports filtering by:
     * - Title (case-insensitive partial match)
     * - Status
     * - Course ID
     * - Creation date range (from/to)
     * Excludes soft-deleted chapters unless explicitly requested
     */
    Page<ChapterDTOs.ChapterResponseDto> searchChapters(
            ChapterDTOs.ChapterSearchDTO request,
            Pageable pageable
    );

    /**
     * Reorders chapters within a course
     * Validates:
     * - All chapters exist and belong to the course
     * - Course is not archived
     * - Current user has permission to modify the course
     * Updates order numbers to maintain sequence
     */
    void reorderChapters(Long courseId);

    /**
     * Validates if a chapter exists and is accessible
     * - Returns true if chapter exists and is not soft-deleted
     * - Returns false otherwise
     */
    boolean validateChapterExists(Long id);

    /**
     * Validates if multiple chapters exist and are accessible
     * - Returns true if all chapters exist and are not soft-deleted
     * - Returns false otherwise
     */
    boolean validateChaptersExist(List<Long> ids);

    /**
     * Returns the total number of active chapters in a course
     * - Excludes soft-deleted chapters
     * - Returns 0 if course doesn't exist
     */
    long getChapterCountByCourse(Long courseId);
}