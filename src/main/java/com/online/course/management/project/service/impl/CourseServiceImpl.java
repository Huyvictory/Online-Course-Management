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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseServiceImpl implements ICourseService {

    private final ICourseRepository courseRepository;
    private final IUserRepository userRepository;
    private final ICategoryRepository categoryRepository;
    private final CourseMapper courseMapper;

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
    public CourseDTOS.CourseDetailsResponseDto updateCourse(Long id, CourseDTOS.UpdateCourseRequestDTO request) {
        log.info("Updating course with id: {}", id);

        Course course = getCourseWithValidation(id);

        // Handle instructor update if requested
        if (request.getInstructorId() != null) {
            validateAdminRole();
            User newInstructor = userRepository.findById(request.getInstructorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
            validateInstructorRole(newInstructor);
            course.setInstructor(newInstructor);
        }

        // Update basic course information
        courseMapper.updateCourseFromDto(request, course);

        // Handle category updates if provided
        if (request.getCategoryIds() != null) {
            // Using the validateCategories method for consistent validation
            if (!request.getCategoryIds().isEmpty()) {
                Set<Category> validCategories = validateCategories(request.getCategoryIds());
                course.getCategories().clear();
                validCategories.forEach(course::addCategory);
            } else {
                // Clear categories if empty set provided
                course.getCategories().clear();
            }
        }

        Course updatedCourse = courseRepository.save(course);
        return courseMapper.toDto(updatedCourse);
    }

    @Override
    @Transactional
    public void archiveCourse(Long id) {
        log.info("Archiving course with id: {}", id);
        Course course = getCourseWithValidation(id);
        courseRepository.archiveCourse(id);
    }

    @Override
    @Transactional
    public void unarchiveCourse(Long id) {
        log.info("Unarchiving course with id: {}", id);
        Course course = getCourseWithValidation(id);
        courseRepository.unarchiveCourse(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseDTOS.CourseDetailsResponseDto> getCourseById(Long id) {
        return courseRepository.findByIdWithCategories(id)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTOS.CourseListResponseDto> searchCourses(CourseDTOS.SearchCourseRequestDTO request, Pageable pageable) {
        return courseRepository.searchCourses(
                request.getTitle(),
                request.getStatus(),
                request.getInstructorName(),
                request.getFromDate(),
                request.getToDate(),
                request.getCategoryIds(),
                pageable
        ).map(courseMapper::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTOS.CourseListResponseDto> getCoursesByInstructor(Long instructorId, Pageable pageable) {
        if (!userRepository.existsById(instructorId)) {
            throw new ResourceNotFoundException("Instructor not found");
        }
        return courseRepository.findByInstructorId(instructorId, pageable)
                .map(courseMapper::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseDTOS.CourseListResponseDto> getCoursesByStatus(CourseStatus status, Pageable pageable) {
        return courseRepository.findByStatus(status, pageable)
                .map(courseMapper::toListDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTOS.CourseListResponseDto> getLatestCourses(int limit) {
        return courseRepository.findLatestCourses(PageRequest.of(0, limit))
                .stream()
                .map(courseMapper::toListDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByInstructor(Long instructorId) {
        if (!userRepository.existsById(instructorId)) {
            throw new ResourceNotFoundException("Instructor not found");
        }
        return courseRepository.countByInstructorId(instructorId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(CourseStatus status) {
        return courseRepository.countCoursesByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countCoursesInCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found");
        }
        return courseRepository.countCoursesInCategory(categoryId);
    }

    @Override
    @Transactional
    public CourseDTOS.CourseDetailsResponseDto assignCategories(Long courseId, Set<Long> categoryIds) {
        log.info("Assigning categories {} to course {}", categoryIds, courseId);

        Course course = getCourseWithValidation(courseId);

        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new InvalidRequestException("Category IDs must not be empty");
        }

        Set<Long> newCategoryIds = categoryIds.stream()
                .filter(catId -> course.getCategories().stream()
                        .noneMatch(existing -> existing.getId().equals(catId)))
                .collect(Collectors.toSet());

        if (!newCategoryIds.isEmpty()) {
            validateCategories(newCategoryIds);
            courseRepository.addCourseCategories(courseId, newCategoryIds);
        }

        return courseMapper.toDto(
                courseRepository.findByIdWithCategories(courseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found"))
        );
    }

    @Override
    @Transactional
    public CourseDTOS.CourseDetailsResponseDto removeCategories(Long courseId, Set<Long> categoryIds) {
        log.info("Removing categories {} from course {}", categoryIds, courseId);

        Course course = getCourseWithValidation(courseId);

        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new InvalidRequestException("Category IDs must not be empty");
        }

        Set<Long> categoryIdsToRemove = course.getCategories().stream()
                .map(Category::getId)
                .filter(categoryIds::contains)
                .collect(Collectors.toSet());

        if (!categoryIdsToRemove.isEmpty()) {
            courseRepository.removeCourseCategories(courseId, categoryIdsToRemove);
        }

        return courseMapper.toDto(
                courseRepository.findByIdWithCategories(courseId)
                        .orElseThrow(() -> new ResourceNotFoundException("Course not found"))
        );
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
            Set<Long> foundIds = categories.stream()
                    .map(Category::getId)
                    .collect(Collectors.toSet());
            Set<Long> missingIds = categoryIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new ResourceNotFoundException("Categories not found: " + missingIds);
        }

        Set<Long> deletedCategoryIds = categories.stream()
                .filter(cat -> cat.getDeletedAt() != null)
                .map(Category::getId)
                .collect(Collectors.toSet());

        if (!deletedCategoryIds.isEmpty()) {
            throw new InvalidRequestException("Cannot use deleted categories: " + deletedCategoryIds);
        }

        return categories;
    }

    private Course getCourseWithValidation(Long id) {
        Course course = courseRepository.findByIdWithCategories(id)
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
}