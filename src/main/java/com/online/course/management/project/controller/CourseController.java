package com.online.course.management.project.controller;

import com.online.course.management.project.constants.CourseConstants;
import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.ICourseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.function.EntityResponse;

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
}
