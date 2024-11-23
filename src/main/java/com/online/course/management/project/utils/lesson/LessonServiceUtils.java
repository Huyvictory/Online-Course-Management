package com.online.course.management.project.utils.lesson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.entity.Lesson;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.repository.IChapterRepository;
import com.online.course.management.project.repository.ILessonRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LessonServiceUtils {

    private final ILessonRepository lessonRepository;
    private final IChapterRepository chapterRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_BULK_OPERATION_SIZE = 5;

    @Autowired
    public LessonServiceUtils(ILessonRepository lessonRepository, IChapterRepository chapterRepository, ObjectMapper objectMapper) {
        this.lessonRepository = lessonRepository;
        this.chapterRepository = chapterRepository;
        this.objectMapper = objectMapper;
    }

    public Lesson GetLessonOrThrow(Long id) {
        return lessonRepository.findLessonDetailsById(id).orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
    }

    public String hasLessonOrderTakenSingle(Long chapterId, Integer order) {
        if (lessonRepository.isOrderNumberLessonTaken(chapterId, order)) {
            return String.format("Order number %d is already taken in this chapter", order);
        }

        return null;
    }

    public List<String> hasLessonOrderTakenMultiple(Long chapterId, List<Lesson> lessonsList) {

        List<String> takenLessonOrders = new ArrayList<>();
        for (Lesson lesson : lessonsList) {
            var takenLessonOrder = hasLessonOrderTakenSingle(chapterId, lesson.getOrder());

            if (takenLessonOrder != null) {
                takenLessonOrders.add(takenLessonOrder);
            }
        }

        return takenLessonOrders;
    }

    public boolean IsListOrdersContainsDuplicates(List<Integer> listOrders) {
        return listOrders.stream().distinct().count() != listOrders.size();
    }

    public void validateLessonStatus(CourseStatus currentStatus, CourseStatus newStatus)
            throws JsonProcessingException {
        if (newStatus == null) {
            throw new InvalidRequestException("Lesson status cannot be null");
        }

        // Valid status transitions
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

    public void validateBulkOperation(List<Long> lessonIds) {
        if (lessonIds == null || lessonIds.isEmpty()) {
            throw new InvalidRequestException("No lessons IDs provided");
        }

        if (lessonIds.size() > MAX_BULK_OPERATION_SIZE) {
            throw new InvalidRequestException(
                    String.format("Maximum of %d lessons can be processed at once", MAX_BULK_OPERATION_SIZE)
            );
        }

        // Check for duplicates
        Set<Long> uniqueIds = new HashSet<>(lessonIds);
        if (uniqueIds.size() != lessonIds.size()) {
            throw new InvalidRequestException("Duplicate lessons IDs found");
        }

        // Validate all lessons exist
        if (!lessonRepository.validateLessonsExists(lessonIds, lessonIds.size())) {
            throw new ResourceNotFoundException("One or more lessons not found");
        }
    }


    public void validateSortFields(Map<String, String> sort) {
        Set<String> validFields = Set.of(
                "title",
                "order_number",
                "status",
                "type",
                "created_at"
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

    public Sort createLessonSortParams(Map<String, String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "created_at");
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
}
