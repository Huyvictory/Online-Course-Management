package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ICourseService {
    /**
     * Creates a new course
     */
    CourseDTOS.CourseDetailsResponseDto createCourse(CourseDTOS.CreateCourseRequestDTO request);

    /**
     * Updates an existing course
     */
    CourseDTOS.CourseDetailsResponseDto updateCourse(Long id, CourseDTOS.UpdateCourseRequestDTO request);

    /**
     * Archives a course (soft delete)
     */
    void archiveCourse(Long id);

    /**
     * Restores an archived course
     */
    void unarchiveCourse(Long id);

    /**
     * Gets course details by ID
     */
    Optional<CourseDTOS.CourseDetailsResponseDto> getCourseById(Long id);

    /**
     * Searches courses with filters and pagination
     */
    Page<CourseDTOS.CourseDetailsResponseDto> searchCourses(CourseDTOS.SearchCourseRequestDTO request, Pageable pageable);

    /**
     * Gets courses by instructor with pagination
     */
    Page<CourseDTOS.CourseDetailsResponseDto> getCoursesByInstructor(Long instructorId, boolean includeArchived, Pageable pageable);

    /**
     * Gets courses by status with pagination
     */
    Page<CourseDTOS.CourseDetailsResponseDto> getCoursesByStatus(CourseStatus status, Pageable pageable);

    /**
     * Gets the latest courses
     */
    List<CourseDTOS.CourseDetailsResponseDto> getLatestCourses(int limit);

    /**
     * Count courses by instructor
     */
    long countByInstructor(Long instructorId);

    /**
     * Count courses by status
     */
    long countByStatus(CourseStatus status);

    /**
     * Count courses in a category
     */
    long countCoursesInCategory(Long categoryId);
}