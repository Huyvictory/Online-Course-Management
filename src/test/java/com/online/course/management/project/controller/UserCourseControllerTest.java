package com.online.course.management.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.aspect.RoleAuthorizationAspect;
import com.online.course.management.project.config.SecurityTestConfig;
import com.online.course.management.project.dto.UserCourseDTOs;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.entity.UserCourse;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.EnrollmentStatus;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.filter.JwtAuthenticationFilter;
import com.online.course.management.project.security.annotation.WithMockCustomUser;
import com.online.course.management.project.service.interfaces.IUserCourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserCourseController.class)
@Import({SecurityTestConfig.class, RoleAuthorizationAspect.class})
@AutoConfigureMockMvc(addFilters = false)
class UserCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUserCourseService userCourseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserCourseDTOs.UserCourseRequestDTO enrollmentRequest;
    private UserCourseDTOs.UserCourseResponseDto userCourseResponse;
    private UserCourseDTOs.UserCourseSearchDTO searchRequest;
    private Course testCourse;
    private User testUser;
    private UserCourse testUserCourse;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        // Setup test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("Test Course");
        testCourse.setDescription("Test Description");
        testCourse.setStatus(CourseStatus.PUBLISHED);

        // Setup test user course
        testUserCourse = new UserCourse();
        testUserCourse.setId(1L);
        testUserCourse.setUser(testUser);
        testUserCourse.setCourse(testCourse);
        testUserCourse.setStatus(EnrollmentStatus.ENROLLED);
        testUserCourse.setEnrollmentDate(LocalDateTime.now());

        // Setup enrollment request
        enrollmentRequest = new UserCourseDTOs.UserCourseRequestDTO();
        enrollmentRequest.setCourseId(1L);

        // Setup user course response
        userCourseResponse = new UserCourseDTOs.UserCourseResponseDto();
        userCourseResponse.setId(1L);
        userCourseResponse.setUserId(1L);
        userCourseResponse.setCourseId(1L);
        userCourseResponse.setCourseTitle("Test Course");
        userCourseResponse.setInstructorName("Test Instructor");
        userCourseResponse.setStatus(EnrollmentStatus.ENROLLED);
        userCourseResponse.setEnrollmentDate(LocalDateTime.now());
        userCourseResponse.setTotalLessons(10);
        userCourseResponse.setCompletedLessons(0);
        userCourseResponse.setProcessingLessons(0);
        userCourseResponse.setAverageRating(4.5);
        userCourseResponse.setCompletionRate(0.0);

        // Setup search request
        searchRequest = new UserCourseDTOs.UserCourseSearchDTO();
        searchRequest.setPage(1);
        searchRequest.setLimit(10);
    }

    @Test
    @WithMockCustomUser
    void enrollInCourse_Success() throws Exception {
        when(userCourseService.enrollInCourse(any(UserCourseDTOs.UserCourseRequestDTO.class)))
                .thenReturn(userCourseResponse);

        mockMvc.perform(post("/api/v1/user-courses/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userCourseResponse.getId()))
                .andExpect(jsonPath("$.courseTitle").value(userCourseResponse.getCourseTitle()))
                .andExpect(jsonPath("$.status").value(userCourseResponse.getStatus().name()));

        verify(userCourseService).enrollInCourse(any(UserCourseDTOs.UserCourseRequestDTO.class));
    }

    @Test
    @WithMockCustomUser
    void enrollInCourse_AlreadyEnrolled() throws Exception {
        when(userCourseService.enrollInCourse(any(UserCourseDTOs.UserCourseRequestDTO.class)))
                .thenThrow(new InvalidRequestException("User is already enrolled in this course"));

        mockMvc.perform(post("/api/v1/user-courses/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User is already enrolled in this course"));
    }

    @Test
    @WithMockCustomUser
    void getEnrollmentDetails_Success() throws Exception {
        when(userCourseService.getEnrollmentDetails(anyLong())).thenReturn(userCourseResponse);

        mockMvc.perform(post("/api/v1/user-courses/enrollment-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userCourseResponse.getId()))
                .andExpect(jsonPath("$.status").value(userCourseResponse.getStatus().name()));

        verify(userCourseService).getEnrollmentDetails(anyLong());
    }

    @Test
    @WithMockCustomUser
    void searchUserEnrollments_Success() throws Exception {
        var enrollmentsPage = new PageImpl<>(
                Collections.singletonList(userCourseResponse),
                searchRequest.toPageable(),
                1
        );

        when(userCourseService.searchUserEnrollments(any(UserCourseDTOs.UserCourseSearchDTO.class)))
                .thenReturn(enrollmentsPage);

        mockMvc.perform(post("/api/v1/user-courses/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(userCourseResponse.getId()))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(1));

        verify(userCourseService).searchUserEnrollments(any(UserCourseDTOs.UserCourseSearchDTO.class));
    }

    @Test
    @WithMockCustomUser
    void dropEnrollment_Success() throws Exception {
        doNothing().when(userCourseService).dropEnrollment(anyLong());

        mockMvc.perform(put("/api/v1/user-courses/drop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Enrollment dropped successfully"));

        verify(userCourseService).dropEnrollment(anyLong());
    }

    @Test
    @WithMockCustomUser
    void resumeEnrollment_Success() throws Exception {
        doNothing().when(userCourseService).resumeEnrollment(anyLong());

        mockMvc.perform(put("/api/v1/user-courses/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Enrollment resumed successfully"));

        verify(userCourseService).resumeEnrollment(anyLong());
    }

    @Test
    @WithMockCustomUser
    void searchUserEnrollments_WithFilters() throws Exception {
        searchRequest.setName("Test Course");
        searchRequest.setStatus(EnrollmentStatus.IN_PROGRESS);
        searchRequest.setInstructorName("Test Instructor");
        searchRequest.setMinRating(4.0);
        searchRequest.setMaxRating(5.0);
        searchRequest.setTotalLessons(10);

        var enrollmentsPage = new PageImpl<>(
                Collections.singletonList(userCourseResponse),
                searchRequest.toPageable(),
                1
        );

        when(userCourseService.searchUserEnrollments(any(UserCourseDTOs.UserCourseSearchDTO.class)))
                .thenReturn(enrollmentsPage);

        mockMvc.perform(post("/api/v1/user-courses/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(userCourseResponse.getId()));

        verify(userCourseService).searchUserEnrollments(any(UserCourseDTOs.UserCourseSearchDTO.class));
    }

    @Test
    @WithMockCustomUser
    void searchUserEnrollments_InvalidDateRange() throws Exception {
        searchRequest.setFromDate(LocalDateTime.now().plusDays(1));
        searchRequest.setToDate(LocalDateTime.now());

        mockMvc.perform(post("/api/v1/user-courses/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("dateRangeValid: toDate must be after fromDate"));

        verify(userCourseService, never()).searchUserEnrollments(any());
    }

    @Test
    @WithMockCustomUser
    void searchUserEnrollments_InvalidRatingRange() throws Exception {
        searchRequest.setMinRating(5.0);
        searchRequest.setMaxRating(4.0);

        mockMvc.perform(post("/api/v1/user-courses/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("ratingRangeValid: maxRating must be greater than minRating"));

        verify(userCourseService, never()).searchUserEnrollments(any());
    }

    @Test
    void enrollInCourse_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/user-courses/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(userCourseService, never()).enrollInCourse(any());
    }

    @Test
    @WithMockCustomUser
    void dropEnrollment_CompletedCourse() throws Exception {
        doThrow(new InvalidRequestException("Cannot drop completed course"))
                .when(userCourseService).dropEnrollment(anyLong());

        mockMvc.perform(put("/api/v1/user-courses/drop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot drop completed course"));
    }

    @Test
    @WithMockCustomUser
    void resumeEnrollment_NotDropped() throws Exception {
        doThrow(new InvalidRequestException("Cannot resume course"))
                .when(userCourseService).resumeEnrollment(anyLong());

        mockMvc.perform(put("/api/v1/user-courses/resume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enrollmentRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot resume course"));
    }
}