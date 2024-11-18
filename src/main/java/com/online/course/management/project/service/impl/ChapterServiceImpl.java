package com.online.course.management.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.online.course.management.project.dto.ChapterDTOs;
import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.Lesson;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.mapper.ChapterMapper;
import com.online.course.management.project.mapper.LessonMapper;
import com.online.course.management.project.repository.chapter.IChapterRepository;
import com.online.course.management.project.repository.lesson.LessonOperations;
import com.online.course.management.project.service.interfaces.IChapterService;
import com.online.course.management.project.utils.chapter.ChapterServiceUtils;
import com.online.course.management.project.utils.course.CourseServiceUtils;
import com.online.course.management.project.utils.lesson.LessonServiceUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final LessonOperations lessonRepository;
    private final ChapterMapper chapterMapper;
    private final LessonMapper lessonMapper;
    private final ChapterServiceUtils chapterServiceUtils;
    private final CourseServiceUtils courseServiceUtils;
    private final LessonServiceUtils lessonServiceUtils;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ChapterServiceImpl(IChapterRepository chapterRepository, LessonOperations lessonRepository, ChapterMapper chapterMapper, LessonMapper lessonMapper, ChapterServiceUtils chapterServiceUtils, CourseServiceUtils courseServiceUtils, LessonServiceUtils lessonServiceUtils) {
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
        this.chapterMapper = chapterMapper;
        this.lessonMapper = lessonMapper;
        this.chapterServiceUtils = chapterServiceUtils;
        this.courseServiceUtils = courseServiceUtils;
        this.lessonServiceUtils = lessonServiceUtils;
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
        Chapter chapter = createChapterWithLessons(request, course);
        chapter.setCourse(course);
        chapter.setStatus(CourseStatus.DRAFT);

        // Handle lessons if provided
        if (request.getLessons() != null && !request.getLessons().isEmpty()) {
            validateBulkLessonsOrders(chapter.getId(), chapter.getLessons());
        }

        Chapter savedChapter = chapterRepository.save(chapter);

        return chapterMapper.toDetailDto(savedChapter);
    }

    @Override
    @Transactional
    public List<ChapterDTOs.ChapterDetailResponseDto> bulkCreateChapters(ChapterDTOs.BulkCreateChapterDTO request) {
        log.info("Bulk creating chapters for course ID: {}", request.getChapters().get(0).getCourseId());

        // Validate request
        validateBulkCreateRequest(request);

        // Get and validate course
        Course course = courseServiceUtils.getCourseWithValidation(request.getChapters().get(0).getCourseId());

        // Validate all chapter orders
        validateBulkChapterOrders(course.getId(), request.getChapters());

        // Create and save chapters with their lessons
        List<Chapter> chapters = new ArrayList<>();
        for (ChapterDTOs.CreateChapterDTO chapterDto : request.getChapters()) {
            Chapter chapter = createChapterWithLessons(chapterDto, course);
            chapters.add(chapter);
        }

        // Save all chapters
        List<Chapter> savedChapters = chapterRepository.saveAll(chapters);

        log.info("Successfully created {} chapters with their lessons", savedChapters.size());

        // Map to response DTOs
        return savedChapters.stream().map(chapterMapper::toDetailDto).collect(Collectors.toList());
    }

    private void validateBulkCreateRequest(ChapterDTOs.BulkCreateChapterDTO request) {
        if (request.getChapters() == null || request.getChapters().isEmpty()) {
            throw new InvalidRequestException("No chapters provided for creation");
        }

        if (request.getChapters().size() > 5) {
            throw new InvalidRequestException("Maximum 5 chapters can be created at once");
        }

        // Validate chapter orders uniqueness
        Set<Integer> chapterOrders = new HashSet<>();
        for (ChapterDTOs.CreateChapterDTO chapter : request.getChapters()) {
            if (!chapterOrders.add(chapter.getOrder())) {
                throw new InvalidRequestException(String.format("Duplicate chapter order found: %d", chapter.getOrder()));
            }

            // Validate lessons if present
            if (chapter.getLessons() != null && !chapter.getLessons().isEmpty()) {
                validateLessons(chapter.getLessons());
            }
        }
    }

    public void validateBulkChapterOrders(Long courseId, List<ChapterDTOs.CreateChapterDTO> chapters) {
        List<String> conflictingOrders = new ArrayList<>();

        for (ChapterDTOs.CreateChapterDTO chapter : chapters) {
            var takenOrder = chapterServiceUtils.validateChapterOrder(courseId, chapter.getOrder());
            if (takenOrder != null) {
                conflictingOrders.add(takenOrder);
            }
        }

        if (!conflictingOrders.isEmpty()) {
            throw new InvalidRequestException("Order conflicts found: " + String.join(", ", conflictingOrders));
        }
    }

    public void validateBulkLessonsOrders(Long chapterId, List<Lesson> lessons) {
        // Validate lesson orders
        List<String> takenLessonOrders = new ArrayList<>();
        for (Lesson lesson : lessons) {
            if (lessonRepository.isOrderNumberLessonTaken(chapterId, lesson.getOrder())) {
                takenLessonOrders.add(String.format("Order number %d is already taken in this chapter", lesson.getOrder()));
            }
        }

        if (!takenLessonOrders.isEmpty()) {
            throw new InvalidRequestException(String.format("One or more lessons have duplicate order numbers: %s", String.join(", ", takenLessonOrders)));
        }
    }

    public Chapter createChapterWithLessons(ChapterDTOs.CreateChapterDTO dto, Course course) {
        Chapter chapter = new Chapter();
        chapter.setCourse(course);
        chapter.setTitle(dto.getTitle());
        chapter.setDescription(dto.getDescription());
        chapter.setOrder(dto.getOrder());
        chapter.setStatus(CourseStatus.DRAFT);

        if (dto.getLessons() != null && !dto.getLessons().isEmpty()) {
            List<Lesson> lessons = dto.getLessons().stream().map(lessonDto -> {
                Lesson lesson = new Lesson();
                lesson.setChapter(chapter);
                lesson.setTitle(lessonDto.getTitle());
                lesson.setContent(lessonDto.getContent());
                lesson.setOrder(lessonDto.getOrder()); // Maintain original order
                lesson.setType(lessonDto.getType());
                lesson.setStatus(CourseStatus.DRAFT);
                return lesson;
            }).collect(Collectors.toList());

            chapter.setLessons(lessons);
        }

        return chapter;
    }

    private void validateLessons(List<LessonDTOs.CreateLessonDTO> lessons) {
        // Validate orders
        Set<Integer> seenOrders = new HashSet<>();
        List<Integer> duplicateOrders = new ArrayList<>();

        for (LessonDTOs.CreateLessonDTO lesson : lessons) {
            // Validate order is positive
            if (lesson.getOrder() <= 0) {
                throw new InvalidRequestException(String.format("Invalid lesson order: %d. Order must be greater than 0", lesson.getOrder()));
            }

            // Check for duplicates
            if (!seenOrders.add(lesson.getOrder())) {
                duplicateOrders.add(lesson.getOrder());
            }
        }

        if (!duplicateOrders.isEmpty()) {
            throw new InvalidRequestException("Duplicate lesson orders found: " + duplicateOrders.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        }
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
        return chapters.stream().map(chapterMapper::toDto).collect(Collectors.toList());
    }

    @Override
    public Page<ChapterDTOs.ChapterResponseDto> searchChapters(ChapterDTOs.ChapterSearchDTO request, Pageable
            pageable) {
        log.info("Searching chapters with criteria: {}", request);

        // Validate and create sort if provided
        if (request.getSort() != null && !request.getSort().isEmpty()) {
            chapterServiceUtils.validateSortFields(request.getSort());
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), chapterServiceUtils.createChapterSort(request.getSort()));
        }

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

    @Override
    public boolean validateChapterExists(Long id) {
        return chapterRepository.existsById(id);
    }

    @Override
    public boolean validateChaptersExist(List<Long> ids) {
        return chapterRepository.validateChaptersExist(ids, ids.size());
    }

    @Override
    public long getChapterCountByCourse(Long courseId) {
        // Validate course exists
        courseServiceUtils.GetCourseWithoutValidation(courseId);
        return chapterRepository.findAllChaptersByCourseId(courseId).size();
    }

    private void validateChapterOrder(Long courseId, Integer order) {
        var takenChapterOrder = chapterServiceUtils.validateChapterOrder(courseId, order);
        if (takenChapterOrder != null) {
            throw new InvalidRequestException(takenChapterOrder);
        }
    }
}