package com.online.course.management.project.controller;

import com.online.course.management.project.constants.CourseConstants;
import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.dto.PaginationDto;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.ICourseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(CourseConstants.BASE_PATH)
@Slf4j
public class CourseController {

    private final ICourseService courseService;

    @Autowired
    public CourseController(ICourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping(CourseConstants.CREATE_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<CourseDTOS.CourseDetailsResponseDto> createCourse(@RequestBody @Valid CourseDTOS.CreateCourseRequestDTO request) {

        CourseDTOS.CourseDetailsResponseDto createdCourse = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCourse);
    }

    @PutMapping(CourseConstants.UPDATE_PATH)
    @RequiredRole({"ADMIN", "INSTRUCTOR"})
    public ResponseEntity<CourseDTOS.CourseDetailsResponseDto> updateCourse(@PathVariable @Valid long id, @RequestBody @Valid CourseDTOS.UpdateCourseRequestDTO request) {
        CourseDTOS.CourseDetailsResponseDto updatedCourse = courseService.updateCourse(id, request);
        return ResponseEntity.ok(updatedCourse);
    }

    @PatchMapping(CourseConstants.ARCHIVE_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<String> archiveCourse(@PathVariable @Valid long id) {
        courseService.archiveCourse(id);
        return ResponseEntity.ok().body("Course archived successfully");
    }

    @PatchMapping(CourseConstants.UNARCHIVE_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<String> unarchiveCourse(@PathVariable @Valid long id) {
        courseService.unarchiveCourse(id);
        return ResponseEntity.ok().body("Course unarchived successfully");
    }

    @PostMapping(CourseConstants.PATH_VARIABLE_PATH)
    public ResponseEntity<CourseDTOS.CourseDetailsResponseDto> getCourseById(@PathVariable @Valid long id) {
        log.info("Fetching course with id: {}", id);
        CourseDTOS.CourseDetailsResponseDto response = courseService.getCourseById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping(CourseConstants.SEARCH_COURSE_INSTRUCTOR_PATH)
    public ResponseEntity<PaginationDto.PaginationResponseDto<CourseDTOS.CourseDetailsResponseDto>> searchCoursesByInstructor(
            @Valid @RequestBody CourseDTOS.SearchInstructorCourseRequestDTO searchRequest) {
        log.info("Searching courses with criteria: {}, page: {}, size: {}",
                searchRequest, searchRequest.getPage(), searchRequest.getLimit());

        var coursesPage = courseService.getCoursesByInstructor(searchRequest.getInstructorId(), searchRequest.isIncludeArchived(), searchRequest.toPageable());

        var response = new PaginationDto.PaginationResponseDto<>(
                coursesPage.getContent(),
                coursesPage.getNumber() + 1,
                coursesPage.getSize(),
                coursesPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(CourseConstants.SEARCH_COURSE_STATUS_PATH)
    public ResponseEntity<PaginationDto.PaginationResponseDto<CourseDTOS.CourseDetailsResponseDto>> searchCoursesByStatus(
            @Valid @RequestBody CourseDTOS.SearchStatusRequestDTO searchRequest) {
        log.info("Searching courses with status: {}, page: {}, size: {}",
                searchRequest, searchRequest.getPage(), searchRequest.getLimit());

        var coursesPage = courseService.getCoursesByStatus(CourseStatus.valueOf(searchRequest.getStatus()), searchRequest.toPageable());

        var response = new PaginationDto.PaginationResponseDto<>(
                coursesPage.getContent(),
                coursesPage.getNumber() + 1,
                coursesPage.getSize(),
                coursesPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping(CourseConstants.SEARCH_LATEST_COURSES_PATH)
    public ResponseEntity<List<CourseDTOS.CourseDetailsResponseDto>> searchLatestCourses(
            @Valid @RequestBody CourseDTOS.SearchLatestCoursesRequestDTO searchRequest) {
        log.info("Searching latest courses with limit: {}", searchRequest.getLimit());

        var listLatestCourses = courseService.getLatestCourses(searchRequest.getLimit());

        return ResponseEntity.ok(listLatestCourses);
    }
}
