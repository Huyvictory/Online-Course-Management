package com.online.course.management.project.service.impl;

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

        User instructor = determineInstructor(request.getInstructorId());
        Course course = courseMapper.toEntity(request);
        course.setInstructor(instructor);

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            Set<Category> categories = validateCategories(request.getCategoryIds());
            categories.forEach(course::addCategory);
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
            validateAdminRole();
            User newInstructor = userRepository.findById(request.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
            validateInstructorRole(newInstructor);
            course.setInstructor(newInstructor);
        }

        courseMapper.updateCourseFromDto(request, course);

        if (request.getCategoryIds() != null) {
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

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#courseId")
    public CourseDTOS.CourseDetailsResponseDto assignCategories(Long courseId, Set<Long> categoryIds) {
        log.info("Assigning categories {} to course {}", categoryIds, courseId);

        Course course = getCourseWithValidation(courseId);
        validateCategories(categoryIds);

        courseRepository.addCourseCategory(courseId, categoryIds.iterator().next());
        return courseMapper.toDto(course);
    }

    @Override
    @Transactional
    @CacheEvict(value = "courses", key = "#courseId")
    public CourseDTOS.CourseDetailsResponseDto removeCategories(Long courseId, Set<Long> categoryIds) {
        log.info("Removing categories {} from course {}", categoryIds, courseId);

        Course course = getCourseWithValidation(courseId);
        courseRepository.removeCourseCategories(courseId, categoryIds);
        return courseMapper.toDto(course);
    }

    // Helper methods
    private User determineInstructor(Long instructorId) {
        if (instructorId != null) {
            validateAdminRole();
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

        Set<Category> categories = categoryRepository.findAllById(categoryIds)
                .stream()
                .collect(Collectors.toSet());

        if (categories.size() != categoryIds.size()) {
            throw new ResourceNotFoundException("Some categories not found");
        }

        categories.stream()
                .filter(cat -> cat.getDeletedAt() != null)
                .findAny()
                .ifPresent(cat -> {
                    throw new InvalidRequestException("Cannot use deleted category: " + cat.getId());
                });

        return categories;
    }

    private void updateCourseCategories(Course course, Set<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            course.getCategories().clear();
        } else {
            Set<Category> validCategories = validateCategories(categoryIds);
            course.getCategories().clear();
            validCategories.forEach(course::addCategory);
        }
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
}