package com.online.course.management.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.aspect.RoleAuthorizationAspect;
import com.online.course.management.project.config.SecurityTestConfig;
import com.online.course.management.project.dto.CourseRatingDTOs;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.filter.JwtAuthenticationFilter;
import com.online.course.management.project.security.annotation.WithMockCustomUser;
import com.online.course.management.project.service.interfaces.ICourseRatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseRatingController.class)
@Import({SecurityTestConfig.class, RoleAuthorizationAspect.class})
@AutoConfigureMockMvc(addFilters = false)
class CourseRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICourseRatingService courseRatingService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private CourseRatingDTOs.CourseRatingResponseDTO testRatingResponse;
    private CourseRatingDTOs.CourseRatingCreateDTO createRatingRequest;
    private CourseRatingDTOs.CourseRatingUpdateDTO updateRatingRequest;
    private CourseRatingDTOs.RatingDistributionDTO distributionResponse;

    @BeforeEach
    void setUp() {
        // Setup test rating response
        testRatingResponse = new CourseRatingDTOs.CourseRatingResponseDTO();
        testRatingResponse.setId(1L);
        testRatingResponse.setUserId(1L);
        testRatingResponse.setReviewerName("Test User");
        testRatingResponse.setCourseId(1L);
        testRatingResponse.setRating(5);
        testRatingResponse.setReviewText("Great course!");
        testRatingResponse.setCreatedAt(LocalDateTime.now());
        testRatingResponse.setUpdatedAt(LocalDateTime.now());

        // Setup create rating request
        createRatingRequest = new CourseRatingDTOs.CourseRatingCreateDTO();
        createRatingRequest.setCourseId(1L);
        createRatingRequest.setRating(5);
        createRatingRequest.setReviewText("Great course!");

        // Setup update rating request
        updateRatingRequest = new CourseRatingDTOs.CourseRatingUpdateDTO();
        updateRatingRequest.setId(1L);
        updateRatingRequest.setCourseId(1L);
        updateRatingRequest.setRating(4);
        updateRatingRequest.setReviewText("Updated review");

        // Setup distribution response
        Map<Long, Long> distribution = new HashMap<>();
        distribution.put(5L, 10L);
        distribution.put(4L, 5L);

        Map<Long, Double> percentages = new HashMap<>();
        percentages.put(5L, 66.7);
        percentages.put(4L, 33.3);

        distributionResponse = new CourseRatingDTOs.RatingDistributionDTO(distribution, percentages);
    }

    @Test
    @WithMockCustomUser()
    void createCourseRating_Success() throws Exception {
        when(courseRatingService.createCourseRating(any())).thenReturn(testRatingResponse);

        mockMvc.perform(post("/api/v1/course-ratings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRatingRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testRatingResponse.getId()))
                .andExpect(jsonPath("$.rating").value(testRatingResponse.getRating()))
                .andExpect(jsonPath("$.reviewText").value(testRatingResponse.getReviewText()));

        verify(courseRatingService).createCourseRating(any());
    }

    @Test
    @WithMockCustomUser()
    void createCourseRating_AlreadyExists() throws Exception {
        when(courseRatingService.createCourseRating(any()))
                .thenThrow(new IllegalArgumentException("User already rated this course"));

        mockMvc.perform(post("/api/v1/course-ratings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRatingRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User already rated this course"));
    }

    @Test
    @WithMockCustomUser()
    void updateCourseRating_Success() throws Exception {
        when(courseRatingService.updateCourseRating(any())).thenReturn(testRatingResponse);

        mockMvc.perform(put("/api/v1/course-ratings/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRatingRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testRatingResponse.getId()))
                .andExpect(jsonPath("$.rating").value(testRatingResponse.getRating()));

        verify(courseRatingService).updateCourseRating(any());
    }

    @Test
    @WithMockCustomUser()
    void updateCourseRating_NotFound() throws Exception {
        when(courseRatingService.updateCourseRating(any()))
                .thenThrow(new ResourceNotFoundException("Course rating not found"));

        mockMvc.perform(put("/api/v1/course-ratings/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRatingRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course rating not found"));
    }

    @Test
    @WithMockCustomUser()
    void searchCourseRatings_Success() throws Exception {
        CourseRatingDTOs.CourseRatingSearchDTO searchRequest = new CourseRatingDTOs.CourseRatingSearchDTO();
        searchRequest.setCourseId(1L);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        List<CourseRatingDTOs.CourseRatingResponseDTO> ratings = Collections.singletonList(testRatingResponse);
        var ratingsPage = new PageImpl<>(ratings, PageRequest.of(0, 10), ratings.size());

        when(courseRatingService.getCourseRatings(any())).thenReturn(ratingsPage);

        mockMvc.perform(post("/api/v1/course-ratings/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(1));

        verify(courseRatingService).getCourseRatings(any());
    }

    @Test
    @WithMockCustomUser()
    void deleteCourseRating_Success() throws Exception {
        doNothing().when(courseRatingService).deleteCourseRating(anyLong());

        mockMvc.perform(delete("/api/v1/course-ratings/delete/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Course rating deleted successfully"));

        verify(courseRatingService).deleteCourseRating(1L);
    }

    @Test
    @WithMockCustomUser()
    void deleteCourseRating_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Rating not found"))
                .when(courseRatingService).deleteCourseRating(anyLong());

        mockMvc.perform(delete("/api/v1/course-ratings/delete/999"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Rating not found"));
    }

    @Test
    @WithMockCustomUser()
    void deleteCourseRating_Unauthorized() throws Exception {
        doThrow(new ForbiddenException("User is not the owner of this rating"))
                .when(courseRatingService).deleteCourseRating(anyLong());

        mockMvc.perform(delete("/api/v1/course-ratings/delete/1"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not the owner of this rating"));
    }

    @Test
    @WithMockCustomUser()
    void getRatingDistribution_Success() throws Exception {
        when(courseRatingService.getCourseRatingDistribution(anyLong())).thenReturn(distributionResponse);

        mockMvc.perform(post("/api/v1/course-ratings/get-rating-distribution/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distribution").exists())
                .andExpect(jsonPath("$.percentages").exists());

        verify(courseRatingService).getCourseRatingDistribution(1L);
    }

    @Test
    void searchCourseRatings_InvalidRequest() throws Exception {
        CourseRatingDTOs.CourseRatingSearchDTO request = new CourseRatingDTOs.CourseRatingSearchDTO();
        request.setPage(1);

        mockMvc.perform(post("/api/v1/course-ratings/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("courseId: Course ID is required"));

        verify(courseRatingService, never()).getCourseRatings(any());
    }

    @Test
    @WithMockCustomUser()
    void createCourseRating_InvalidRating() throws Exception {
        createRatingRequest.setRating(6); // Invalid rating > 5

        mockMvc.perform(post("/api/v1/course-ratings/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRatingRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("rating: Rating must be between 1 and 5"));

        verify(courseRatingService, never()).createCourseRating(any());
    }
}