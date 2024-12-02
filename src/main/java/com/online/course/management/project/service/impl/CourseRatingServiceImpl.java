package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.CourseRatingDTOs;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.CourseRating;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.mapper.CourseRatingMapper;
import com.online.course.management.project.repository.ICourseRatingRepository;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.service.interfaces.ICourseRatingService;
import com.online.course.management.project.utils.courserating.CourseRatingServiceUtils;
import com.online.course.management.project.utils.user.UserSecurityUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CourseRatingServiceImpl implements ICourseRatingService {

    private final ICourseRatingRepository courseRatingRepository;
    private final CourseRatingMapper courseRatingMapper;
    private final UserSecurityUtils userSecurityUtils;
    private final CourseRatingServiceUtils courseRatingServiceUtils;
    private final ICourseRepository courseRepository;

    @Autowired
    public CourseRatingServiceImpl(
            ICourseRatingRepository courseRatingRepository,
            CourseRatingMapper courseRatingMapper,
            UserSecurityUtils userSecurityUtils,
            CourseRatingServiceUtils courseRatingServiceUtils,
            ICourseRepository courseRepository) {
        this.courseRatingRepository = courseRatingRepository;
        this.courseRatingMapper = courseRatingMapper;
        this.userSecurityUtils = userSecurityUtils;
        this.courseRatingServiceUtils = courseRatingServiceUtils;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public CourseRatingDTOs.CourseRatingResponseDTO createCourseRating(CourseRatingDTOs.CourseRatingCreateDTO request) {
        log.info("Creating new course rating for course ID: {}", request.getCourseId());

        var currentUser = userSecurityUtils.getCurrentUser();

        if (courseRatingRepository.existsByUserIdAndCourseId(currentUser.getId(), request.getCourseId())) {
            throw new IllegalArgumentException("User already rated this course");
        }

        User user = userSecurityUtils.getCurrentUser();
        Course course = courseRepository.findById(request.getCourseId()).orElseThrow(() -> new IllegalArgumentException("Course not found"));

        CourseRating courseRatingToCreate = new CourseRating();
        courseRatingToCreate.setUser(user);
        courseRatingToCreate.setCourse(course);
        courseRatingToCreate.setRating(request.getRating());
        courseRatingToCreate.setReviewText(request.getReviewText());

        courseRatingRepository.save(courseRatingToCreate);

        return courseRatingMapper.toDto(courseRatingToCreate);

    }

    @Override
    @Transactional
    public CourseRatingDTOs.CourseRatingResponseDTO updateCourseRating(CourseRatingDTOs.CourseRatingUpdateDTO request) {
        log.info("Updating course rating for course ID: {}", request.getCourseId());

        var currentUser = userSecurityUtils.getCurrentUser();

        CourseRating courseRating = courseRatingRepository
                .findByUserIdAndCourseIdAndId(currentUser.getId(), request.getCourseId(), request.getId())
                .orElse(null);

        if (courseRating == null) {
            throw new ResourceNotFoundException("Course rating not found");
        }

        if (request.getRating() != null) {
            courseRating.setRating(request.getRating());
        }

        if (request.getReviewText() != null) {
            courseRating.setReviewText(request.getReviewText());
        }

        courseRatingRepository.save(courseRating);

        return courseRatingMapper.toDto(courseRating);
    }

    @Override
    @Transactional
    public Page<CourseRatingDTOs.CourseRatingResponseDTO> getCourseRatings(CourseRatingDTOs.CourseRatingSearchDTO request) {

        if (request.getSort() != null) {
            courseRatingServiceUtils.validateSortFields(request.getSort());
        }

        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getLimit(),
                courseRatingServiceUtils.createCourseRatingSort(request.getSort())
        );

        return courseRatingRepository.searchRatings(
                request.getCourseId(),
                request.getMinRating(),
                request.getMaxRating(),
                pageable
        ).map(courseRatingMapper::toDto);
    }

    @Override
    @Transactional
    public void deleteCourseRating(Long id) {
        var currentUser = userSecurityUtils.getCurrentUser();

        if (!courseRatingRepository.existsById(id)) {
            throw new IllegalArgumentException("Rating not found");
        }

        CourseRating courseRatingToDelete = courseRatingRepository.findById(id).get();

        if (!currentUser.getId().equals(courseRatingToDelete.getUser().getId())) {
            throw new ForbiddenException("User is not the owner of this rating");
        }

        if (courseRatingToDelete.getDeletedAt() != null) {
            throw new IllegalArgumentException("Rating is already deleted");
        }

        courseRatingRepository.softDeleteRating(id);

        log.info("Deleted course rating with ID: {}", id);
    }

    @Override
    @Transactional
    public CourseRatingDTOs.RatingDistributionDTO getCourseRatingDistribution(Long id) {

        var courseRatingDistribution = courseRatingServiceUtils.getRatingDistributionFormatted(id);

        var courseRatingDistributionPercentage = courseRatingServiceUtils.getRatingPercentages(id);

        return new CourseRatingDTOs.RatingDistributionDTO(
                courseRatingDistribution,
                courseRatingDistributionPercentage
        );
    }
}
