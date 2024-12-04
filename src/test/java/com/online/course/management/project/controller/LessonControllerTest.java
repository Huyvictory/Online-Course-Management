package com.online.course.management.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.aspect.RoleAuthorizationAspect;
import com.online.course.management.project.config.SecurityTestConfig;
import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.Lesson;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.LessonType;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.filter.JwtAuthenticationFilter;
import com.online.course.management.project.security.annotation.WithMockCustomUser;
import com.online.course.management.project.service.interfaces.ILessonService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LessonController.class)
@Import({SecurityTestConfig.class, RoleAuthorizationAspect.class})
@AutoConfigureMockMvc(addFilters = false)
class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ILessonService lessonService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private Course testCourse;
    private Chapter testChapter;
    private Lesson testLesson;
    private LessonDTOs.CreateLessonDTO createLessonRequestWithoutChapterId;
    private LessonDTOs.LessonResponseDto testLessonResponseDto;
    private LessonDTOs.LessonDetailResponseDto testLessonDetailResponseDto;
    private LessonDTOs.CreateLessonDTOWithChapterId createLessonRequest;
    private LessonDTOs.UpdateLessonDTO updateLessonRequest;

    @BeforeEach
    void setUp() {
        // Setup instructor
        User instructor = new User();
        instructor.setId(1L);
        instructor.setUsername("instructor");
        instructor.setEmail("instructor@test.com");

        // Setup course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("Test Course");
        testCourse.setDescription("Test Course Description");
        testCourse.setInstructor(instructor);
        testCourse.setStatus(CourseStatus.PUBLISHED);
        testCourse.setCreatedAt(LocalDateTime.now());
        testCourse.setUpdatedAt(LocalDateTime.now());

        // Setup chapter
        testChapter = new Chapter();
        testChapter.setId(1L);
        testChapter.setCourse(testCourse);
        testChapter.setTitle("Test Chapter");
        testChapter.setDescription("Test Chapter Description");
        testChapter.setOrder(1);
        testChapter.setStatus(CourseStatus.DRAFT);
        testChapter.setCreatedAt(LocalDateTime.now());
        testChapter.setUpdatedAt(LocalDateTime.now());

        // Setup lesson
        testLesson = new Lesson();
        testLesson.setId(1L);
        testLesson.setChapter(testChapter);
        testLesson.setTitle("Test Lesson");
        testLesson.setContent("Test Content");
        testLesson.setOrder(1);
        testLesson.setType(LessonType.VIDEO);
        testLesson.setStatus(CourseStatus.DRAFT);
        testLesson.setCreatedAt(LocalDateTime.now());
        testLesson.setUpdatedAt(LocalDateTime.now());

        // Setup lesson response DTOs
        testLessonResponseDto = new LessonDTOs.LessonResponseDto();
        testLessonResponseDto.setId(1L);
        testLessonResponseDto.setChapterId(1L);
        testLessonResponseDto.setChapterTitle("Test Chapter");
        testLessonResponseDto.setTitle("Test Lesson");
        testLessonResponseDto.setContent("Test Content");
        testLessonResponseDto.setOrder(1);
        testLessonResponseDto.setType(LessonType.VIDEO);
        testLessonResponseDto.setStatus(CourseStatus.DRAFT);

        testLessonDetailResponseDto = new LessonDTOs.LessonDetailResponseDto();
        testLessonDetailResponseDto.setId(1L);
        testLessonDetailResponseDto.setChapterId(1L);
        testLessonDetailResponseDto.setChapterTitle("Test Chapter");
        testLessonDetailResponseDto.setCourseId(1L);
        testLessonDetailResponseDto.setCourseTitle("Test Course");
        testLessonDetailResponseDto.setTitle("Test Lesson");
        testLessonDetailResponseDto.setContent("Test Content");
        testLessonDetailResponseDto.setOrder(1);
        testLessonDetailResponseDto.setType(LessonType.VIDEO);
        testLessonDetailResponseDto.setStatus(CourseStatus.DRAFT);

        // Setup create lesson request
        createLessonRequest = new LessonDTOs.CreateLessonDTOWithChapterId();
        createLessonRequest.setChapterId(1L);
        createLessonRequest.setTitle("New Lesson");
        createLessonRequest.setContent("New Content");
        createLessonRequest.setOrder(1);
        createLessonRequest.setType(LessonType.VIDEO);

        // Setup create lesson request without chapter ID
        createLessonRequestWithoutChapterId = new LessonDTOs.CreateLessonDTO();
        createLessonRequestWithoutChapterId.setTitle("New Lesson");
        createLessonRequestWithoutChapterId.setContent("New Content");
        createLessonRequestWithoutChapterId.setOrder(1);
        createLessonRequestWithoutChapterId.setType(LessonType.VIDEO);

        // Setup update lesson request
        updateLessonRequest = new LessonDTOs.UpdateLessonDTO();
        updateLessonRequest.setTitle("Updated Lesson");
        updateLessonRequest.setContent("Updated Content");
        updateLessonRequest.setOrder(2);
        updateLessonRequest.setType(LessonType.TEXT);
        updateLessonRequest.setStatus(CourseStatus.PUBLISHED);
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void createLesson_Success() throws Exception {
        when(lessonService.createLesson(any(LessonDTOs.CreateLessonDTOWithChapterId.class)))
                .thenReturn(testLessonResponseDto);

        mockMvc.perform(post("/api/v1/lessons/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLessonRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testLessonResponseDto.getId()))
                .andExpect(jsonPath("$.title").value(testLessonResponseDto.getTitle()));

        verify(lessonService).createLesson(any());
    }

    @Test
    void createLesson_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/lessons/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLessonRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(lessonService, never()).createLesson(any());
    }

    @Test
    @WithMockCustomUser(roles = {"USER"})
    void createLesson_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/lessons/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLessonRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User does not have the required role"));

        verify(lessonService, never()).createLesson(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkCreateLessons_Success() throws Exception {
        LessonDTOs.BulkCreateLessonDTO bulkRequest = new LessonDTOs.BulkCreateLessonDTO();
        bulkRequest.setChapterId(1L);
        bulkRequest.setLessons(Collections.singletonList(createLessonRequestWithoutChapterId));

        when(lessonService.bulkCreateLessons(any()))
                .thenReturn(Collections.singletonList(testLessonResponseDto));

        mockMvc.perform(post("/api/v1/lessons/bulk-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testLessonResponseDto.getId()));

        verify(lessonService).bulkCreateLessons(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void updateLesson_Success() throws Exception {
        when(lessonService.updateLesson(eq(1L), any()))
                .thenReturn(testLessonResponseDto);

        mockMvc.perform(put("/api/v1/lessons/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLessonRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLessonResponseDto.getId()));

        verify(lessonService).updateLesson(eq(1L), any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void updateLesson_NotFound() throws Exception {
        when(lessonService.updateLesson(eq(999L), any()))
                .thenThrow(new ResourceNotFoundException("Lesson not found"));

        mockMvc.perform(put("/api/v1/lessons/999/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLessonRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lesson not found"));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void deleteLesson_Success() throws Exception {
        doNothing().when(lessonService).deleteSingleLesson(1L);

        mockMvc.perform(delete("/api/v1/lessons/1/delete"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Lesson deleted successfully"));

        verify(lessonService).deleteSingleLesson(1L);
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkDeleteLessons_Success() throws Exception {
        LessonDTOs.BulkOperationLessonDTO bulkRequest = new LessonDTOs.BulkOperationLessonDTO();
        bulkRequest.setLessonIds(Collections.singletonList(1L));

        doNothing().when(lessonService).bulkDeleteLessons(anyList());

        mockMvc.perform(delete("/api/v1/lessons/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Lessons deleted successfully"));

        verify(lessonService).bulkDeleteLessons(anyList());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void restoreLesson_Success() throws Exception {
        doNothing().when(lessonService).restoreLesson(1L);

        mockMvc.perform(post("/api/v1/lessons/1/restore"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Lesson restored successfully"));

        verify(lessonService).restoreLesson(1L);
    }

    @Test
    void searchLessons_Success() throws Exception {
        LessonDTOs.LessonSearchDTO searchRequest = new LessonDTOs.LessonSearchDTO();
        searchRequest.setTitle("Test");
        searchRequest.setStatus(CourseStatus.DRAFT);
        searchRequest.setCourseIds(Collections.singletonList(1L));
        searchRequest.setChapterIds(Collections.singletonList(1L));
        searchRequest.setType(LessonType.VIDEO);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        List<LessonDTOs.LessonDetailResponseDto> lessons = Collections.singletonList(testLessonDetailResponseDto);
        PageImpl<LessonDTOs.LessonDetailResponseDto> lessonPage = new PageImpl<>(lessons, PageRequest.of(0, 10), 1);

        when(lessonService.searchLessons(any(LessonDTOs.LessonSearchDTO.class)))
                .thenReturn(lessonPage);

        mockMvc.perform(post("/api/v1/lessons/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(1));

        verify(lessonService).searchLessons(any(LessonDTOs.LessonSearchDTO.class));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkUpdateLessons_Success() throws Exception {
        LessonDTOs.BulkUpdateLessonDTO bulkRequest = new LessonDTOs.BulkUpdateLessonDTO();
        bulkRequest.setLessonIds(Collections.singletonList(1L));
        bulkRequest.setLessons(Collections.singletonList(updateLessonRequest));

        when(lessonService.bulkUpdateLessons(any()))
                .thenReturn(Collections.singletonList(testLessonResponseDto));

        mockMvc.perform(put("/api/v1/lessons/bulk-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testLessonResponseDto.getId()));

        verify(lessonService).bulkUpdateLessons(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkUpdateLessons_ValidationError() throws Exception {
        LessonDTOs.BulkUpdateLessonDTO bulkRequest = new LessonDTOs.BulkUpdateLessonDTO();
        // Missing required fields

        mockMvc.perform(put("/api/v1/lessons/bulk-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(lessonService, never()).bulkUpdateLessons(any());
    }

    @Test
    @WithMockCustomUser(roles = {"INSTRUCTOR"})
    void updateLesson_InstructorAccess() throws Exception {
        when(lessonService.updateLesson(eq(1L), any()))
                .thenReturn(testLessonResponseDto);

        mockMvc.perform(put("/api/v1/lessons/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLessonRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testLessonResponseDto.getId()));

        verify(lessonService).updateLesson(eq(1L), any());
    }

    @Test
    @WithMockCustomUser(roles = {"INSTRUCTOR"})
    void updateLesson_InstructorNoAccess() throws Exception {
        when(lessonService.updateLesson(eq(1L), any()))
                .thenThrow(new ForbiddenException("You don't have permission to modify this lesson"));

        mockMvc.perform(put("/api/v1/lessons/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLessonRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You don't have permission to modify this lesson"));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkRestoreLessons_Success() throws Exception {
        LessonDTOs.BulkOperationLessonDTO bulkRequest = new LessonDTOs.BulkOperationLessonDTO();
        bulkRequest.setLessonIds(Collections.singletonList(1L));

        doNothing().when(lessonService).bulkRestoreLessons(anyList());

        mockMvc.perform(post("/api/v1/lessons/bulk-restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Lessons restored successfully"));

        verify(lessonService).bulkRestoreLessons(anyList());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void reorderLessons_Success() throws Exception {
        doNothing().when(lessonService).reorderLessons(1L);

        mockMvc.perform(post("/api/v1/lessons/1/reorder"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Lessons reordered successfully"));

        verify(lessonService).reorderLessons(1L);
    }

    @Test
    void searchLessons_WithInvalidDateRange() throws Exception {
        LessonDTOs.LessonSearchDTO searchRequest = new LessonDTOs.LessonSearchDTO();
        searchRequest.setFromDate(LocalDateTime.now().plusDays(1));
        searchRequest.setToDate(LocalDateTime.now()); // ToDate before FromDate

        mockMvc.perform(post("/api/v1/lessons/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("dateRangeValid: toDate must be after fromDate"));

        verify(lessonService, never()).searchLessons(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void updateLesson_InvalidStatus_ThrowsException() throws Exception {
        when(lessonService.updateLesson(eq(1L), any()))
                .thenThrow(new InvalidRequestException("Invalid status transition"));

        mockMvc.perform(put("/api/v1/lessons/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLessonRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition"));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void deleteLesson_AlreadyDeleted() throws Exception {
        doThrow(new InvalidRequestException("Lesson is already deleted"))
                .when(lessonService).deleteSingleLesson(1L);

        mockMvc.perform(delete("/api/v1/lessons/1/delete"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lesson is already deleted"));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void restoreLesson_NotDeleted() throws Exception {
        doThrow(new InvalidRequestException("Lesson is not deleted"))
                .when(lessonService).restoreLesson(1L);

        mockMvc.perform(post("/api/v1/lessons/1/restore"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lesson is not deleted"));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void createLesson_ValidationError() throws Exception {
        createLessonRequest.setTitle(""); // Invalid empty title

        mockMvc.perform(post("/api/v1/lessons/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createLessonRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("title: Lesson title is required"));

        verify(lessonService, never()).createLesson(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkOperationLessons_ExceedsLimit() throws Exception {
        List<Long> tooManyIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L); // Assuming limit is 5
        LessonDTOs.BulkOperationLessonDTO bulkRequest = new LessonDTOs.BulkOperationLessonDTO();
        bulkRequest.setLessonIds(tooManyIds);

        doThrow(new InvalidRequestException("Maximum of 5 lessons can be processed at once"))
                .when(lessonService).bulkDeleteLessons(tooManyIds);

        mockMvc.perform(delete("/api/v1/lessons/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Maximum of 5 lessons can be processed at once"));
    }
}