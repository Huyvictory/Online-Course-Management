package com.online.course.management.project.utils.course;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.entity.Category;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.repository.ICategoryRepository;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.repository.IUserRepository;
import com.online.course.management.project.security.CustomUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CourseServiceUtils {

    private static final String DEFAULT_SORT_FIELD = "created_at";
    private final IUserRepository userRepository;
    private final ICourseRepository courseRepository;
    private final ICategoryRepository categoryRepository;

    @Autowired
    public CourseServiceUtils(IUserRepository userRepository, ICourseRepository courseRepository, ICategoryRepository categoryRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
    }

    public Sort handleCreateDefaultSort() {
        return Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD);
    }

    private void validateInstructorRole(User user) {
        boolean isInstructor = user.getUserRoles().stream()
                .anyMatch(role -> role.getRole().getName() == RoleType.INSTRUCTOR);
        if (!isInstructor) {
            throw new InvalidRequestException("User is not an instructor");
        }
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser();
    }

    public User determineInstructor(Long instructorId) {
        if (instructorId != null) {

            User instructor = userRepository.findById(instructorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
            validateInstructorRole(instructor);
            return instructor;
        }
        return getCurrentUser();
    }

    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public void validateCourseAccess(Course course) {
        if (!hasAdminRole() && !isInstructor(course)) {
            throw new ForbiddenException("You don't have permission to modify this course");
        }
    }

    private boolean isInstructor(Course course) {
        return course.getInstructor().getId().equals(getCurrentUser().getId());
    }

    public Course getCourseWithValidation(Long id) {
        Course course = courseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        validateCourseAccess(course);
        return course;
    }

    public Course GetCourseWithoutValidation(Long id) {
        return courseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    public Set<Category> validateCategories(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new InvalidRequestException("Category IDs must not be empty");
        }

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Find all categories
            Set<Category> foundCategories = new HashSet<>(categoryRepository.findAllById(categoryIds));

            // Check for non-existent categories
            if (foundCategories.size() != categoryIds.size()) {
                Set<Long> foundIds = foundCategories.stream()
                        .map(Category::getId)
                        .collect(Collectors.toSet());

                Set<Long> notFoundIds = categoryIds.stream()
                        .filter(id -> !foundIds.contains(id))
                        .collect(Collectors.toSet());

                String notFoundMessage = String.format("Categories not found: %s",
                        objectMapper.writeValueAsString(
                                notFoundIds.stream()
                                        .map(id -> Map.of(
                                                "id", id,
                                                "nameCategory", "Not Found"
                                        ))
                                        .collect(Collectors.toList())
                        )
                );

                throw new ResourceNotFoundException(notFoundMessage);
            }

            // Check for deleted categories
            Set<Category> deletedCategories = foundCategories.stream()
                    .filter(cat -> cat.getDeletedAt() != null)
                    .collect(Collectors.toSet());

            if (!deletedCategories.isEmpty()) {
                String deletedCategoriesMessage = String.format("Cannot use deleted categories: %s",
                        objectMapper.writeValueAsString(
                                deletedCategories.stream()
                                        .map(cat -> Map.of(
                                                "id", cat.getId(),
                                                "nameCategory", cat.getName()
                                        ))
                                        .collect(Collectors.toList())
                        )
                );

                throw new InvalidRequestException(deletedCategoriesMessage);
            }

            return foundCategories;

        } catch (JsonProcessingException e) {
            throw new InvalidRequestException("Error processing category validation");
        }
    }

    public void updateCourseCategories(Course course, Set<Long> categoryIds) {
        Set<Category> validCategories = validateCategories(categoryIds);
        course.getCategories().clear();
        validCategories.forEach(course::addCategory);
    }

    // Update the helper method to handle Sort conversion
    public Sort createSort(Map<String, String> sortParams) {
        log.info("Sort params received: {}", sortParams);

        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD);
        }

        // Define mapping of sort fields to actual columns
        Map<String, String> fieldMappings = Map.of(
                "title", "title",
                "created_at", "created_at",
                "updated_at", "updated_at",
                "status", "status",
                "username", "LENGTH(instructor_username)",
                "email", "LENGTH(instructor_email)"
        );

        // Create sort orders dynamically
        List<Sort.Order> orders = sortParams.entrySet().stream()
                .map(entry -> {
                    String mappedField = fieldMappings.getOrDefault(entry.getKey(), entry.getKey());
                    Sort.Direction direction = entry.getValue().equalsIgnoreCase("asc") ?
                            Sort.Direction.ASC : Sort.Direction.DESC;

                    log.info("Creating sort order - field: {}, mapped to: {}, direction: {}",
                            entry.getKey(), mappedField, direction);
                    return JpaSort.unsafe(direction, mappedField).getOrderFor(mappedField);

                })
                .collect(Collectors.toList());

        log.info("Final sort orders: {}", orders);
        return Sort.by(orders);
    }

    // Update the helper method to include all valid sort fields
    public void validateSortFields(Map<String, String> sort) {
        Set<String> validFields = Set.of(
                "title",
                "created_at",
                "updated_at",
                "status",
                "username",
                "email"
        );

        // Validate fields
        Set<String> invalidFields = sort.keySet().stream()
                .filter(field -> !validFields.contains(field))
                .collect(Collectors.toSet());

        if (!invalidFields.isEmpty()) {
            throw new InvalidRequestException(
                    "Invalid sort fields: " + String.join(", ", invalidFields) +
                            ". Valid fields are: " + String.join(", ", validFields)
            );
        }

        // Validate directions
        sort.values().forEach(direction -> {
            if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
                throw new InvalidRequestException(
                        "Invalid sort direction: " + direction + ". Must be 'asc' or 'desc'"
                );
            }
        });
    }


    public void validateCourseStatus(CourseStatus currentStatus, CourseStatus newStatus) throws JsonProcessingException {
        if (newStatus == null) {
            throw new InvalidRequestException("Course status cannot be null");
        }

        // If it's a new course (currentStatus is null), only allow DRAFT
        if (currentStatus == null && newStatus != CourseStatus.DRAFT) {
            throw new InvalidRequestException(
                    String.format("New course must be created with status DRAFT, received: %s", newStatus)
            );
        }

        // Define valid transitions for each status
        Map<CourseStatus, Set<CourseStatus>> validTransitions = Map.of(
                CourseStatus.DRAFT, Set.of(CourseStatus.PUBLISHED, CourseStatus.ARCHIVED),
                CourseStatus.PUBLISHED, Set.of(CourseStatus.DRAFT, CourseStatus.ARCHIVED),
                CourseStatus.ARCHIVED, Set.of(CourseStatus.DRAFT)
        );


        // If current status exists, validate the transition
        if (currentStatus != null && !validTransitions.get(currentStatus).contains(newStatus)) {
            try {

                CourseStatus.valueOf(newStatus.name());
                ObjectMapper objectMapper = new ObjectMapper();
                String errorMessage = String.format("Invalid status transition: %s",
                        objectMapper.writeValueAsString(Map.of(
                                "currentStatus", currentStatus,
                                "newStatus", newStatus,
                                "allowedTransitions", validTransitions.get(currentStatus)
                        ))
                );
                throw new InvalidRequestException(errorMessage);
            } catch (IllegalArgumentException e) {
                ObjectMapper objectMapper = new ObjectMapper();
                String errorMessage = String.format("Invalid course status: %s",
                        objectMapper.writeValueAsString(Map.of(
                                "providedStatus", newStatus,
                                "allowedStatuses", Arrays.stream(CourseStatus.values())
                                        .map(Enum::name)
                                        .collect(Collectors.toList())
                        ))
                );
                throw new InvalidRequestException(errorMessage);
            }
        }
    }
}
