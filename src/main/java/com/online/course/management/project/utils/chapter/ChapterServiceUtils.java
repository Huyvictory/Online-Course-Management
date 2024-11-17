package com.online.course.management.project.utils.chapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.repository.IChapterRepository;
import com.online.course.management.project.utils.course.CourseServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ChapterServiceUtils {

    private final IChapterRepository chapterRepository;
    private final CourseServiceUtils courseServiceUtils;
    private final ObjectMapper objectMapper;
    private static final int MAX_BULK_OPERATION_SIZE = 5;

    public ChapterServiceUtils(
            IChapterRepository chapterRepository,
            CourseServiceUtils courseServiceUtils,
            ObjectMapper objectMapper) {
        this.chapterRepository = chapterRepository;
        this.courseServiceUtils = courseServiceUtils;
        this.objectMapper = objectMapper;
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
     * Handles archival status changes for a chapter
     */
    public void handleArchiveStatus(Chapter chapter, CourseStatus newStatus) {
        chapter.setDeletedAt(LocalDateTime.now());
    }

    /**
     * Validates sort parameters for chapter queries
     */
    public void validateSortFields(Map<String, String> sort) {
        Set<String> validFields = Set.of(
                "title",
                "order",
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
            return Sort.by(Sort.Direction.ASC, "order");
        }

        List<Sort.Order> orders = sortParams.entrySet().stream()
                .map(entry -> {
                    Sort.Direction direction = entry.getValue().equalsIgnoreCase("asc") ?
                            Sort.Direction.ASC : Sort.Direction.DESC;
                    return new Sort.Order(direction, entry.getKey());
                })
                .collect(Collectors.toList());

        return Sort.by(orders);
    }

    /**
     * Validates chapter sequence within a course
     */
    public void validateChapterSequence(Long courseId, List<Long> chapterIds) {
        // Ensure all chapters exist and belong to the course
        List<Chapter> chapters = chapterRepository.findAllById(chapterIds);

        if (chapters.size() != chapterIds.size()) {
            throw new ResourceNotFoundException("One or more chapters not found");
        }

        for (Chapter chapter : chapters) {
            if (!chapter.getCourse().getId().equals(courseId)) {
                throw new InvalidRequestException(
                        String.format("Chapter %d does not belong to course %d",
                                chapter.getId(), courseId)
                );
            }
        }

        // Validate no gaps in sequence
        Set<Integer> orders = chapters.stream()
                .map(Chapter::getOrder)
                .collect(Collectors.toSet());

        for (int i = 1; i <= chapters.size(); i++) {
            if (!orders.contains(i)) {
                throw new InvalidRequestException(
                        String.format("Gap detected in chapter sequence at position %d", i)
                );
            }
        }
    }
}