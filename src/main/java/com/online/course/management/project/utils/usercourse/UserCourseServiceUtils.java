package com.online.course.management.project.utils.usercourse;

import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.repository.IUserCourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserCourseServiceUtils {

    private final IUserCourseRepository userCourseRepository;

    @Autowired
    public UserCourseServiceUtils(IUserCourseRepository userCourseRepository) {
        this.userCourseRepository = userCourseRepository;
    }

    public void validateSortFields(Map<String, String> sort) {
        Set<String> validFields = Set.of(
                "status",
                "instructor_name",
                "course_title",
                "enrollment_date",
                "completion_date",
                "averageRating"
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

    public Sort createUserCourseSort(Map<String, String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "enrollment_date");
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

    public void validateEnrollment(Long userId, Long courseId) {
        // Perform all validations in one query
        String status = userCourseRepository.validateEnrollmentEligibility(userId, courseId);
        switch (status) {
            case "ALREADY_ENROLLED" -> throw new InvalidRequestException("User is already enrolled in this course");
            case "NO_LESSONS" -> throw new InvalidRequestException("Course has no lessons");
            case "INVALID_COURSE" -> throw new ResourceNotFoundException("Course not found or is not available");
            case "DRAFT_COURSE" -> throw new InvalidRequestException("Could not enroll in a draft course");
        }
    }
}
