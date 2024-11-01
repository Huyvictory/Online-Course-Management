package com.online.course.management.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.entity.Category;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.mapper.CourseMapper;
import com.online.course.management.project.repository.ICategoryRepository;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.repository.IUserRepository;
import com.online.course.management.project.security.CustomUserDetails;
import com.online.course.management.project.service.interfaces.ICourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseServiceImpl implements ICourseService {

    private final ICourseRepository courseRepository;
    private final IUserRepository userRepository;
    private final ICategoryRepository categoryRepository;
    private final CourseMapper courseMapper;

    private static final String DEFAULT_SORT_FIELD = "createdAt";
    private static final String DEFAULT_SORT_ORDER = "desc";

    @Autowired
    public CourseServiceImpl(
            ICourseRepository courseRepository,
            IUserRepository userRepository,
            ICategoryRepository categoryRepository,
            CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", allEntries = true)
    public CourseDTOS.CourseDetailsResponseDto createCourse(CourseDTOS.CreateCourseRequestDTO request) {
        log.info("Creating new course with title: {}", request.getTitle());

        Course course = courseMapper.toEntity(request);

        if (request.getInstructorId() != null) {
            User instructor = determineInstructor(request.getInstructorId());
            course.setInstructor(instructor);
        }

        // Important: Initialize the categories set if null
        if (course.getCategories() == null) {
            course.setCategories(new HashSet<>());
        }

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = validateCategories(request.getCategoryIds());
            for (Category category : categories) {
                course.addCategory(category);
            }
        }

        Course savedCourse = courseRepository.save(course);
        return courseMapper.toDto(savedCourse);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public CourseDTOS.CourseDetailsResponseDto updateCourse(Long id, CourseDTOS.UpdateCourseRequestDTO request) {
        log.info("Updating course with id: {}", id);

        Course course = getCourseWithValidation(id);

        if (request.getInstructorId() != null) {
            User newInstructor = determineInstructor(request.getInstructorId());
            course.setInstructor(newInstructor);
        }

        // If status is being updated, validate the transition
        if (request.getStatus() != null) {
            try {
                validateCourseStatus(course.getStatus(), request.getStatus());
            } catch (JsonProcessingException e) {
                throw new InvalidRequestException("Error processing status validation");
            }

            // Handle archived status and deletedAt
            if (request.getStatus() == CourseStatus.ARCHIVED) {
                course.setDeletedAt(LocalDateTime.now());
            } else if (course.getStatus() == CourseStatus.ARCHIVED && request.getStatus() != CourseStatus.ARCHIVED) {
                // If transitioning from ARCHIVED to another status, clear deletedAt
                course.setDeletedAt(null);
            }
        }

        courseMapper.updateCourseFromDto(request, course);

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            updateCourseCategories(course, request.getCategoryIds());
        }

        Course updatedCourse = courseRepository.save(course);
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public void archiveCourse(Long id) {
        log.info("Archiving course with id: {}", id);
        Course course = getCourseWithValidation(id);
        courseRepository.archiveCourse(id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#id")
    public void unarchiveCourse(Long id) {
        log.info("Unarchiving course with id: {}", id);
        Course course = getCourseWithValidation(id);
        courseRepository.unarchiveCourse(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "courses", key = "#id")
    public Optional<CourseDTOS.CourseDetailsResponseDto> getCourseById(Long id) {
        log.info("Fetching course with id: {}", id);
        return courseRepository.findByIdWithDetails(id)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTOS.CourseDetailsResponseDto> searchCourses(
            CourseDTOS.SearchCourseRequestDTO request,
            Pageable pageable) {
        log.info("Searching courses with criteria: {}", request);

        // Validate and create sort if provided in request
        if (request.getSort() != null && !request.getSort().isEmpty()) {
            validateSortFields(request.getSort());
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    createSort(request.getSort())
            );
        } else {
            // Use default sort if none provided
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD)
            );
        }

        return courseRepository.searchCourses(
                request.getTitle(),
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getInstructorName(),
                request.getFromDate(),
                request.getToDate(),
                request.getCategoryIds(),
                request.getIncludeArchived(),
                pageable
        ).map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTOS.CourseDetailsResponseDto> getCoursesByInstructor(
            Long instructorId,
            boolean includeArchived,
            Pageable pageable) {
        log.info("Fetching courses for instructor: {}", instructorId);

        if (!userRepository.existsById(instructorId)) {
            throw new ResourceNotFoundException("Instructor not found");
        }

        // Use default sort if none provided
        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD)
            );
        } else {
            // Validate provided sort
            Map<String, String> sortMap = new HashMap<>();
            pageable.getSort().forEach(order ->
                    sortMap.put(order.getProperty(), order.getDirection().name().toLowerCase())
            );
            validateSortFields(sortMap);
        }

        return courseRepository.findByInstructorId(instructorId, includeArchived, pageable)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTOS.CourseDetailsResponseDto> getCoursesByStatus(
            CourseStatus status,
            Pageable pageable) {
        log.info("Fetching courses with status: {}", status);

        // Use default sort if none provided
        if (!pageable.getSort().isSorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD)
            );
        } else {
            // Validate provided sort
            Map<String, String> sortMap = new HashMap<>();
            pageable.getSort().forEach(order ->
                    sortMap.put(order.getProperty(), order.getDirection().name().toLowerCase())
            );
            validateSortFields(sortMap);
        }

        return courseRepository.findByStatus(status.name(), pageable)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "latestCourses", key = "#limit")
    public List<CourseDTOS.CourseDetailsResponseDto> getLatestCourses(int limit) {
        log.info("Fetching latest {} courses", limit);
        return courseRepository.findLatestCourses(limit)
                .stream()
                .map(courseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public long countByInstructor(Long instructorId) {
        return courseRepository.countByInstructorId(instructorId);
    }

    @Override
    public long countByStatus(CourseStatus status) {
        return courseRepository.countByStatus(status.name());
    }

    @Override
    public long countCoursesInCategory(Long categoryId) {
        return courseRepository.countCoursesInCategory(categoryId);
    }

    // Helper methods
    private User determineInstructor(Long instructorId) {
        if (instructorId != null) {

            User instructor = userRepository.findById(instructorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
            validateInstructorRole(instructor);
            return instructor;
        }
        return getCurrentUser();
    }

    private Set<Category> validateCategories(Set<Long> categoryIds) {
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

    private void updateCourseCategories(Course course, Set<Long> categoryIds) {
        Set<Category> validCategories = validateCategories(categoryIds);
        course.getCategories().clear();
        validCategories.forEach(course::addCategory);

    }

    private Course getCourseWithValidation(Long id) {
        Course course = courseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        validateCourseAccess(course);
        return course;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.getUser();
    }

    private void validateCourseAccess(Course course) {
        if (!hasAdminRole() && !isInstructor(course)) {
            throw new ForbiddenException("You don't have permission to modify this course");
        }
    }

    private boolean isInstructor(Course course) {
        return course.getInstructor().getId().equals(getCurrentUser().getId());
    }

    private void validateAdminRole() {
        if (!hasAdminRole()) {
            throw new ForbiddenException("Admin role required for this operation");
        }
    }

    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void validateInstructorRole(User user) {
        boolean isInstructor = user.getUserRoles().stream()
                .anyMatch(role -> role.getRole().getName() == RoleType.INSTRUCTOR);
        if (!isInstructor) {
            throw new InvalidRequestException("User is not an instructor");
        }
    }

    // Update the helper method to handle Sort conversion
    private Sort createSort(Map<String, String> sortParams) {
        if (sortParams == null || sortParams.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, DEFAULT_SORT_FIELD);
        }

        validateSortFields(sortParams); // Validate before creating Sort

        List<Sort.Order> orders = sortParams.entrySet().stream()
                .map(entry -> new Sort.Order(
                        entry.getValue().equalsIgnoreCase("asc") ?
                                Sort.Direction.ASC : Sort.Direction.DESC,
                        entry.getKey()
                ))
                .collect(Collectors.toList());

        return Sort.by(orders);
    }

    // Update the helper method to include all valid sort fields
    private void validateSortFields(Map<String, String> sort) {
        if (sort == null) return;

        Set<String> validFields = Set.of(
                "title",
                "createdAt",
                "updatedAt",
                "status",
                "instructorName",
                "categoryCount"
        );

        Set<String> invalidFields = sort.keySet().stream()
                .filter(field -> !validFields.contains(field))
                .collect(Collectors.toSet());

        if (!invalidFields.isEmpty()) {
            throw new InvalidRequestException(
                    "Invalid sort fields: " + String.join(", ", invalidFields) +
                            ". Valid fields are: " + String.join(", ", validFields)
            );
        }

        sort.values().forEach(direction -> {
            if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
                throw new InvalidRequestException(
                        "Invalid sort direction: " + direction + ". Must be 'asc' or 'desc'"
                );
            }
        });
    }

    private void validateCourseStatus(CourseStatus currentStatus, CourseStatus newStatus) throws JsonProcessingException {
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