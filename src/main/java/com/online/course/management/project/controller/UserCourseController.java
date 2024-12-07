package com.online.course.management.project.controller;

import com.online.course.management.project.constants.UserCourseConstants;
import com.online.course.management.project.dto.PaginationDto;
import com.online.course.management.project.dto.UserCourseDTOs;
import com.online.course.management.project.service.interfaces.IUserCourseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(UserCourseConstants.BASE_PATH)
public class UserCourseController {

    private final IUserCourseService userCourseService;

    @Autowired
    public UserCourseController(IUserCourseService userCourseService) {
        this.userCourseService = userCourseService;
    }

    @PostMapping(UserCourseConstants.ENROLL_PATH)
    public ResponseEntity<UserCourseDTOs.UserCourseResponseDto> enrollInCourse(@Valid @RequestBody UserCourseDTOs.UserCourseRequestDTO request) {

        var registeredUserCourse = userCourseService.enrollInCourse(request);

        return ResponseEntity.ok(registeredUserCourse);
    }

    @PostMapping(UserCourseConstants.ENROLLMENT_DETAILS_PATH)
    public ResponseEntity<UserCourseDTOs.UserCourseResponseDto> getEnrollmentDetails(@Valid @RequestBody UserCourseDTOs.UserCourseRequestDTO request) {

        var enrollmentDetails = userCourseService.getEnrollmentDetails(request.getCourseId());

        return ResponseEntity.ok(enrollmentDetails);
    }

    @PostMapping(UserCourseConstants.SEARCH_PATH)
    public ResponseEntity<PaginationDto.PaginationResponseDto<UserCourseDTOs.UserCourseResponseDto>> searchUserEnrollments(@Valid @RequestBody UserCourseDTOs.UserCourseSearchDTO request) {

        var searchUserEnrollmentsPage = userCourseService.searchUserEnrollments(request);

        var response = new PaginationDto.PaginationResponseDto<>(
                searchUserEnrollmentsPage.getContent(),
                searchUserEnrollmentsPage.getNumber() + 1,
                searchUserEnrollmentsPage.getSize(),
                searchUserEnrollmentsPage.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping(UserCourseConstants.DROP_PATH)
    public ResponseEntity<String> dropEnrollment(@Valid @RequestBody UserCourseDTOs.UserCourseRequestDTO request) {
        userCourseService.dropEnrollment(request.getCourseId());
        return ResponseEntity.ok("Enrollment dropped successfully");
    }

    @PutMapping(UserCourseConstants.RESUME_PATH)
    public ResponseEntity<String> resumeEnrollment(@Valid @RequestBody UserCourseDTOs.UserCourseRequestDTO request) {
        userCourseService.resumeEnrollment(request.getCourseId());
        return ResponseEntity.ok("Enrollment resumed successfully");
    }
}
