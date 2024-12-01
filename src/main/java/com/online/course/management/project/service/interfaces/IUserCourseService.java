package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.UserCourseDTOs;
import org.springframework.data.domain.Page;

public interface IUserCourseService {

    UserCourseDTOs.UserCourseResponseDto enrollInCourse(UserCourseDTOs.UserCourseRequestDTO request);

    UserCourseDTOs.UserCourseResponseDto getEnrollmentDetails(Long courseId);

    Page<UserCourseDTOs.UserCourseResponseDto> searchUserEnrollments(
            UserCourseDTOs.UserCourseSearchDTO searchRequest
    );

    void dropEnrollment(Long courseId);

    void resumeEnrollment(Long courseId);
}
