package com.online.course.management.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.online.course.management.project.dto.ChapterDTOs;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.mapper.ChapterMapper;
import com.online.course.management.project.repository.IChapterRepository;
import com.online.course.management.project.service.interfaces.IChapterService;
import com.online.course.management.project.utils.chapter.ChapterServiceUtils;
import com.online.course.management.project.utils.course.CourseServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ChapterServiceImpl implements IChapterService {

    private final IChapterRepository chapterRepository;
    private final ChapterMapper chapterMapper;
    private final ChapterServiceUtils chapterServiceUtils;
    private final CourseServiceUtils courseServiceUtils;

    @Autowired
    public ChapterServiceImpl(
            IChapterRepository chapterRepository,
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
    public ChapterDTOs.ChapterResponseDto createChapter(ChapterDTOs.CreateChapterDTO request) {
        log.info("Creating new chapter for course ID: {}", request.getCourseId());

        // Get and validate course
        Course course = courseServiceUtils.GetCourseWithoutValidation(request.getCourseId());
        courseServiceUtils.validateCourseAccess(course);

        // Validate order number
        var takenChapterOrder = chapterServiceUtils.validateChapterOrder(course.getId(), request.getOrder(), null);

        if (takenChapterOrder != null) {
            throw new InvalidRequestException(
                    takenChapterOrder
            );
        }

        // Create chapter
        Chapter chapter = chapterMapper.toEntity(request);
        chapter.setCourse(course);

        // Set initial status
        chapter.setStatus(CourseStatus.DRAFT);

        Chapter savedChapter = chapterRepository.save(chapter);
        log.info("Chapter created successfully with ID: {}", savedChapter.getId());

        return chapterMapper.toDto(savedChapter);
    }

    @Override
    @Transactional
    public List<ChapterDTOs.ChapterResponseDto> bulkCreateChapters(ChapterDTOs.BulkCreateChapterDTO request) {
        log.info("Bulk creating chapters for course ID: {}", request.getCourseId());

        if (request.getChapters().isEmpty()) {
            throw new InvalidRequestException("No chapters provided for creation");
        }

        if (request.getChapters().size() > 5) {
            throw new InvalidRequestException("Maximum 5 chapters can be created at once");
        }

        // Get and validate course accessibility
        Course course = courseServiceUtils.GetCourseWithoutValidation(request.getCourseId());
        courseServiceUtils.validateCourseAccess(course);

        List<Chapter> chaptersToCreate = new ArrayList<>();
        List<String> takenChapterOrders = new ArrayList<>();
        for (ChapterDTOs.CreateChapterDTO chapterDto : request.getChapters()) {
            // Validate order number for each chapter
            var takenChapterOrderString = chapterServiceUtils.validateChapterOrder(course.getId(), chapterDto.getOrder(), null);

            if (takenChapterOrderString != null) {
                takenChapterOrders.add(takenChapterOrderString);
                continue;
            }

            Chapter chapter = chapterMapper.toEntity(chapterDto);
            chapter.setCourse(course);

            chapter.setStatus(CourseStatus.DRAFT);

            chaptersToCreate.add(chapter);
        }

        if (!takenChapterOrders.isEmpty()) {
            throw new InvalidRequestException(
                    String.format("One or more chapters have the same order number: %s",
                            String.join(", ", takenChapterOrders))
            );
        }

        List<Chapter> savedChapters = chapterRepository.saveAll(chaptersToCreate);
        log.info("Successfully created {} chapters", savedChapters.size());

        return savedChapters.stream()
                .map(chapterMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "chapters", key = "#id")
    public ChapterDTOs.ChapterResponseDto updateChapter(Long id, ChapterDTOs.UpdateChapterDTO request) {
        log.info("Updating chapter with ID: {}", id);

        Chapter chapter = chapterServiceUtils.getChapterOrThrow(id);
        chapterServiceUtils.validateChapterAccess(chapter);

        // Validate order number if it's being updated
        if (request.getOrder() != null) {
            var takenChapterOrderString = chapterServiceUtils.validateChapterOrder(
                    chapter.getCourse().getId(),
                    request.getOrder(),
                    chapter.getId()
            );

            if (takenChapterOrderString != null) {
                throw new InvalidRequestException(
                        takenChapterOrderString
                );
            }
        }

        // Validate status if it's being updated
        if (request.getStatus() != null) {
            try {
                chapterServiceUtils.validateChapterStatus(chapter.getStatus(), request.getStatus());
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            chapterServiceUtils.handleArchiveStatus(chapter, request.getStatus());
        }

        chapterMapper.updateChapterFromDto(request, chapter);
        Chapter updatedChapter = chapterRepository.save(chapter);

        log.info("Chapter updated successfully");
        return chapterMapper.toDto(updatedChapter);
    }

    @Override
    @Transactional
    public List<ChapterDTOs.ChapterResponseDto> bulkUpdateChapters
            (List<Long> ids, ChapterDTOs.BulkUpdateChapterDTO request) {
        log.info("Bulk updating {} chapters", ids.size());

        chapterServiceUtils.validateBulkOperation(ids);

        if (request.getChapters().size() != ids.size()) {
            throw new InvalidRequestException("Number of chapter IDs and update requests must match");
        }

        List<Chapter> updatedChapters = new ArrayList<>();
        List<String> takenChapterOrders = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Chapter chapter = chapterServiceUtils.getChapterOrThrow(ids.get(i));
            chapterServiceUtils.validateChapterAccess(chapter);

            ChapterDTOs.UpdateChapterDTO updateDto = request.getChapters().get(i);

            // Validate order if provided
            if (updateDto.getOrder() != null) {
                var tookChapterOrderString = chapterServiceUtils.validateChapterOrder(
                        chapter.getCourse().getId(),
                        updateDto.getOrder(),
                        chapter.getId()
                );

                if (tookChapterOrderString != null) {
                    takenChapterOrders.add(tookChapterOrderString);
                    continue;
                }
            }


            // Validate status if provided
            if (updateDto.getStatus() != null) {
                try {
                    chapterServiceUtils.validateChapterStatus(chapter.getStatus(), updateDto.getStatus());
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                chapterServiceUtils.handleArchiveStatus(chapter, updateDto.getStatus());
            }

            chapterMapper.updateChapterFromDto(updateDto, chapter);
            updatedChapters.add(chapter);
        }

        if (!takenChapterOrders.isEmpty()) {
            throw new InvalidRequestException(
                    String.format("One or more chapters have the same order number: %s",
                            String.join(", ", takenChapterOrders))
            );
        }

        List<Chapter> savedChapters = chapterRepository.saveAll(updatedChapters);
        log.info("Successfully updated {} chapters", savedChapters.size());

        return savedChapters.stream()
                .map(chapterMapper::toDto)
                .collect(Collectors.toList());
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

        chapter.setDeletedAt(LocalDateTime.now());
        chapter.setStatus(CourseStatus.ARCHIVED);
        chapterRepository.save(chapter);

        log.info("Chapter soft deleted successfully");
    }

    @Override
    @Transactional
    public void bulkDeleteChapters(List<Long> ids) {
        log.info("Bulk deleting {} chapters", ids.size());

        chapterServiceUtils.validateBulkOperation(ids);
        chapterRepository.batchSoftDeleteChapters(ids);

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

        chapter.setDeletedAt(null);
        chapter.setStatus(CourseStatus.DRAFT);
        chapterRepository.save(chapter);

        log.info("Chapter restored successfully");
    }

    @Override
    @Transactional
    public void bulkRestoreChapters(List<Long> ids) {
        log.info("Bulk restoring {} chapters", ids.size());

        chapterServiceUtils.validateBulkOperation(ids);
        chapterRepository.batchRestoreChapters(ids);

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

        List<Chapter> chapters = chapterRepository.findAllChaptersByCourseId(courseId);
        return chapters.stream()
                .map(chapterMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ChapterDTOs.ChapterResponseDto> searchChapters(
            ChapterDTOs.ChapterSearchDTO request,
            Pageable pageable) {
        log.info("Searching chapters with criteria: {}", request);

        // Validate and create sort if provided
        if (request.getSort() != null && !request.getSort().isEmpty()) {
            courseServiceUtils.validateSortFields(request.getSort());
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    courseServiceUtils.createSort(request.getSort())
            );
        }

        Page<Chapter> chaptersPage = chapterRepository.searchChapters(
                request.getTitle(),
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getCourseId(),
                request.getFromDate(),
                request.getToDate(),
                pageable
        );

        List<ChapterDTOs.ChapterResponseDto> chapterDtos = chaptersPage.getContent()
                .stream()
                .map(chapterMapper::toDto)
                .collect(Collectors.toList());

        return new PageImpl<>(
                chapterDtos,
                pageable,
                chaptersPage.getTotalElements()
        );
    }

    @Override
    @Transactional
    public void reorderChapters(Long courseId, List<Long> orderedChapterIds) {
        log.info("Reordering chapters for course ID: {}", courseId);

        if (orderedChapterIds == null || orderedChapterIds.isEmpty()) {
            throw new InvalidRequestException("No chapter IDs provided for reordering");
        }

        Course course = courseServiceUtils.GetCourseWithoutValidation(courseId);
        courseServiceUtils.validateCourseAccess(course);

        // Verify all chapters exist and belong to the course
        for (Long chapterId : orderedChapterIds) {
            Chapter chapter = chapterServiceUtils.getChapterOrThrow(chapterId);
            if (!chapter.getCourse().getId().equals(courseId)) {
                throw new InvalidRequestException(
                        String.format("Chapter %d does not belong to course %d", chapterId, courseId)
                );
            }
        }

        // Update order numbers
        for (int i = 0; i < orderedChapterIds.size(); i++) {
            Chapter chapter = chapterServiceUtils.getChapterOrThrow(orderedChapterIds.get(i));
            chapter.setOrder(i + 1);
            chapterRepository.save(chapter);
        }

        log.info("Chapters reordered successfully");
    }

    @Override
    public boolean validateChapterExists(Long id) {
        return chapterRepository.existsById(id);
    }

    @Override
    public boolean validateChaptersExist(List<Long> ids) {
        return chapterRepository.validateChaptersExist(
                ids,
                ids.size()
        );
    }

    @Override
    public long getChapterCountByCourse(Long courseId) {
        // Validate course exists
        courseServiceUtils.GetCourseWithoutValidation(courseId);
        return chapterRepository.findAllChaptersByCourseId(courseId).size();
    }
}