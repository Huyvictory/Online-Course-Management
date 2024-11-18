package com.online.course.management.project.utils.chapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.dto.ChapterDTOs;
import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.Lesson;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.repository.IChapterRepository;
import com.online.course.management.project.repository.ILessonRepository;
import com.online.course.management.project.utils.course.CourseServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChapterServiceUtils {

    private final IChapterRepository chapterRepository;
    private final ILessonRepository lessonRepository;
    private final CourseServiceUtils courseServiceUtils;
    private final ObjectMapper objectMapper;
    private static final int MAX_BULK_OPERATION_SIZE = 5;

    public ChapterServiceUtils(
            IChapterRepository chapterRepository,
            CourseServiceUtils courseServiceUtils,
            ObjectMapper objectMapper,
            ILessonRepository lessonRepository) {
        this.chapterRepository = chapterRepository;
        this.courseServiceUtils = courseServiceUtils;
        this.objectMapper = objectMapper;
        this.lessonRepository = lessonRepository;
    }

    /**
     * Gets a chapter by ID or throws an exception if not found
     */
    public Chapter getChapterOrThrow(Long id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Chapter not found with id: %d", id)
                ));
    }

    /**
     * Validates chapter order number within a course
     */
    public String validateChapterOrder(Long courseId, Integer order) {
        log.debug("Validating order {} for course {}",
                order, courseId);

        if (order == null || order < 1) {
            throw new InvalidRequestException("Order must be greater than 0");
        }

        if (chapterRepository.isOrderNumberChapterTaken(courseId, order)) {
            return String.format("Order number %d is already taken in this course", order);
        }

        return null;
    }

    /**
     * Validates chapter status transitions
     */
    public void validateChapterStatus(CourseStatus currentStatus, CourseStatus newStatus)
            throws JsonProcessingException {
        if (newStatus == null) {
            throw new InvalidRequestException("Chapter status cannot be null");
        }

        // If it's a new chapter (currentStatus is null), only allow DRAFT
        if (currentStatus == null && newStatus != CourseStatus.DRAFT) {
            throw new InvalidRequestException(
                    String.format("New chapter must be created with status DRAFT, received: %s", newStatus)
            );
        }

        // Define valid transitions for each status
        Map<CourseStatus, Set<CourseStatus>> validTransitions = Map.of(
                CourseStatus.DRAFT, Set.of(CourseStatus.PUBLISHED, CourseStatus.ARCHIVED),
                CourseStatus.PUBLISHED, Set.of(CourseStatus.DRAFT, CourseStatus.ARCHIVED),
                CourseStatus.ARCHIVED, Set.of(CourseStatus.DRAFT)
        );

        // Validate the transition
        if (currentStatus != null && !validTransitions.get(currentStatus).contains(newStatus)) {
            String errorMessage = String.format("Invalid status transition: %s",
                    objectMapper.writeValueAsString(Map.of(
                            "currentStatus", currentStatus,
                            "newStatus", newStatus,
                            "allowedTransitions", validTransitions.get(currentStatus)
                    ))
            );
            throw new InvalidRequestException(errorMessage);
        }
    }

    /**
     * Validates bulk operation requirements
     */
    public void validateBulkOperation(List<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            throw new InvalidRequestException("No chapter IDs provided");
        }

        if (chapterIds.size() > MAX_BULK_OPERATION_SIZE) {
            throw new InvalidRequestException(
                    String.format("Maximum of %d chapters can be processed at once", MAX_BULK_OPERATION_SIZE)
            );
        }

        // Check for duplicates
        Set<Long> uniqueIds = new HashSet<>(chapterIds);
        if (uniqueIds.size() != chapterIds.size()) {
            throw new InvalidRequestException("Duplicate chapter IDs found");
        }

        // Validate all chapters exist
        if (!chapterRepository.validateChaptersExist(chapterIds, chapterIds.size())) {
            throw new ResourceNotFoundException("One or more chapters not found");
        }
    }

    /**
     * Validates access to a chapter based on course status and user permissions
     */
    public void validateChapterAccess(Chapter chapter) {
        Course course = chapter.getCourse();

        if (course.getStatus() == CourseStatus.ARCHIVED) {
            throw new ForbiddenException(
                    String.format("Cannot modify chapter %d - course %d is archived",
                            chapter.getId(), course.getId())
            );
        }

        courseServiceUtils.validateCourseAccess(course);
    }

    /**
     * Validates sort parameters for chapter queries
     */
    public void validateSortFields(Map<String, String> sort) {
        Set<String> validFields = Set.of(
                "title",
                "order_number",
                "status",
                "created_at",
                "updated_at"
        );

        Set<String> invalidFields = sort.keySet().stream()
                .filter(field -> !validFields.contains(field))
                .collect(Collectors.toSet());

        if (!invalidFields.isEmpty()) {
            throw new InvalidRequestException(
                    String.format("Invalid sort fields: %s. Valid fields are: %s",
                            String.join(", ", invalidFields),
                            String.join(", ", validFields))
            );
        }

        // Validate sort directions
        sort.values().forEach(direction -> {
            if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
                throw new InvalidRequestException(
                        String.format("Invalid sort direction: %s. Must be 'asc' or 'desc'", direction)
                );
            }
        });
    }

    /**
     * Creates a Sort object for chapter queries with default sorting
     */
    public Sort createChapterSort(Map<String, String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "order_number");
        }

        List<Sort.Order> orders = sortParams.entrySet().stream()
                .map(entry -> {
                    Sort.Direction direction = entry.getValue().equalsIgnoreCase("asc") ?
                            Sort.Direction.ASC : Sort.Direction.DESC;
                    return JpaSort.unsafe(direction, entry.getKey()).getOrderFor(entry.getKey());
                })
                .collect(Collectors.toList());

        return Sort.by(orders);
    }

    public void validateBulkCreateRequest(ChapterDTOs.BulkCreateChapterDTO request) {
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
            var takenOrder = validateChapterOrder(courseId, chapter.getOrder());
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

    public void validateLessons(List<LessonDTOs.CreateLessonDTO> lessons) {
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
}