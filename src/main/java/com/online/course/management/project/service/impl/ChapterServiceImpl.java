package com.online.course.management.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.online.course.management.project.dto.ChapterDTOs;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.Lesson;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.mapper.ChapterMapper;
import com.online.course.management.project.repository.IChapterRepository;
import com.online.course.management.project.service.interfaces.IChapterService;
import com.online.course.management.project.utils.chapter.ChapterServiceUtils;
import com.online.course.management.project.utils.course.CourseServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChapterServiceImpl implements IChapterService {

    private final IChapterRepository chapterRepository;
    private final ChapterMapper chapterMapper;
    private final ChapterServiceUtils chapterServiceUtils;
    private final CourseServiceUtils courseServiceUtils;

    @Autowired
    public ChapterServiceImpl
            (IChapterRepository chapterRepository,
             ChapterMapper chapterMapper,
             ChapterServiceUtils chapterServiceUtils,
             CourseServiceUtils courseServiceUtils) {
        this.chapterRepository = chapterRepository;
        this.chapterMapper = chapterMapper;
        this.chapterServiceUtils = chapterServiceUtils;
        this.courseServiceUtils = courseServiceUtils;
    }

    @Override
    @Transactional
    public ChapterDTOs.ChapterDetailResponseDto createChapter(ChapterDTOs.CreateChapterDTO request) {
        log.info("Creating new chapter for course ID: {}", request.getCourseId());

        // Get and validate course
        Course course = courseServiceUtils.getCourseWithValidation(request.getCourseId());

        // Validate order number
        validateChapterOrder(course.getId(), request.getOrder());

        // Create and save chapter first
        Chapter chapter = chapterServiceUtils.createChapterWithLessons(request, course);
        chapter.setCourse(course);
        chapter.setStatus(CourseStatus.DRAFT);

        // Handle lessons if provided
        if (request.getLessons() != null && !request.getLessons().isEmpty()) {
            chapterServiceUtils.validateBulkLessonsOrders(chapter.getId(), chapter.getLessons());
        }

        Chapter savedChapter = chapterRepository.save(chapter);

        return chapterMapper.toDetailDto(savedChapter);
    }

    @Override
    @Transactional
    public List<ChapterDTOs.ChapterDetailResponseDto> bulkCreateChapters(ChapterDTOs.BulkCreateChapterDTO request) {
        log.info("Bulk creating chapters for course ID: {}", request.getChapters().get(0).getCourseId());

        // Validate request
        chapterServiceUtils.validateBulkCreateRequest(request);

        // Get and validate course
        Course course = courseServiceUtils.getCourseWithValidation(request.getChapters().get(0).getCourseId());

        // Validate all chapter orders
        chapterServiceUtils.validateBulkChapterOrders(course.getId(), request.getChapters());

        // Create and save chapters with their lessons
        List<Chapter> chapters = new ArrayList<>();
        for (ChapterDTOs.CreateChapterDTO chapterDto : request.getChapters()) {
            Chapter chapter = chapterServiceUtils.createChapterWithLessons(chapterDto, course);
            chapters.add(chapter);
        }

        // Save all chapters
        List<Chapter> savedChapters = chapterRepository.saveAll(chapters);

        log.info("Successfully created {} chapters with their lessons", savedChapters.size());

        // Map to response DTOs
        return savedChapters.stream().map(chapterMapper::toDetailDto).collect(Collectors.toList());
    }


    @Override
    @Transactional
    public ChapterDTOs.ChapterDetailResponseDto updateChapter(Long id, ChapterDTOs.UpdateChapterDTO request) {
        log.info("Updating chapter with ID: {}", id);

        // Get chapter details and validate access
        Chapter chapter = chapterServiceUtils.getChapterOrThrow(id);
        chapterServiceUtils.validateChapterAccess(chapter);

        updateBasicFields(chapter, request);

        if (request.getStatus() != null) {
            handleStatusChange(chapter, request.getStatus());
        }

        Chapter savedChapter = chapterRepository.save(chapter);
        return chapterMapper.toDetailDto(savedChapter);
    }

    private void updateBasicFields(Chapter chapter, ChapterDTOs.UpdateChapterDTO request) {
        if (request.getTitle() != null) {
            chapter.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            chapter.setDescription(request.getDescription());
        }
        if (request.getOrder() != null) {
            validateChapterOrder(chapter.getCourse().getId(), request.getOrder());
            chapter.setOrder(request.getOrder());
        }
    }

    @Transactional
    protected void handleStatusChange(Chapter chapter, CourseStatus newStatus) {
        try {
            chapterServiceUtils.validateChapterStatus(chapter.getStatus(), newStatus);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        LocalDateTime now = LocalDateTime.now();

        // Handle archival case
        if (newStatus == CourseStatus.ARCHIVED && chapter.getStatus() != CourseStatus.ARCHIVED) {
            // Set the chapter status and dates
            chapter.setStatus(newStatus);
            chapter.setDeletedAt(now);
            chapter.setUpdatedAt(now);

            // Update lessons status
            if (chapter.getLessons() != null) {
                for (Lesson lesson : chapter.getLessons()) {
                    lesson.setStatus(CourseStatus.ARCHIVED);
                    lesson.setDeletedAt(now);
                    lesson.setUpdatedAt(now);
                }
            }
        }

        // Handle restoration case
        else if (newStatus != CourseStatus.ARCHIVED && chapter.getStatus() == CourseStatus.ARCHIVED) {
            // Set the chapter status and dates
            chapter.setStatus(newStatus);
            chapter.setDeletedAt(null);
            chapter.setUpdatedAt(now);

            // Update lessons status
            if (chapter.getLessons() != null) {
                for (Lesson lesson : chapter.getLessons()) {
                    lesson.setStatus(CourseStatus.DRAFT);
                    lesson.setDeletedAt(null);
                    lesson.setUpdatedAt(now);
                }
            }
        }
        // Handle other status changes
        else {
            chapter.setStatus(newStatus);
            chapter.setUpdatedAt(now);
        }
    }

    @Override
    @Transactional
    public List<ChapterDTOs.ChapterResponseDto> bulkUpdateChapters
            (List<Long> ids, List<ChapterDTOs.UpdateChapterDTO> chapters) {
        log.info("Bulk updating {} chapters", ids.size());

        if (chapters.size() != ids.size()) {
            throw new InvalidRequestException("Number of chapter IDs and update requests must match");
        }

        chapterServiceUtils.validateBulkOperation(ids);

        List<Chapter> updatedChapters = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Chapter chapter = chapterServiceUtils.getChapterOrThrow(ids.get(i));
            chapterServiceUtils.validateChapterAccess(chapter);

            ChapterDTOs.UpdateChapterDTO updateDto = chapters.get(i);


            // Validate status if provided
            if (updateDto.getStatus() != null) {
                handleStatusChange(chapter, updateDto.getStatus());
            }

            updateBasicFields(chapter, updateDto);
            updatedChapters.add(chapter);
        }

        // Build CASE statements
        List<Chapter> savedChapters = chapterRepository.saveAll(updatedChapters);

        log.info("Successfully updated {} chapters", chapters.size());
        return savedChapters.stream().map(chapterMapper::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteChapter(Long id) {
        log.info("Soft deleting chapter with ID: {}", id);

        Chapter chapter = chapterServiceUtils.getChapterOrThrow(id);
        chapterServiceUtils.validateChapterAccess(chapter);

        if (chapter.getDeletedAt() != null) {
            throw new InvalidRequestException("Chapter is already deleted");
        }

        chapterRepository.batchSoftDeleteChapters(List.of(id));
        chapterRepository.batchSoftDeleteLessonsChapters(List.of(id));

        log.info("Chapter soft deleted successfully");
    }

    @Override
    @Transactional
    public void bulkDeleteChapters(List<Long> ids) {
        log.info("Bulk deleting {} chapters", ids.size());

        chapterServiceUtils.validateBulkOperation(ids);

        List<Chapter> chapters = chapterRepository.findAllById(ids);

        for (Chapter chapter : chapters) {
            chapterServiceUtils.validateChapterAccess(chapter);
        }

        chapterRepository.batchSoftDeleteChapters(ids);
        chapterRepository.batchSoftDeleteLessonsChapters(ids);

        log.info("Successfully deleted {} chapters", ids.size());
    }

    @Override
    @Transactional
    public void restoreChapter(Long id) {
        log.info("Restoring chapter with ID: {}", id);

        Chapter chapter = chapterServiceUtils.getChapterOrThrow(id);
        chapterServiceUtils.validateChapterAccess(chapter);

        if (chapter.getDeletedAt() == null) {
            throw new InvalidRequestException("Chapter is not deleted");
        }

        chapterRepository.batchRestoreChapters(List.of(id));
        chapterRepository.batchRestoreLessonsChapters(List.of(id));

        log.info("Chapter restored successfully");
    }

    @Override
    @Transactional
    public void bulkRestoreChapters(List<Long> ids) {
        log.info("Bulk restoring {} chapters", ids.size());

        chapterServiceUtils.validateBulkOperation(ids);

        List<Chapter> chapters = chapterRepository.findAllById(ids);

        for (Chapter chapter : chapters) {
            chapterServiceUtils.validateChapterAccess(chapter);
        }

        chapterRepository.batchRestoreChapters(ids);
        chapterRepository.batchRestoreLessonsChapters(ids);

        log.info("Successfully restored {} chapters", ids.size());
    }

    @Override
    @Cacheable(value = "chapters", key = "#id")
    public ChapterDTOs.ChapterResponseDto getChapterById(Long id) {
        log.info("Fetching chapter with ID: {}", id);
        return chapterMapper.toDto(chapterServiceUtils.getChapterOrThrow(id));
    }

    @Override
    @Cacheable(value = "chapters", key = "'detail-' + #id")
    public ChapterDTOs.ChapterDetailResponseDto getChapterWithLessons(Long id) {
        log.info("Fetching chapter details with lessons for ID: {}", id);
        return chapterMapper.toDetailDto(chapterServiceUtils.getChapterOrThrow(id));
    }

    @Override
    public List<ChapterDTOs.ChapterResponseDto> getAllChaptersByCourseId(Long courseId) {
        log.info("Fetching all chapters for course ID: {}", courseId);

        courseServiceUtils.GetCourseWithoutValidation(courseId);

        List<Chapter> chapters = chapterRepository.findAllChaptersByCourseId(courseId);
        return chapters.stream().map(chapterMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Page<ChapterDTOs.ChapterResponseDto> searchChapters(ChapterDTOs.ChapterSearchDTO request, Pageable
            pageable) {
        log.info("Searching chapters with criteria: {}", request);

        // Validate and create sort if provided

        if (request.getSort() != null) {
            chapterServiceUtils.validateSortFields(request.getSort());
        }
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), chapterServiceUtils.createChapterSort(request.getSort()));

        log.info(pageable.toString());

        Page<Chapter> chaptersPage = chapterRepository.searchChapters(request.getTitle(), request.getStatus() != null ? request.getStatus().name() : null, request.getCourseId(), request.getFromDate(), request.getToDate(), pageable);

        List<ChapterDTOs.ChapterResponseDto> chapterDtos = chaptersPage.getContent().stream().map(chapterMapper::toDto).collect(Collectors.toList());

        return new PageImpl<>(chapterDtos, pageable, chaptersPage.getTotalElements());
    }

    @Override
    @Transactional
    public void reorderChapters(Long courseId) {
        log.info("Reordering chapters for course ID: {}", courseId);
        courseServiceUtils.getCourseWithValidation(courseId);

        // Update order numbers
        chapterRepository.reorderChapters(courseId);

        log.info("Chapters reordered successfully");
    }

    private void validateChapterOrder(Long courseId, Integer order) {
        var takenChapterOrder = chapterServiceUtils.validateChapterOrder(courseId, order);
        if (takenChapterOrder != null) {
            throw new InvalidRequestException(takenChapterOrder);
        }
    }
}