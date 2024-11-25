package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.ChapterDTOs;
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
import com.online.course.management.project.mapper.ChapterMapper;
import com.online.course.management.project.repository.IChapterRepository;
import com.online.course.management.project.utils.chapter.ChapterServiceUtils;
import com.online.course.management.project.utils.course.CourseServiceUtils;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChapterServiceImplTest {

    @Mock
    private IChapterRepository chapterRepository;

    @Mock
    private ChapterMapper chapterMapper;

    @Mock
    private ChapterServiceUtils chapterServiceUtils;

    @Mock
    private CourseServiceUtils courseServiceUtils;

    @InjectMocks
    private ChapterServiceImpl chapterService;

    private Course testCourse;
    private Chapter testChapter;
    private Lesson testLesson;
    private ChapterDTOs.ChapterDetailResponseDto testChapterDetailResponseDto;
    private ChapterDTOs.ChapterResponseDto testChapterResponseDto;
    private ChapterDTOs.CreateChapterDTO createChapterRequest;
    private ChapterDTOs.UpdateChapterDTO updateChapterRequest;
    private List<LessonDTOs.CreateLessonDTO> createLessonDtos;

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

        // Setup lesson
        testLesson = new Lesson();
        testLesson.setId(1L);
        testLesson.setTitle("Test Lesson");
        testLesson.setContent("Test Content");
        testLesson.setOrder(1);
        testLesson.setType(LessonType.VIDEO);
        testLesson.setStatus(CourseStatus.DRAFT);
        testLesson.setCreatedAt(LocalDateTime.now());
        testLesson.setUpdatedAt(LocalDateTime.now());

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
        testChapter.setLessons(Collections.singletonList(testLesson));
        testLesson.setChapter(testChapter);

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
    void createChapter_Success() {
        // Arrange
        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);
        when(chapterServiceUtils.validateChapterOrder(anyLong(), anyInt())).thenReturn(null);
        when(chapterServiceUtils.createChapterWithLessons(any(), any())).thenReturn(testChapter);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter);
        when(chapterMapper.toDetailDto(any(Chapter.class))).thenReturn(testChapterDetailResponseDto);

        // Act
        ChapterDTOs.ChapterDetailResponseDto result = chapterService.createChapter(createChapterRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testChapterDetailResponseDto.getId(), result.getId());
        assertEquals(testChapterDetailResponseDto.getTitle(), result.getTitle());
        assertEquals(testChapterDetailResponseDto.getCourseId(), result.getCourseId());

        verify(courseServiceUtils).getCourseWithValidation(createChapterRequest.getCourseId());
        verify(chapterServiceUtils).validateChapterOrder(testCourse.getId(), createChapterRequest.getOrder());
        verify(chapterRepository).save(any(Chapter.class));
        verify(chapterMapper).toDetailDto(testChapter);
    }

    @Test
    void createChapter_WithLessons_Success() {
        // Arrange
        createChapterRequest.setLessons(createLessonDtos);

        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);
        when(chapterServiceUtils.validateChapterOrder(anyLong(), anyInt())).thenReturn(null);
        when(chapterServiceUtils.createChapterWithLessons(any(), any())).thenReturn(testChapter);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter);
        when(chapterMapper.toDetailDto(any(Chapter.class))).thenReturn(testChapterDetailResponseDto);

        // Act
        ChapterDTOs.ChapterDetailResponseDto result = chapterService.createChapter(createChapterRequest);

        // Assert
        assertNotNull(result);
        verify(chapterServiceUtils).validateBulkLessonsOrders(anyLong(), anyList());
        verify(chapterRepository).save(any(Chapter.class));
    }

    @Test
    void createChapter_DuplicateOrder_ThrowsException() {
        // Arrange
        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);
        when(chapterServiceUtils.validateChapterOrder(anyLong(), anyInt()))
                .thenReturn("Order number 1 is already taken in this course");

        // Act & Assert
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> chapterService.createChapter(createChapterRequest));
        assertEquals("Order number 1 is already taken in this course", exception.getMessage());

        verify(chapterRepository, never()).save(any(Chapter.class));
    }

    @Test
    void updateChapter_Success() {
        // Arrange
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter);
        when(chapterMapper.toDetailDto(any(Chapter.class))).thenReturn(testChapterDetailResponseDto);

        // Act
        ChapterDTOs.ChapterDetailResponseDto result = chapterService.updateChapter(1L, updateChapterRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testChapterDetailResponseDto.getId(), result.getId());
        verify(chapterServiceUtils).getChapterOrThrow(1L);
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(chapterRepository).save(testChapter);
    }

    @Test
    void updateChapter_WithStatusChange_Success() throws Exception {
        // Arrange
        testChapterDetailResponseDto.setStatus(CourseStatus.PUBLISHED);
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        when(chapterRepository.save(any(Chapter.class))).thenReturn(testChapter);
        when(chapterMapper.toDetailDto(any(Chapter.class))).thenReturn(testChapterDetailResponseDto);

        // Act
        ChapterDTOs.ChapterDetailResponseDto result = chapterService.updateChapter(1L, updateChapterRequest);

        // Assert
        assertNotNull(result);
        assertEquals(CourseStatus.PUBLISHED, result.getStatus());
        verify(chapterServiceUtils).validateChapterStatus(any(), any());
    }

    @Test
    void updateChapter_NotFound_ThrowsException() {
        // Arrange
        when(chapterServiceUtils.getChapterOrThrow(anyLong()))
                .thenThrow(new ResourceNotFoundException("Chapter not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> chapterService.updateChapter(999L, updateChapterRequest));
        verify(chapterRepository, never()).save(any(Chapter.class));
    }

    @Test
    void deleteChapter_Success() {
        // Arrange
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        doNothing().when(chapterRepository).batchSoftDeleteChapters(anyList());
        doNothing().when(chapterRepository).batchSoftDeleteLessonsChapters(anyList());

        // Act
        chapterService.deleteChapter(1L);

        // Assert
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(chapterRepository).batchSoftDeleteChapters(Collections.singletonList(1L));
        verify(chapterRepository).batchSoftDeleteLessonsChapters(Collections.singletonList(1L));
    }

    @Test
    void deleteChapter_AlreadyDeleted_ThrowsException() {
        // Arrange
        testChapter.setDeletedAt(LocalDateTime.now());
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> chapterService.deleteChapter(1L));
        verify(chapterRepository, never()).batchSoftDeleteChapters(anyList());
    }

    @Test
    void searchChapters_Success() {
        // Arrange
        ChapterDTOs.ChapterSearchDTO searchRequest = new ChapterDTOs.ChapterSearchDTO();
        searchRequest.setTitle("Test");
        searchRequest.setStatus(CourseStatus.DRAFT);
        searchRequest.setCourseId(1L);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        // Mock the createChapterSort method to return default sort
        Sort defaultSort = Sort.by(Sort.Direction.ASC, "order_number");
        when(chapterServiceUtils.createChapterSort(any())).thenReturn(defaultSort);

        List<Chapter> chapters = Collections.singletonList(testChapter);
        Page<Chapter> chapterPage = new PageImpl<>(chapters);

        when(chapterRepository.searchChapters(
                eq("Test"),
                eq(CourseStatus.DRAFT.name()),
                eq(1L),
                any(),
                any(),
                any(Pageable.class)
        )).thenReturn(chapterPage);
        when(chapterMapper.toDto(any(Chapter.class))).thenReturn(testChapterResponseDto);

        // Act
        Page<ChapterDTOs.ChapterResponseDto> result = chapterService.searchChapters(
                searchRequest, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testChapterResponseDto.getTitle(), result.getContent().get(0).getTitle());
    }

    @Test
    void reorderChapters_Success() {
        // Arrange
        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);
        doNothing().when(chapterRepository).reorderChapters(anyLong());

        // Act
        chapterService.reorderChapters(1L);

        // Assert
        verify(courseServiceUtils).getCourseWithValidation(1L);
        verify(chapterRepository).reorderChapters(1L);
    }

    @Test
    void bulkCreateChapters_Success() {
        // Arrange
        ChapterDTOs.BulkCreateChapterDTO bulkRequest = new ChapterDTOs.BulkCreateChapterDTO();
        bulkRequest.setChapters(Collections.singletonList(createChapterRequest));

        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);
        when(chapterServiceUtils.createChapterWithLessons(any(), any())).thenReturn(testChapter);
        when(chapterRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testChapter));
        when(chapterMapper.toDetailDto(any(Chapter.class))).thenReturn(testChapterDetailResponseDto);

        // Act
        List<ChapterDTOs.ChapterDetailResponseDto> result = chapterService.bulkCreateChapters(bulkRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testChapterDetailResponseDto.getId(), result.get(0).getId());

        verify(courseServiceUtils).getCourseWithValidation(createChapterRequest.getCourseId());
        verify(chapterServiceUtils).validateBulkChapterOrders(eq(testCourse.getId()), anyList());
        verify(chapterRepository).saveAll(anyList());
    }

    @Test
    void bulkCreateChapters_EmptyRequest_ThrowsException() {
        // Arrange
        ChapterDTOs.BulkCreateChapterDTO bulkRequest = new ChapterDTOs.BulkCreateChapterDTO();
        bulkRequest.setChapters(Collections.emptyList());

        doThrow(new InvalidRequestException("No chapters provided for creation"))
                .when(chapterServiceUtils).validateBulkCreateRequest(any());

        // Act & Assert
        assertThrows(InvalidRequestException.class, () -> chapterService.bulkCreateChapters(bulkRequest));
        verify(chapterRepository, never()).saveAll(anyList());
    }

    @Test
    void bulkUpdateChapters_Success() {
        // Arrange
        List<Long> chapterIds = Collections.singletonList(1L);
        List<ChapterDTOs.UpdateChapterDTO> updateRequests = Collections.singletonList(updateChapterRequest);

        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        when(chapterRepository.saveAll(anyList())).thenReturn(Collections.singletonList(testChapter));
        when(chapterMapper.toDto(any(Chapter.class))).thenReturn(testChapterResponseDto);

        // Act
        List<ChapterDTOs.ChapterResponseDto> result = chapterService.bulkUpdateChapters(chapterIds, updateRequests);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testChapterResponseDto.getId(), result.get(0).getId());

        verify(chapterServiceUtils).validateBulkOperation(chapterIds);
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(chapterRepository).saveAll(anyList());
    }

    @Test
    void bulkDeleteChapters_Success() {
        // Arrange
        List<Long> chapterIds = Collections.singletonList(1L);
        when(chapterRepository.findAllById(anyList())).thenReturn(Collections.singletonList(testChapter));
        doNothing().when(chapterRepository).batchSoftDeleteChapters(anyList());
        doNothing().when(chapterRepository).batchSoftDeleteLessonsChapters(anyList());

        // Act
        chapterService.bulkDeleteChapters(chapterIds);

        // Assert
        verify(chapterServiceUtils).validateBulkOperation(chapterIds);
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(chapterRepository).batchSoftDeleteChapters(chapterIds);
        verify(chapterRepository).batchSoftDeleteLessonsChapters(chapterIds);
    }

    @Test
    void restoreChapter_Success() {
        // Arrange
        testChapter.setDeletedAt(LocalDateTime.now());
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        doNothing().when(chapterRepository).batchRestoreChapters(anyList());
        doNothing().when(chapterRepository).batchRestoreLessonsChapters(anyList());

        // Act
        chapterService.restoreChapter(1L);

        // Assert
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(chapterRepository).batchRestoreChapters(Collections.singletonList(1L));
        verify(chapterRepository).batchRestoreLessonsChapters(Collections.singletonList(1L));
    }

    @Test
    void restoreChapter_NotDeleted_ThrowsException() {
        // Arrange
        testChapter.setDeletedAt(null);
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> chapterService.restoreChapter(1L));
        verify(chapterRepository, never()).batchRestoreChapters(anyList());
    }

    @Test
    void bulkRestoreChapters_Success() {
        // Arrange
        List<Long> chapterIds = Collections.singletonList(1L);
        testChapter.setDeletedAt(LocalDateTime.now());
        when(chapterRepository.findAllById(anyList())).thenReturn(Collections.singletonList(testChapter));
        doNothing().when(chapterRepository).batchRestoreChapters(anyList());
        doNothing().when(chapterRepository).batchRestoreLessonsChapters(anyList());

        // Act
        chapterService.bulkRestoreChapters(chapterIds);

        // Assert
        verify(chapterServiceUtils).validateBulkOperation(chapterIds);
        verify(chapterServiceUtils).validateChapterAccess(testChapter);
        verify(chapterRepository).batchRestoreChapters(chapterIds);
        verify(chapterRepository).batchRestoreLessonsChapters(chapterIds);
    }

    @Test
    void getChapterById_Success() {
        // Arrange
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        when(chapterMapper.toDto(any(Chapter.class))).thenReturn(testChapterResponseDto);

        // Act
        ChapterDTOs.ChapterResponseDto result = chapterService.getChapterById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testChapterResponseDto.getId(), result.getId());
        assertEquals(testChapterResponseDto.getTitle(), result.getTitle());

        verify(chapterServiceUtils).getChapterOrThrow(1L);
        verify(chapterMapper).toDto(testChapter);
    }

    @Test
    void getChapterWithLessons_Success() {
        // Arrange
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        when(chapterMapper.toDetailDto(any(Chapter.class))).thenReturn(testChapterDetailResponseDto);

        // Act
        ChapterDTOs.ChapterDetailResponseDto result = chapterService.getChapterWithLessons(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testChapterDetailResponseDto.getId(), result.getId());
        assertEquals(testChapterDetailResponseDto.getTitle(), result.getTitle());

        verify(chapterServiceUtils).getChapterOrThrow(1L);
        verify(chapterMapper).toDetailDto(testChapter);
    }

    @Test
    void getAllChaptersByCourseId_Success() {
        // Arrange
        when(courseServiceUtils.GetCourseWithoutValidation(anyLong())).thenReturn(testCourse);
        when(chapterRepository.findAllChaptersByCourseId(anyLong()))
                .thenReturn(Collections.singletonList(testChapter));
        when(chapterMapper.toDto(any(Chapter.class))).thenReturn(testChapterResponseDto);

        // Act
        List<ChapterDTOs.ChapterResponseDto> result = chapterService.getAllChaptersByCourseId(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testChapterResponseDto.getId(), result.get(0).getId());

        verify(courseServiceUtils).GetCourseWithoutValidation(1L);
        verify(chapterRepository).findAllChaptersByCourseId(1L);
        verify(chapterMapper).toDto(testChapter);
    }

    @Test
    void searchChapters_WithSorting_Success() {
        // Arrange
        ChapterDTOs.ChapterSearchDTO searchRequest = new ChapterDTOs.ChapterSearchDTO();
        searchRequest.setTitle("Test");
        searchRequest.setStatus(CourseStatus.DRAFT);
        searchRequest.setCourseId(1L);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);
        Map<String, String> sortParams = new HashMap<>();
        sortParams.put("title", "asc");
        searchRequest.setSort(sortParams);

        List<Chapter> chapters = Collections.singletonList(testChapter);
        Page<Chapter> chapterPage = new PageImpl<>(chapters);
        Sort sort = Sort.by(Sort.Direction.ASC, "title");

        when(chapterServiceUtils.createChapterSort(any())).thenReturn(sort);
        when(chapterRepository.searchChapters(
                any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(chapterPage);
        when(chapterMapper.toDto(any(Chapter.class))).thenReturn(testChapterResponseDto);

        // Act
        Page<ChapterDTOs.ChapterResponseDto> result = chapterService.searchChapters(
                searchRequest, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(chapterServiceUtils).validateSortFields(sortParams);
        verify(chapterServiceUtils).createChapterSort(sortParams);
    }

    @Test
    void searchChapters_InvalidSortField_ThrowsException() {
        // Arrange
        ChapterDTOs.ChapterSearchDTO searchRequest = new ChapterDTOs.ChapterSearchDTO();
        Map<String, String> sortParams = new HashMap<>();
        sortParams.put("invalidField", "asc");
        searchRequest.setSort(sortParams);

        doThrow(new InvalidRequestException("Invalid sort fields"))
                .when(chapterServiceUtils).validateSortFields(any());

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> chapterService.searchChapters(searchRequest, PageRequest.of(0, 10)));
        verify(chapterRepository, never()).searchChapters(
                any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void updateChapter_ArchivedCourse_ThrowsForbiddenException() {
        // Arrange
        testCourse.setStatus(CourseStatus.ARCHIVED);
        when(chapterServiceUtils.getChapterOrThrow(anyLong())).thenReturn(testChapter);
        doThrow(new ForbiddenException("Cannot modify chapter of an archived course"))
                .when(chapterServiceUtils).validateChapterAccess(any());

        // Act & Assert
        assertThrows(ForbiddenException.class,
                () -> chapterService.updateChapter(1L, updateChapterRequest));
        verify(chapterRepository, never()).save(any(Chapter.class));
    }
}