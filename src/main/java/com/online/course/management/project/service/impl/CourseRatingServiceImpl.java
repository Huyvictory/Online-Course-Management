package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.CourseRatingDTOs;
import com.online.course.management.project.entity.CourseRating;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.mapper.CourseRatingMapper;
import com.online.course.management.project.repository.ICourseRatingRepository;
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

    @Autowired
    public CourseRatingServiceImpl(
            ICourseRatingRepository courseRatingRepository,
            CourseRatingMapper courseRatingMapper,
            UserSecurityUtils userSecurityUtils,
            CourseRatingServiceUtils courseRatingServiceUtils) {
        this.courseRatingRepository = courseRatingRepository;
        this.courseRatingMapper = courseRatingMapper;
        this.userSecurityUtils = userSecurityUtils;
        this.courseRatingServiceUtils = courseRatingServiceUtils;
    }

    @Override
    @Transactional
    public CourseRatingDTOs.CourseRatingResponseDTO createCourseRating(CourseRatingDTOs.CourseRatingCreateDTO request) {
        log.info("Creating new course rating for course ID: {}", request.getCourseId());

        var currentUser = userSecurityUtils.getCurrentUser();

        if (courseRatingRepository.existsByUserIdAndCourseId(currentUser.getId(), request.getCourseId())) {
            throw new IllegalArgumentException("User already rated this course");
        }

        CourseRating courseRatingToCreate = courseRatingMapper.toEntity(request);

        courseRatingRepository.save(courseRatingToCreate);

        return courseRatingMapper.toDto(courseRatingToCreate);

    }

    @Override
    @Transactional
    public CourseRatingDTOs.CourseRatingResponseDTO updateCourseRating(CourseRatingDTOs.CourseRatingUpdateDTO request) {
        log.info("Updating course rating for course ID: {}", request.getCourseId());

        var currentUser = userSecurityUtils.getCurrentUser();

        if (courseRatingRepository.existsByUserIdAndCourseId(currentUser.getId(), request.getCourseId())) {
            throw new IllegalArgumentException("User already rated this course");
        }

        CourseRating courseRating = courseRatingRepository.findByUserIdAndCourseId(currentUser.getId(), request.getCourseId()).get();

        if (!currentUser.getId().equals(courseRating.getUser().getId())) {
            throw new ForbiddenException("User is not the owner of this rating");
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

        if (courseRatingRepository.existsById(id)) {
            throw new IllegalArgumentException("Rating not found");
        }

        CourseRating courseRatingToDelete = courseRatingRepository.findById(id).get();

        if (!currentUser.getId().equals(courseRatingToDelete.getUser().getId())) {
            throw new ForbiddenException("User is not the owner of this rating");
        }

        courseRatingRepository.softDeleteRating(id);

        log.info("Deleted course rating with ID: {}", id);
    }
}
