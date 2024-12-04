package com.online.course.management.project.service.impl;

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
import com.online.course.management.project.mapper.LessonMapper;
import com.online.course.management.project.repository.ILessonRepository;
import com.online.course.management.project.utils.chapter.ChapterServiceUtils;
import com.online.course.management.project.utils.lesson.LessonServiceUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    @Mock
    private ILessonRepository lessonRepository;

    @Mock
    private LessonMapper lessonMapper;

    @Mock
    private LessonServiceUtils lessonServiceUtils;

    @Mock
    private ChapterServiceUtils chapterServiceUtils;

    @InjectMocks
    private LessonServiceImpl lessonService;

    private Course testCourse;
    private Chapter testChapter;
    private Lesson testLesson;
    private User testInstructor;
    private LessonDTOs.LessonResponseDto testLessonResponseDto;
    private LessonDTOs.LessonDetailResponseDto testLessonDetailResponseDto;
    private LessonDTOs.CreateLessonDTOWithChapterId createLessonRequest;
    private LessonDTOs.UpdateLessonDTO updateLessonRequest;

    @BeforeEach
    void setUp() {
        // Setup instructor
        testInstructor = new User();
        testInstructor.setId(1L);
        testInstructor.setUsername("instructor");
        testInstructor.setEmail("instructor@test.com");

        // Setup course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("Test Course");
        testCourse.setDescription("Test Course Description");
        testCourse.setInstructor(testInstructor);
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

        // Setup update lesson request
        updateLessonRequest = new LessonDTOs.UpdateLessonDTO();
        updateLessonRequest.setTitle("Updated Lesson");
        updateLessonRequest.setContent("Updated Content");
        updateLessonRequest.setOrder(2);
        updateLessonRequest.setType(LessonType.TEXT);
        updateLessonRequest.setStatus(CourseStatus.PUBLISHED);
    }

    @Test
    void createLesson_Success() {
        // Arrange
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        when(lessonServiceUtils.hasLessonOrderTakenSingle(anyLong(), anyInt())).thenReturn(null);
        when(lessonMapper.toEntity(any())).thenReturn(testLesson);
        when(lessonRepository.save(any())).thenReturn(testLesson);
        when(lessonMapper.toDto(any())).thenReturn(testLessonResponseDto);

        // Act
        LessonDTOs.LessonResponseDto result = lessonService.createLesson(createLessonRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testLessonResponseDto.getId(), result.getId());
        assertEquals(testLessonResponseDto.getTitle(), result.getTitle());
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(lessonRepository).save(any(Lesson.class));
    }

    @Test
    void createLesson_DuplicateOrder_ThrowsException() {
        // Arrange
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        when(lessonServiceUtils.hasLessonOrderTakenSingle(anyLong(), anyInt()))
                .thenReturn("Order number 1 is already taken in this chapter");

        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> lessonService.createLesson(createLessonRequest));
        assertEquals("Order number 1 is already taken in this chapter", exception.getMessage());
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void updateLesson_Success() {
        // Arrange
        when(lessonServiceUtils.GetLessonOrThrow(anyLong())).thenReturn(testLesson);
        when(lessonRepository.save(any())).thenReturn(testLesson);
        when(lessonMapper.toDto(any())).thenReturn(testLessonResponseDto);

        // Act
        LessonDTOs.LessonResponseDto result = lessonService.updateLesson(1L, updateLessonRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testLessonResponseDto.getId(), result.getId());
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(lessonRepository).save(testLesson);
    }

    @Test
    void updateLesson_NotFound() {
        // Arrange
        when(lessonServiceUtils.GetLessonOrThrow(anyLong()))
                .thenThrow(new ResourceNotFoundException("Lesson not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> lessonService.updateLesson(999L, updateLessonRequest));
        verify(lessonRepository, never()).save(any());
    }

    @Test
    void deleteSingleLesson_Success() {
        // Arrange
        when(lessonServiceUtils.GetLessonOrThrow(anyLong())).thenReturn(testLesson);
        doNothing().when(lessonRepository).batchSoftDeleteLessons(anyList());

        // Act
        lessonService.deleteSingleLesson(1L);

        // Assert
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(lessonRepository).batchSoftDeleteLessons(Collections.singletonList(1L));
    }

    @Test
    void deleteSingleLesson_AlreadyDeleted() {
        // Arrange
        testLesson.setDeletedAt(LocalDateTime.now());
        when(lessonServiceUtils.GetLessonOrThrow(anyLong())).thenReturn(testLesson);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> lessonService.deleteSingleLesson(1L));
        verify(lessonRepository, never()).batchSoftDeleteLessons(anyList());
    }

    @Test
    void searchLessons_Success() {
        // Arrange
        LessonDTOs.LessonSearchDTO searchRequest = new LessonDTOs.LessonSearchDTO();
        searchRequest.setTitle("Test");
        searchRequest.setStatus(CourseStatus.DRAFT);
        searchRequest.setCourseIds(List.of(1L));
        searchRequest.setChapterIds(List.of(1L));
        searchRequest.setType(LessonType.VIDEO);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        List<Lesson> lessons = Collections.singletonList(testLesson);
        Page<Lesson> lessonPage = new PageImpl<>(lessons);

        Sort defaultSort = Sort.by(Sort.Direction.DESC, "created_at");
        when(lessonServiceUtils.createLessonSortParams(any())).thenReturn(defaultSort);
        when(lessonRepository.searchLessons(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(lessonPage);
        when(lessonMapper.toDetailDto(any())).thenReturn(testLessonDetailResponseDto);

        // Act
        Page<LessonDTOs.LessonDetailResponseDto> result = lessonService.searchLessons(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testLessonDetailResponseDto.getId(), result.getContent().get(0).getId());
        verify(lessonRepository).searchLessons(
                eq("Test"),
                eq("DRAFT"),
                eq(List.of(1L)),
                eq(List.of(1L)),
                eq("VIDEO"),
                any(),
                any(),
                any(Pageable.class)
        );
    }

    @Test
    void bulkCreateLessons_Success() {
        // Arrange
        List<LessonDTOs.CreateLessonDTO> lessonDtos = Collections.singletonList(createLessonRequest);
        LessonDTOs.BulkCreateLessonDTO bulkRequest = new LessonDTOs.BulkCreateLessonDTO();
        bulkRequest.setChapterId(1L);
        bulkRequest.setLessons(lessonDtos);

        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        when(lessonServiceUtils.hasLessonOrderTakenMultiple(anyLong(), anyList()))
                .thenReturn(Collections.emptyList());
        when(lessonMapper.toEntity(any())).thenReturn(testLesson);
        when(lessonRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testLesson));
        when(lessonMapper.toDto(any())).thenReturn(testLessonResponseDto);

        // Act
        List<LessonDTOs.LessonResponseDto> result = lessonService.bulkCreateLessons(bulkRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testLessonResponseDto.getId(), result.get(0).getId());
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(lessonRepository).saveAll(anyList());
    }

    @Test
    void reorderLessons_Success() {
        // Arrange
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        doNothing().when(lessonRepository).reorderLessons(anyLong());

        // Act
        lessonService.reorderLessons(1L);

        // Assert
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(lessonRepository).reorderLessons(1L);
    }

    @Test
    void bulkUpdateLessons_Success() {
        // Arrange
        List<Long> lessonIds = Collections.singletonList(1L);
        List<LessonDTOs.UpdateLessonDTO> updateRequests = Collections.singletonList(updateLessonRequest);
        LessonDTOs.BulkUpdateLessonDTO bulkRequest = new LessonDTOs.BulkUpdateLessonDTO();
        bulkRequest.setLessonIds(lessonIds);
        bulkRequest.setLessons(updateRequests);

        when(lessonRepository.findAllById(anyList())).thenReturn(Collections.singletonList(testLesson));
        when(lessonRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testLesson));
        when(lessonMapper.toDto(any())).thenReturn(testLessonResponseDto);

        // Act
        List<LessonDTOs.LessonResponseDto> result = lessonService.bulkUpdateLessons(bulkRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testLessonResponseDto.getId(), result.get(0).getId());
        verify(lessonMapper).updateLessonFromDto(eq(updateLessonRequest), eq(testLesson));
        verify(lessonRepository).saveAll(anyList());
    }

    @Test
    void bulkUpdateLessons_NotFound() {
        // Arrange
        List<Long> lessonIds = Collections.singletonList(999L);
        List<LessonDTOs.UpdateLessonDTO> updateRequests = Collections.singletonList(updateLessonRequest);
        LessonDTOs.BulkUpdateLessonDTO bulkRequest = new LessonDTOs.BulkUpdateLessonDTO();
        bulkRequest.setLessonIds(lessonIds);
        bulkRequest.setLessons(updateRequests);

        doThrow(new ResourceNotFoundException("Lesson not found")).when(lessonServiceUtils).validateBulkOperation(anyList());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> lessonService.bulkUpdateLessons(bulkRequest));
        verify(lessonRepository, never()).saveAll(anyList());
    }

    @Test
    void bulkDeleteLessons_Success() {
        // Arrange
        List<Long> lessonIds = Collections.singletonList(1L);
        when(lessonRepository.findAllById(anyList())).thenReturn(Collections.singletonList(testLesson));
        doNothing().when(lessonRepository).batchSoftDeleteLessons(anyList());

        // Act
        lessonService.bulkDeleteLessons(lessonIds);

        // Assert
        verify(lessonServiceUtils).validateBulkOperation(lessonIds);
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(lessonRepository).batchSoftDeleteLessons(lessonIds);
    }

    @Test
    void bulkRestoreLessons_Success() {
        // Arrange
        List<Long> lessonIds = Collections.singletonList(1L);
        testLesson.setDeletedAt(LocalDateTime.now());
        when(lessonRepository.findAllById(anyList())).thenReturn(Collections.singletonList(testLesson));
        doNothing().when(lessonRepository).batchRestoreLessons(anyList());

        // Act
        lessonService.bulkRestoreLessons(lessonIds);

        // Assert
        verify(lessonServiceUtils).validateBulkOperation(lessonIds);
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(lessonRepository).batchRestoreLessons(lessonIds);
    }

    @Test
    void restoreLesson_Success() {
        // Arrange
        testLesson.setDeletedAt(LocalDateTime.now());
        when(lessonServiceUtils.GetLessonOrThrow(anyLong())).thenReturn(testLesson);
        doNothing().when(lessonRepository).batchRestoreLessons(anyList());

        // Act
        lessonService.restoreLesson(1L);

        // Assert
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(lessonRepository).batchRestoreLessons(Collections.singletonList(1L));
    }

    @Test
    void restoreLesson_NotDeleted() {
        // Arrange
        testLesson.setDeletedAt(null);
        when(lessonServiceUtils.GetLessonOrThrow(anyLong())).thenReturn(testLesson);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> lessonService.restoreLesson(1L));
        verify(lessonRepository, never()).batchRestoreLessons(anyList());
    }

    @Test
    void searchLessons_WithSorting_Success() {
        // Arrange
        LessonDTOs.LessonSearchDTO searchRequest = new LessonDTOs.LessonSearchDTO();
        searchRequest.setTitle("Test");
        searchRequest.setPage(1);
        searchRequest.setLimit(10);
        Map<String, String> sortParams = new HashMap<>();
        sortParams.put("title", "asc");
        searchRequest.setSort(sortParams);

        List<Lesson> lessons = Collections.singletonList(testLesson);
        Page<Lesson> lessonPage = new PageImpl<>(lessons);

        Sort sort = Sort.by(Sort.Direction.ASC, "title");
        when(lessonServiceUtils.createLessonSortParams(any())).thenReturn(sort);
        when(lessonRepository.searchLessons(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(lessonPage);
        when(lessonMapper.toDetailDto(any())).thenReturn(testLessonDetailResponseDto);

        // Act
        Page<LessonDTOs.LessonDetailResponseDto> result = lessonService.searchLessons(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(lessonServiceUtils).validateSortFields(sortParams);
        verify(lessonServiceUtils).createLessonSortParams(sortParams);
    }

    @Test
    void searchLessons_InvalidSortField_ThrowsException() {
        // Arrange
        LessonDTOs.LessonSearchDTO searchRequest = new LessonDTOs.LessonSearchDTO();
        Map<String, String> sortParams = new HashMap<>();
        sortParams.put("invalidField", "asc");
        searchRequest.setSort(sortParams);

        doThrow(new InvalidRequestException("Invalid sort fields"))
                .when(lessonServiceUtils).validateSortFields(any());

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> lessonService.searchLessons(searchRequest));
        verify(lessonRepository, never()).searchLessons(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void updateLesson_ArchivedChapter_ThrowsForbiddenException() {
        // Arrange
        testChapter.setStatus(CourseStatus.ARCHIVED);
        when(lessonServiceUtils.GetLessonOrThrow(anyLong())).thenReturn(testLesson);
        doThrow(new ForbiddenException("Cannot modify lesson of an archived chapter"))
                .when(chapterServiceUtils).validateChapterAccess(any());

        // Act & Assert
        assertThrows(ForbiddenException.class,
                () -> lessonService.updateLesson(1L, updateLessonRequest));
        verify(lessonRepository, never()).save(any());
    }

    // Helper method to create a DTO based on existing test lesson
    private LessonDTOs.CreateLessonDTO createLessonDTOFromExisting(Lesson existingLesson, int order) {
        LessonDTOs.CreateLessonDTO dto = new LessonDTOs.CreateLessonDTO();
        dto.setTitle(existingLesson.getTitle());
        dto.setContent(existingLesson.getContent());
        dto.setOrder(order);
        dto.setType(existingLesson.getType());
        dto.setStatus(CourseStatus.DRAFT);
        return dto;
    }

    @Test
    void bulkCreateLessons_DuplicateOrders_ThrowsException() {
        // Arrange
        // Create lesson DTOs with duplicate orders using existing test data as base
        List<LessonDTOs.CreateLessonDTO> lessonsWithDuplicateOrders = Arrays.asList(
                createLessonDTOFromExisting(testLesson, 1),  // First lesson with order 1
                createLessonDTOFromExisting(testLesson, 1)   // Second lesson with same order 1
        );

        // Use existing test chapter
        LessonDTOs.BulkCreateLessonDTO request = new LessonDTOs.BulkCreateLessonDTO();
        request.setChapterId(testChapter.getId());
        request.setLessons(lessonsWithDuplicateOrders);

        // Mock behavior using existing test objects
        when(chapterServiceUtils.getChapterOrThrow(testChapter.getId())).thenReturn(testChapter);
        when(lessonMapper.toEntity(any(LessonDTOs.CreateLessonDTO.class))).thenReturn(testLesson);
        when(lessonServiceUtils.IsListOrdersContainsDuplicates(anyList())).thenReturn(true);

        // Act & Assert
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> lessonService.bulkCreateLessons(request)
        );
        assertEquals("Duplicate lesson order found", exception.getMessage());

        // Verify that no lessons were saved
        verify(lessonRepository, never()).saveAll(anyList());
    }
}