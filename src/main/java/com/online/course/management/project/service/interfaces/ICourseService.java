package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ICourseService {
    CourseDTOS.CourseDetailsResponseDto createCourse(CourseDTOS.CreateCourseRequestDTO request);

    CourseDTOS.CourseDetailsResponseDto updateCourse(Long id, CourseDTOS.UpdateCourseRequestDTO request);

    void archiveCourse(Long id);

    void unarchiveCourse(Long id);

    Optional<CourseDTOS.CourseDetailsResponseDto> getCourseById(Long id);

    Page<CourseDTOS.CourseListResponseDto> searchCourses(CourseDTOS.SearchCourseRequestDTO request, Pageable pageable);

    Page<CourseDTOS.CourseListResponseDto> getCoursesByInstructor(Long instructorId, Pageable pageable);

    Page<CourseDTOS.CourseListResponseDto> getCoursesByStatus(CourseStatus status, Pageable pageable);

    List<CourseDTOS.CourseListResponseDto> getLatestCourses(int limit);

    long countByInstructor(Long instructorId);

    long countByStatus(CourseStatus status);

    long countCoursesInCategory(Long categoryId);

    CourseDTOS.CourseDetailsResponseDto assignCategories(Long courseId, Set<Long> categoryIds);

    CourseDTOS.CourseDetailsResponseDto removeCategories(Long courseId, Set<Long> categoryIds);
}