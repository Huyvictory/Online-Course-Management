package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.UserCourseDTOs;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.entity.UserCourse;
import com.online.course.management.project.enums.EnrollmentStatus;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.mapper.UserCourseMapper;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.repository.IUserCourseRepository;
import com.online.course.management.project.repository.IUserRepository;
import com.online.course.management.project.service.interfaces.IUserCourseService;
import com.online.course.management.project.utils.user.UserSecurityUtils;
import com.online.course.management.project.utils.usercourse.UserCourseServiceUtils;
import jakarta.transaction.Transactional;
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
    private final UserSecurityUtils userSecurityUtils;


    @Autowired
    public UserCourseServiceImpl(
            IUserCourseRepository userCourseRepository,
            IUserRepository IUserRepository,
            ICourseRepository courseRepository,
            UserCourseMapper userCourseMapper,
            UserCourseServiceUtils userCourseServiceUtils,
            UserSecurityUtils userSecurityUtils) {
        this.userCourseRepository = userCourseRepository;
        this.userRepository = IUserRepository;
        this.courseRepository = courseRepository;
        this.userCourseMapper = userCourseMapper;
        this.userCourseServiceUtils = userCourseServiceUtils;
        this.userSecurityUtils = userSecurityUtils;
    }


    @Override
    @Transactional
    public UserCourseDTOs.UserCourseResponseDto enrollInCourse(UserCourseDTOs.UserCourseRequestDTO request) {

        var currentUser = userSecurityUtils.getCurrentUser();

        userCourseServiceUtils.validateEnrollment(currentUser.getId(), request.getCourseId());

        Optional<User> requestUser = userRepository.findById(currentUser.getId());
        Optional<Course> requestCourse = courseRepository.findById(request.getCourseId());

        if (requestUser.isEmpty()) {
            throw new InvalidRequestException("User not found");
        }
        // Convert payload request to entity
        UserCourse convertedUserCourse = userCourseMapper.toEntity(requestUser.get(), requestCourse.get());

        // Save the entity
        UserCourse savedUserCourse = userCourseRepository.save(convertedUserCourse);

        return userCourseMapper.toDto(savedUserCourse);
    }

    @Override
    @Transactional
    public UserCourseDTOs.UserCourseResponseDto getEnrollmentDetails(Long courseId) {

        var currentUser = userSecurityUtils.getCurrentUser();

        var userCourse = userCourseRepository.findByUserIdAndCourseId(currentUser.getId(), courseId);

        if (userCourse == null) {
            throw new ResourceNotFoundException("User or course not found");
        }

        return userCourseMapper.toDto(userCourse);
    }

    @Override
    @Transactional
    public Page<UserCourseDTOs.UserCourseResponseDto> searchUserEnrollments(UserCourseDTOs.UserCourseSearchDTO request) {
        log.info("Searching chapters with criteria: {}", request);

        var currentUser = userSecurityUtils.getCurrentUser();

        // Validate and create sort if provided
        if (request.getSort() != null) {
            userCourseServiceUtils.validateSortFields(request.getSort());
        }
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit(), userCourseServiceUtils.createUserCourseSort(request.getSort()));

        log.info(pageable.toString());

        log.info("Searching user enrollments with criteria: {}", request);

        Page<UserCourse> userCoursesPage = userCourseRepository.searchUserEnrollments(
                currentUser.getId(),
                request.getName(),
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getInstructorName(),
                request.getFromDate(),
                request.getToDate(),
                request.getMinRating(),
                request.getMaxRating(),
                request.getTotalLessons(),
                pageable);

        List<UserCourseDTOs.UserCourseResponseDto> userCourseListDtos = userCoursesPage.getContent().stream().map(userCourseMapper::toDto).toList();

        log.info(userCourseListDtos.toString());

        return new PageImpl<>(userCourseListDtos, pageable, userCoursesPage.getTotalElements());
    }

    @Override
    @Transactional
    public void dropEnrollment(Long courseId) {
        var currentUser = userSecurityUtils.getCurrentUser();

        var userCourse = userCourseRepository.findByUserIdAndCourseId(currentUser.getId(), courseId);

        if (userCourse == null) {
            throw new ResourceNotFoundException("User or course not found");
        }

        if (!currentUser.getId().equals(userCourse.getUser().getId()) && !userSecurityUtils.isAdmin()) {
            throw new ForbiddenException("You don't have permission to drop this course");
        }

        if (userCourse.getStatus() == EnrollmentStatus.COMPLETED) {
            throw new InvalidRequestException("Cannot drop completed course");
        }

        if (userCourse.getStatus() == EnrollmentStatus.DROPPED) {
            throw new InvalidRequestException("Course is already dropped");
        }

        userCourseRepository.dropRelevantProgress(currentUser.getId(), courseId);

        log.info("User {} dropped from course {}", currentUser.getId(), courseId);
    }

    @Override
    @Transactional
    public void resumeEnrollment(Long courseId) {
        var currentUser = userSecurityUtils.getCurrentUser();

        var userCourse = userCourseRepository.findByUserIdAndCourseId(currentUser.getId(), courseId);

        if (userCourse == null) {
            throw new ResourceNotFoundException("User or course not found");
        }

        if (!currentUser.getId().equals(userCourse.getUser().getId()) && !userSecurityUtils.isAdmin()) {
            throw new ForbiddenException("You don't have permission to resume this course");
        }

        if (userCourse.getStatus() != EnrollmentStatus.DROPPED) {
            throw new InvalidRequestException("Cannot resume course");
        }

        userCourseRepository.resumeRelevantProgress(currentUser.getId(), courseId);

        log.info("User {} resumed from course {}", currentUser.getId(), courseId);
    }
}
