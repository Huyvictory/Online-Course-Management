package com.online.course.management.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.aspect.RoleAuthorizationAspect;
import com.online.course.management.project.config.SecurityTestConfig;
import com.online.course.management.project.dto.UserLessonProgressDtos;
import com.online.course.management.project.entity.*;
import com.online.course.management.project.enums.*;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.filter.JwtAuthenticationFilter;
import com.online.course.management.project.security.annotation.WithMockCustomUser;
import com.online.course.management.project.service.interfaces.IUserLessonProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserLessonProgressController.class)
@Import({SecurityTestConfig.class, RoleAuthorizationAspect.class})
@AutoConfigureMockMvc(addFilters = false)
class UserLessonProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUserLessonProgressService userLessonProgressService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private UserLessonProgressDtos.UpdateStatusLessonProgressDTO updateStatusRequest;
    private UserLessonProgressDtos.LessonProgressResponseDTO progressResponse;
    private User testUser;
    private Course testCourse;
    private Chapter testChapter;
    private Lesson testLesson;
    private UserLessonProgress testProgress;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setStatus(UserStatus.ACTIVE);

        // Setup test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("Test Course");
        testCourse.setStatus(CourseStatus.PUBLISHED);
        testCourse.setInstructor(testUser);

        // Setup test chapter
        testChapter = new Chapter();
        testChapter.setId(1L);
        testChapter.setCourse(testCourse);
        testChapter.setTitle("Test Chapter");
        testChapter.setOrder(1);
        testChapter.setStatus(CourseStatus.PUBLISHED);

        // Setup test lesson
        testLesson = new Lesson();
        testLesson.setId(1L);
        testLesson.setChapter(testChapter);
        testLesson.setTitle("Test Lesson");
        testLesson.setContent("Test Content");
        testLesson.setType(LessonType.VIDEO);
        testLesson.setOrder(1);
        testLesson.setStatus(CourseStatus.PUBLISHED);

        // Setup test progress
        testProgress = new UserLessonProgress();
        testProgress.setId(1L);
        testProgress.setUser(testUser);
        testProgress.setCourse(testCourse);
        testProgress.setChapter(testChapter);
        testProgress.setLesson(testLesson);
        testProgress.setStatus(ProgressStatus.NOT_STARTED);
        testProgress.setLastAccessedAt(LocalDateTime.now());

        // Setup request DTO
        updateStatusRequest = new UserLessonProgressDtos.UpdateStatusLessonProgressDTO();
        updateStatusRequest.setId(1L);

        // Setup response DTO
        progressResponse = new UserLessonProgressDtos.LessonProgressResponseDTO();
        progressResponse.setId(1);
        progressResponse.setCourseId(1);
        progressResponse.setChapterId(1);
        progressResponse.setLessonId(1);
        progressResponse.setStatus(ProgressStatus.IN_PROGRESS.name());
        progressResponse.setLastAccessedAt(LocalDateTime.now().toString());
    }

    @Test
    @WithMockCustomUser
    void startLearningLesson_Success() throws Exception {
        when(userLessonProgressService.startLearningLesson(any()))
                .thenReturn(progressResponse);

        mockMvc.perform(post("/api/v1/user-lesson-progress/start-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(progressResponse.getId()))
                .andExpect(jsonPath("$.status").value(progressResponse.getStatus()));

        verify(userLessonProgressService).startLearningLesson(any());
    }

    @Test
    @WithMockCustomUser
    void startLearningLesson_AlreadyStarted() throws Exception {
        when(userLessonProgressService.startLearningLesson(any()))
                .thenThrow(new InvalidRequestException("Lesson is already started"));

        mockMvc.perform(post("/api/v1/user-lesson-progress/start-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lesson is already started"));

        verify(userLessonProgressService).startLearningLesson(any());
    }

    @Test
    @WithMockCustomUser
    void startLearningLesson_LessonNotFound() throws Exception {
        when(userLessonProgressService.startLearningLesson(any()))
                .thenThrow(new ResourceNotFoundException("Lesson progress not found"));

        mockMvc.perform(post("/api/v1/user-lesson-progress/start-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lesson progress not found"));
    }

    @Test
    @WithMockCustomUser
    void completeLearningLesson_Success() throws Exception {
        progressResponse.setStatus(ProgressStatus.COMPLETED.name());
        progressResponse.setCompletionDate(LocalDateTime.now().toString());

        when(userLessonProgressService.completeLearningLesson(any()))
                .thenReturn(progressResponse);

        mockMvc.perform(post("/api/v1/user-lesson-progress/complete-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(progressResponse.getId()))
                .andExpect(jsonPath("$.status").value(progressResponse.getStatus()))
                .andExpect(jsonPath("$.completionDate").exists());

        verify(userLessonProgressService).completeLearningLesson(any());
    }

    @Test
    @WithMockCustomUser
    void completeLearningLesson_NotStarted() throws Exception {
        when(userLessonProgressService.completeLearningLesson(any()))
                .thenThrow(new InvalidRequestException("You must start the lesson before completing it"));

        mockMvc.perform(post("/api/v1/user-lesson-progress/complete-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You must start the lesson before completing it"));
    }

    @Test
    @WithMockCustomUser
    void completeLearningLesson_AlreadyCompleted() throws Exception {
        when(userLessonProgressService.completeLearningLesson(any()))
                .thenThrow(new InvalidRequestException("Lesson is already completed"));

        mockMvc.perform(post("/api/v1/user-lesson-progress/complete-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lesson is already completed"));
    }

    @Test
    @WithMockCustomUser
    void completeLearningLesson_NoPermission() throws Exception {
        when(userLessonProgressService.completeLearningLesson(any()))
                .thenThrow(new ForbiddenException("You don't have permission to complete this lesson"));

        mockMvc.perform(post("/api/v1/user-lesson-progress/complete-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You don't have permission to complete this lesson"));
    }

    @Test
    void startLearningLesson_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/user-lesson-progress/start-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(userLessonProgressService, never()).startLearningLesson(any());
    }

    @Test
    void completeLearningLesson_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/user-lesson-progress/complete-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(userLessonProgressService, never()).completeLearningLesson(any());
    }

    @Test
    @WithMockCustomUser
    void startLearningLesson_InvalidId() throws Exception {
        updateStatusRequest.setId(null);

        mockMvc.perform(post("/api/v1/user-lesson-progress/start-learning")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStatusRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("id: Lesson progress ID is required"));

        verify(userLessonProgressService, never()).startLearningLesson(any());
    }
}