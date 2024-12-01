package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.UserCourseDTOs;
import org.springframework.data.domain.Page;

public interface IUserCourseService {

    UserCourseDTOs.UserCourseResponseDto enrollInCourse(UserCourseDTOs.EnrollCourseRequestDto request);

    UserCourseDTOs.UserCourseResponseDto getEnrollmentDetails(Long userId, Long courseId);

    Page<UserCourseDTOs.UserCourseResponseDto> searchUserEnrollments(
            Long userId,
            UserCourseDTOs.UserCourseSearchDTO searchRequest

    );

    void dropEnrollment(Long userId, Long courseId);

    void resumeEnrollment(Long userId, Long courseId);
}
