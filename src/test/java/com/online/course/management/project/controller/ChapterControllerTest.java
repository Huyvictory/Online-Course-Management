package com.online.course.management.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.aspect.RoleAuthorizationAspect;
import com.online.course.management.project.config.SecurityTestConfig;
import com.online.course.management.project.dto.ChapterDTOs;
import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.LessonType;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.filter.JwtAuthenticationFilter;
import com.online.course.management.project.security.annotation.WithMockCustomUser;
import com.online.course.management.project.service.interfaces.IChapterService;
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

@WebMvcTest(ChapterController.class)
@Import({SecurityTestConfig.class, RoleAuthorizationAspect.class})
@AutoConfigureMockMvc(addFilters = false)
class ChapterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IChapterService chapterService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private ChapterDTOs.ChapterDetailResponseDto testChapterDetailResponseDto;
    private ChapterDTOs.ChapterResponseDto testChapterResponseDto;
    private ChapterDTOs.CreateChapterDTO createChapterRequest;
    private ChapterDTOs.UpdateChapterDTO updateChapterRequest;
    private List<LessonDTOs.CreateLessonDTO> createLessonDtos;

    @BeforeEach
    void setUp() {
        // Setup chapter response DTOs
        testChapterDetailResponseDto = new ChapterDTOs.ChapterDetailResponseDto();
        testChapterDetailResponseDto.setId(1L);
        testChapterDetailResponseDto.setCourseId(1L);
        testChapterDetailResponseDto.setCourseTitle("Test Course");
        testChapterDetailResponseDto.setTitle("Test Chapter");
        testChapterDetailResponseDto.setDescription("Test Chapter Description");
        testChapterDetailResponseDto.setOrder(1);
        testChapterDetailResponseDto.setStatus(CourseStatus.DRAFT);
        testChapterDetailResponseDto.setCreatedAt(LocalDateTime.now());
        testChapterDetailResponseDto.setUpdatedAt(LocalDateTime.now());

        testChapterResponseDto = new ChapterDTOs.ChapterResponseDto();
        testChapterResponseDto.setId(1L);
        testChapterResponseDto.setCourseId(1L);
        testChapterResponseDto.setCourseTitle("Test Course");
        testChapterResponseDto.setTitle("Test Chapter");
        testChapterResponseDto.setDescription("Test Chapter Description");
        testChapterResponseDto.setOrder(1);
        testChapterResponseDto.setStatus(CourseStatus.DRAFT);

        // Setup create chapter request
        createChapterRequest = new ChapterDTOs.CreateChapterDTO();
        createChapterRequest.setCourseId(1L);
        createChapterRequest.setTitle("New Chapter");
        createChapterRequest.setDescription("New Chapter Description");
        createChapterRequest.setOrder(1);

        // Setup update chapter request
        updateChapterRequest = new ChapterDTOs.UpdateChapterDTO();
        updateChapterRequest.setTitle("Updated Chapter");
        updateChapterRequest.setDescription("Updated Chapter Description");
        updateChapterRequest.setOrder(2);
        updateChapterRequest.setStatus(CourseStatus.PUBLISHED);

        // Setup create lesson DTOs
        createLessonDtos = new ArrayList<>();
        LessonDTOs.CreateLessonDTO lessonDto = new LessonDTOs.CreateLessonDTO();
        lessonDto.setTitle("New Lesson");
        lessonDto.setContent("New Lesson Content");
        lessonDto.setOrder(1);
        lessonDto.setType(LessonType.VIDEO);
        createLessonDtos.add(lessonDto);
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void createChapter_Success() throws Exception {
        // Arrange
        when(chapterService.createChapter(any(ChapterDTOs.CreateChapterDTO.class)))
                .thenReturn(testChapterDetailResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createChapterRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testChapterDetailResponseDto.getId()))
                .andExpect(jsonPath("$.title").value(testChapterDetailResponseDto.getTitle()))
                .andExpect(jsonPath("$.courseId").value(testChapterDetailResponseDto.getCourseId()));

        verify(chapterService).createChapter(any(ChapterDTOs.CreateChapterDTO.class));
    }

    @Test
    void createChapter_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/chapters/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createChapterRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(chapterService, never()).createChapter(any());
    }

    @Test
    @WithMockCustomUser(roles = {"USER"})
    void createChapter_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/chapters/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createChapterRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(chapterService, never()).createChapter(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkCreateChapters_Success() throws Exception {
        // Arrange
        ChapterDTOs.BulkCreateChapterDTO bulkRequest = new ChapterDTOs.BulkCreateChapterDTO();
        bulkRequest.setChapters(Collections.singletonList(createChapterRequest));

        when(chapterService.bulkCreateChapters(any(ChapterDTOs.BulkCreateChapterDTO.class)))
                .thenReturn(Collections.singletonList(testChapterDetailResponseDto));

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/bulk-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testChapterDetailResponseDto.getId()));

        verify(chapterService).bulkCreateChapters(any(ChapterDTOs.BulkCreateChapterDTO.class));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void updateChapter_Success() throws Exception {
        // Arrange
        when(chapterService.updateChapter(eq(1L), any(ChapterDTOs.UpdateChapterDTO.class)))
                .thenReturn(testChapterDetailResponseDto);

        // Act & Assert
        mockMvc.perform(put("/api/v1/chapters/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateChapterRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testChapterDetailResponseDto.getId()))
                .andExpect(jsonPath("$.title").value(testChapterDetailResponseDto.getTitle()));

        verify(chapterService).updateChapter(eq(1L), any(ChapterDTOs.UpdateChapterDTO.class));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void updateChapter_NotFound() throws Exception {
        // Arrange
        when(chapterService.updateChapter(eq(999L), any(ChapterDTOs.UpdateChapterDTO.class)))
                .thenThrow(new ResourceNotFoundException("Chapter not found"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/chapters/999/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateChapterRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Chapter not found"));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void deleteChapter_Success() throws Exception {
        // Arrange
        doNothing().when(chapterService).deleteChapter(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/chapters/1/delete"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Chapter deleted successfully"));

        verify(chapterService).deleteChapter(1L);
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkDeleteChapters_Success() throws Exception {
        // Arrange
        ChapterDTOs.BulkOperationChapterDTO bulkRequest = new ChapterDTOs.BulkOperationChapterDTO();
        bulkRequest.setChapterIds(Collections.singletonList(1L));

        doNothing().when(chapterService).bulkDeleteChapters(anyList());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/chapters/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Chapters deleted successfully"));

        verify(chapterService).bulkDeleteChapters(anyList());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void restoreChapter_Success() throws Exception {
        // Arrange
        doNothing().when(chapterService).restoreChapter(1L);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/1/restore"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Chapter restored successfully"));

        verify(chapterService).restoreChapter(1L);
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkRestoreChapters_Success() throws Exception {
        // Arrange
        ChapterDTOs.BulkOperationChapterDTO bulkRequest = new ChapterDTOs.BulkOperationChapterDTO();
        bulkRequest.setChapterIds(Collections.singletonList(1L));

        doNothing().when(chapterService).bulkRestoreChapters(anyList());

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/bulk-restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Chapters restored successfully"));

        verify(chapterService).bulkRestoreChapters(anyList());
    }

    @Test
    void getChapterDetailsById_Success() throws Exception {
        // Arrange
        when(chapterService.getChapterById(1L)).thenReturn(testChapterResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/1/details"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testChapterResponseDto.getId()))
                .andExpect(jsonPath("$.title").value(testChapterResponseDto.getTitle()));

        verify(chapterService).getChapterById(1L);
    }

    @Test
    void getChapterDetailsWithLessons_Success() throws Exception {
        // Arrange
        when(chapterService.getChapterWithLessons(1L)).thenReturn(testChapterDetailResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/1/details-with-lessons"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testChapterDetailResponseDto.getId()))
                .andExpect(jsonPath("$.title").value(testChapterDetailResponseDto.getTitle()));

        verify(chapterService).getChapterWithLessons(1L);
    }

    @Test
    void getChaptersByCourseId_Success() throws Exception {
        // Arrange
        when(chapterService.getAllChaptersByCourseId(1L))
                .thenReturn(Collections.singletonList(testChapterResponseDto));

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/1/list"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testChapterResponseDto.getId()));

        verify(chapterService).getAllChaptersByCourseId(1L);
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkUpdateChapters_Success() throws Exception {
        // Arrange
        ChapterDTOs.BulkUpdateChapterDTO bulkRequest = new ChapterDTOs.BulkUpdateChapterDTO();
        bulkRequest.setChapterIds(Collections.singletonList(1L));
        bulkRequest.setChapters(Collections.singletonList(updateChapterRequest));

        when(chapterService.bulkUpdateChapters(anyList(), anyList()))
                .thenReturn(Collections.singletonList(testChapterResponseDto));

        // Act & Assert
        mockMvc.perform(put("/api/v1/chapters/bulk-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testChapterResponseDto.getId()));

        verify(chapterService).bulkUpdateChapters(anyList(), anyList());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkUpdateChapters_ValidationError() throws Exception {
        // Arrange
        ChapterDTOs.BulkUpdateChapterDTO bulkRequest = new ChapterDTOs.BulkUpdateChapterDTO();
        // Missing required fields

        // Act & Assert
        mockMvc.perform(put("/api/v1/chapters/bulk-update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(chapterService, never()).bulkUpdateChapters(anyList(), anyList());
    }

    @Test
    void searchChapters_Success() throws Exception {
        // Arrange
        ChapterDTOs.ChapterSearchDTO searchRequest = new ChapterDTOs.ChapterSearchDTO();
        searchRequest.setTitle("Test");
        searchRequest.setStatus(CourseStatus.DRAFT);
        searchRequest.setCourseId(1L);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        List<ChapterDTOs.ChapterResponseDto> chapters = Collections.singletonList(testChapterResponseDto);
        PageImpl<ChapterDTOs.ChapterResponseDto> chapterPage =
                new PageImpl<>(chapters, PageRequest.of(0, 10), 1);

        when(chapterService.searchChapters(any(ChapterDTOs.ChapterSearchDTO.class), any()))
                .thenReturn(chapterPage);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(1));

        verify(chapterService).searchChapters(any(ChapterDTOs.ChapterSearchDTO.class), any());
    }

    @Test
    void searchChapters_InvalidDateRange() throws Exception {
        // Arrange
        ChapterDTOs.ChapterSearchDTO searchRequest = new ChapterDTOs.ChapterSearchDTO();
        searchRequest.setFromDate(LocalDateTime.now().plusDays(1));
        searchRequest.setToDate(LocalDateTime.now());

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("dateRangeValid: toDate must be after fromDate"));

        verify(chapterService, never()).searchChapters(any(), any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void reorderChapters_Success() throws Exception {
        // Arrange
        doNothing().when(chapterService).reorderChapters(1L);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/1/reorder"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Chapter reordered successfully"));

        verify(chapterService).reorderChapters(1L);
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void createChapter_ValidationError() throws Exception {
        // Arrange
        createChapterRequest.setTitle(""); // Invalid empty title

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createChapterRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("title: Chapter title is required"));

        verify(chapterService, never()).createChapter(any());
    }

    @Test
    @WithMockCustomUser(roles = {"INSTRUCTOR"})
    void updateChapter_InstructorAccess() throws Exception {
        // Arrange
        when(chapterService.updateChapter(eq(1L), any(ChapterDTOs.UpdateChapterDTO.class)))
                .thenReturn(testChapterDetailResponseDto);

        // Act & Assert
        mockMvc.perform(put("/api/v1/chapters/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateChapterRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testChapterDetailResponseDto.getId()));

        verify(chapterService).updateChapter(eq(1L), any(ChapterDTOs.UpdateChapterDTO.class));
    }

    @Test
    @WithMockCustomUser(roles = {"INSTRUCTOR"})
    void updateChapter_InstructorNoAccess() throws Exception {
        // Arrange
        when(chapterService.updateChapter(eq(1L), any(ChapterDTOs.UpdateChapterDTO.class)))
                .thenThrow(new ForbiddenException("You don't have permission to modify this chapter"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/chapters/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateChapterRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message")
                        .value("You don't have permission to modify this chapter"));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void bulkDeleteChapters_ValidationError() throws Exception {
        // Arrange
        ChapterDTOs.BulkOperationChapterDTO bulkRequest = new ChapterDTOs.BulkOperationChapterDTO();
        // Missing required chapterIds

        // Act & Assert
        mockMvc.perform(delete("/api/v1/chapters/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bulkRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("chapterIds: Chapter IDs are required"));

        verify(chapterService, never()).bulkDeleteChapters(anyList());
    }

    @Test
    void getChapterDetailsById_NotFound() throws Exception {
        // Arrange
        when(chapterService.getChapterById(999L))
                .thenThrow(new ResourceNotFoundException("Chapter not found"));

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/999/details"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Chapter not found"));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void createChapter_WithLessons_Success() throws Exception {
        // Arrange
        createChapterRequest.setLessons(createLessonDtos);
        when(chapterService.createChapter(any(ChapterDTOs.CreateChapterDTO.class)))
                .thenReturn(testChapterDetailResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/chapters/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createChapterRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testChapterDetailResponseDto.getId()));

        verify(chapterService).createChapter(any(ChapterDTOs.CreateChapterDTO.class));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void updateChapter_InvalidStatus_ThrowsException() throws Exception {
        // Arrange
        when(chapterService.updateChapter(eq(1L), any(ChapterDTOs.UpdateChapterDTO.class)))
                .thenThrow(new InvalidRequestException("Invalid status transition"));

        // Act & Assert
        mockMvc.perform(put("/api/v1/chapters/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateChapterRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid status transition"));
    }
}