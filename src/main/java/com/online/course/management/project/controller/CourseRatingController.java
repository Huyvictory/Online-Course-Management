package com.online.course.management.project.controller;

import com.online.course.management.project.constants.CourseRatingConstants;
import com.online.course.management.project.dto.CourseRatingDTOs;
import com.online.course.management.project.dto.PaginationDto;
import com.online.course.management.project.service.interfaces.ICourseRatingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(CourseRatingConstants.BASE_PATH)
public class CourseRatingController {

    private final ICourseRatingService courseRatingService;

    @Autowired
    public CourseRatingController(ICourseRatingService courseRatingService) {
        this.courseRatingService = courseRatingService;
    }

    @PostMapping(CourseRatingConstants.CREATE_PATH)
    public ResponseEntity<CourseRatingDTOs.CourseRatingResponseDTO> createCourseRating(@Valid @RequestBody CourseRatingDTOs.CourseRatingCreateDTO request) {

        var response = courseRatingService.createCourseRating(request);

        return ResponseEntity.ok(response);

    }

    @PutMapping(CourseRatingConstants.UPDATE_PATH)
    public ResponseEntity<CourseRatingDTOs.CourseRatingResponseDTO> updateCourseRating(@Valid @RequestBody CourseRatingDTOs.CourseRatingUpdateDTO request) {

        var response = courseRatingService.updateCourseRating(request);

        return ResponseEntity.ok(response);

    }

    @PostMapping(CourseRatingConstants.SEARCH_PATH)
    public ResponseEntity<PaginationDto.PaginationResponseDto<CourseRatingDTOs.CourseRatingResponseDTO>> searchCourseRatings(@Valid @RequestBody CourseRatingDTOs.CourseRatingSearchDTO request) {

        var response = courseRatingService.getCourseRatings(request);

        PaginationDto.PaginationResponseDto<CourseRatingDTOs.CourseRatingResponseDTO> pageResponseDto =
                new PaginationDto.PaginationResponseDto<>(
                        response.getContent(),
                        response.getNumber() + 1,
                        response.getSize(),
                        response.getTotalElements()
                );

        return ResponseEntity.ok(pageResponseDto);
    }

    @DeleteMapping(CourseRatingConstants.DELETE_PATH)
    public ResponseEntity<String> deleteCourseRating(@Valid @PathVariable long id) {

        courseRatingService.deleteCourseRating(id);

        return ResponseEntity.ok("Course rating deleted successfully");
    }

    @PostMapping(CourseRatingConstants.GET_DISTRIBUTION_PATH)
    public ResponseEntity<CourseRatingDTOs.RatingDistributionDTO> getCourseRatingDistribution(@Valid @PathVariable long id) {

        var response = courseRatingService.getCourseRatingDistribution(id);

        return ResponseEntity.ok(response);
    }


}
