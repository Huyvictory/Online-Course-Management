package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.UserCourseDTOs;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.entity.UserCourse;
import com.online.course.management.project.enums.EnrollmentStatus;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.mapper.UserCourseMapper;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.repository.IUserCourseRepository;
import com.online.course.management.project.repository.IUserRepository;
import com.online.course.management.project.service.interfaces.IUserCourseService;
import com.online.course.management.project.utils.usercourse.UserCourseServiceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class UserCourseServiceImpl implements IUserCourseService {

    private final IUserCourseRepository userCourseRepository;
    private final IUserRepository userRepository;
    private final ICourseRepository courseRepository;
    private final UserCourseMapper userCourseMapper;
    private final UserCourseServiceUtils userCourseServiceUtils;

    @Autowired
    public UserCourseServiceImpl(
            IUserCourseRepository userCourseRepository,
            IUserRepository IUserRepository,
            ICourseRepository courseRepository,
            UserCourseMapper userCourseMapper,
            UserCourseServiceUtils userCourseServiceUtils) {
        this.userCourseRepository = userCourseRepository;
        this.userRepository = IUserRepository;
        this.courseRepository = courseRepository;
        this.userCourseMapper = userCourseMapper;
        this.userCourseServiceUtils = userCourseServiceUtils;
    }

    @Override
    public UserCourseDTOs.UserCourseResponseDto enrollInCourse(Long userId, UserCourseDTOs.EnrollCourseRequestDto request) {
        if (userCourseRepository.existsByUserIdAndCourseId(userId, request.getCourseId())) {
            throw new InvalidRequestException("User is already enrolled in this course");
        }


        Optional<User> requestUser = userRepository.findById(userId);
        Optional<Course> requestCourse = courseRepository.findById(request.getCourseId());

        if (requestUser.isEmpty()) {
            throw new InvalidRequestException("User not found");
        }

        if (requestCourse.isEmpty()) {
            throw new InvalidRequestException("Course not found");
        }

        // Convert payload request to entity
        UserCourse convertedUserCourse = userCourseMapper.toEntity(requestUser.get(), requestCourse.get());

        // Save the entity
        UserCourse savedUserCourse = userCourseRepository.save(convertedUserCourse);

        return userCourseMapper.toDto(savedUserCourse);
    }

    @Override
    public UserCourseDTOs.UserCourseResponseDto getEnrollmentDetails(Long userId, Long courseId) {
        return userCourseMapper.toDto(userCourseRepository.findByUserIdAndCourseId(userId, courseId));
    }

    @Override
    public Page<UserCourseDTOs.UserCourseResponseDto> searchUserEnrollments(Long userId, UserCourseDTOs.UserCourseSearchDTO request) {
        log.info("Searching chapters with criteria: {}", request);

        // Validate and create sort if provided
        if (request.getSort() != null) {
            userCourseServiceUtils.validateSortFields(request.getSort());
        }
        Pageable pageable = PageRequest.of(request.getPage(), request.getLimit(), userCourseServiceUtils.createChapterSort(request.getSort()));

        log.info(pageable.toString());

        Page<UserCourse> userCoursesPage = userCourseRepository.searchUserEnrollments(
                userId,
                request.getName(),
                request.getStatus().name(),
                request.getInstructorName(),
                request.getFromDate(),
                request.getToDate(),
                request.getMinRating(),
                request.getMaxRating(),
                request.getLessonCount(),
                pageable);

        List<UserCourseDTOs.UserCourseResponseDto> userCourseListDtos = userCourseMapper.toDtoList(userCoursesPage.getContent());

        return new PageImpl<>(userCourseListDtos, pageable, userCoursesPage.getTotalElements());
    }

    @Override
    public UserCourseDTOs.UserCourseStatisticsDTO getUserCourseStatistics(Long userId, Long courseId) {

        if (!userCourseRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new ResourceNotFoundException("User or course not found");
        }

        return userCourseMapper.toStatisticsDto(userCourseRepository.findEnrollmentWithStats(userId, courseId).get());
    }

    @Override
    public void dropEnrollment(Long userId, Long courseId) {

        var userCourse = userCourseRepository.findByUserIdAndCourseId(userId, courseId);

        if (userCourse == null) {
            throw new ResourceNotFoundException("User or course not found");
        }

        if (userCourse.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new InvalidRequestException("Cannot drop completed course");
        }

        if (userCourse.getStatus() == EnrollmentStatus.DROPPED) {
            throw new InvalidRequestException("Course is already dropped");
        }

        userCourseRepository.dropEnrollment(userId, courseId);
        userCourseRepository.dropRelevantProgress(userId, courseId);

        log.info("User {} dropped from course {}", userId, courseId);
    }

    @Override
    public void resumeEnrollment(Long userId, Long courseId) {
        var userCourse = userCourseRepository.findByUserIdAndCourseId(userId, courseId);

        if (userCourse == null) {
            throw new ResourceNotFoundException("User or course not found");
        }

        if (userCourse.getStatus() != EnrollmentStatus.DROPPED) {
            throw new InvalidRequestException("Cannot resume course");
        }

        userCourseRepository.resumeEnrollment(userId, courseId);
        userCourseRepository.resumeRelevantProgress(userId, courseId);

        log.info("User {} resumed from course {}", userId, courseId);
    }
}
