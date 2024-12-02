package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.CourseRatingDTOs;
import org.springframework.data.domain.Page;

public interface ICourseRatingService {

    CourseRatingDTOs.CourseRatingResponseDTO createCourseRating(CourseRatingDTOs.CourseRatingCreateDTO request);

    CourseRatingDTOs.CourseRatingResponseDTO updateCourseRating(CourseRatingDTOs.CourseRatingUpdateDTO request);

    Page<CourseRatingDTOs.CourseRatingResponseDTO> getCourseRatings(CourseRatingDTOs.CourseRatingSearchDTO request);

    void deleteCourseRating(Long id);

    CourseRatingDTOs.RatingDistributionDTO getCourseRatingDistribution(Long id);
}
