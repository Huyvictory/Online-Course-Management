package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.UserLessonProgressDtos;
import com.online.course.management.project.entity.*;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.LessonType;
import com.online.course.management.project.enums.ProgressStatus;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.mapper.UserLessonProgressMapper;
import com.online.course.management.project.repository.IUserLessonProgressRepository;
import com.online.course.management.project.utils.user.UserSecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLessonProgressImplTest {

    @Mock
    private IUserLessonProgressRepository userLessonProgressRepository;

    @Mock
    private UserLessonProgressMapper userLessonProgressMapper;

    @Mock
    private UserSecurityUtils userSecurityUtils;

    @InjectMocks
    private UserLessonProgressImpl userLessonProgressService;

    private User testUser;
    private Course testCourse;
    private Chapter testChapter;
    private Lesson testLesson;
    private UserLessonProgress testUserLessonProgress;
    private UserLessonProgressDtos.LessonProgressResponseDTO testProgressResponseDto;
    private UserLessonProgressDtos.UpdateStatusLessonProgressDTO updateStatusRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");

        Role userRole = new Role();
        userRole.setName(RoleType.USER);
        testUser.addRole(userRole);

        // Setup test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("Test Course");
        testCourse.setDescription("Test Course Description");
        testCourse.setStatus(CourseStatus.PUBLISHED);

        // Setup test chapter
        testChapter = new Chapter();
        testChapter.setId(1L);
        testChapter.setCourse(testCourse);
        testChapter.setTitle("Test Chapter");
        testChapter.setDescription("Test Chapter Description");
        testChapter.setOrder(1);
        testChapter.setStatus(CourseStatus.PUBLISHED);

        // Setup test lesson
        testLesson = new Lesson();
        testLesson.setId(1L);
        testLesson.setChapter(testChapter);
        testLesson.setTitle("Test Lesson");
        testLesson.setContent("Test Content");
        testLesson.setOrder(1);
        testLesson.setType(LessonType.VIDEO);
        testLesson.setStatus(CourseStatus.PUBLISHED);

        // Setup test user lesson progress
        testUserLessonProgress = new UserLessonProgress();
        testUserLessonProgress.setId(1L);
        testUserLessonProgress.setUser(testUser);
        testUserLessonProgress.setCourse(testCourse);
        testUserLessonProgress.setChapter(testChapter);
        testUserLessonProgress.setLesson(testLesson);
        testUserLessonProgress.setStatus(ProgressStatus.NOT_STARTED);
        testUserLessonProgress.setLastAccessedAt(LocalDateTime.now());

        // Setup test progress response DTO
        testProgressResponseDto = new UserLessonProgressDtos.LessonProgressResponseDTO();
        testProgressResponseDto.setId(1);
        testProgressResponseDto.setCourseId(1);
        testProgressResponseDto.setChapterId(1);
        testProgressResponseDto.setLessonId(1);
        testProgressResponseDto.setStatus(ProgressStatus.NOT_STARTED.name());

        // Setup update status request DTO
        updateStatusRequest = new UserLessonProgressDtos.UpdateStatusLessonProgressDTO();
        updateStatusRequest.setId(1L);
    }

    @Test
    void startLearningLesson_Success() {
        // Arrange
        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userLessonProgressRepository.save(any(UserLessonProgress.class))).thenReturn(testUserLessonProgress);
        when(userLessonProgressMapper.toDto(any(UserLessonProgress.class))).thenReturn(testProgressResponseDto);

        // Act
        UserLessonProgressDtos.LessonProgressResponseDTO result = userLessonProgressService.startLearningLesson(updateStatusRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testProgressResponseDto.getId(), result.getId());
        verify(userLessonProgressRepository).save(any(UserLessonProgress.class));
        verify(userLessonProgressMapper).toDto(any(UserLessonProgress.class));
    }

    @Test
    void startLearningLesson_AlreadyStarted_ThrowsException() {
        // Arrange
        testUserLessonProgress.setStatus(ProgressStatus.IN_PROGRESS);
        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> userLessonProgressService.startLearningLesson(updateStatusRequest));
        verify(userLessonProgressRepository, never()).save(any(UserLessonProgress.class));
    }

    @Test
    void startLearningLesson_AlreadyCompleted_ThrowsException() {
        // Arrange
        testUserLessonProgress.setStatus(ProgressStatus.COMPLETED);
        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> userLessonProgressService.startLearningLesson(updateStatusRequest));
        verify(userLessonProgressRepository, never()).save(any(UserLessonProgress.class));
    }

    @Test
    void startLearningLesson_UnauthorizedUser_ThrowsException() {
        // Arrange
        User differentUser = new User();
        differentUser.setId(2L);
        differentUser.setUsername("differentUser");

        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.getCurrentUser()).thenReturn(differentUser);
        when(userSecurityUtils.isAdmin()).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> userLessonProgressService.startLearningLesson(updateStatusRequest));
        verify(userLessonProgressRepository, never()).save(any(UserLessonProgress.class));
    }

    @Test
    void completeLearningLesson_Success() {
        // Arrange
        testUserLessonProgress.setStatus(ProgressStatus.IN_PROGRESS);
        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userLessonProgressRepository.save(any(UserLessonProgress.class))).thenReturn(testUserLessonProgress);
        when(userLessonProgressMapper.toDto(any(UserLessonProgress.class))).thenReturn(testProgressResponseDto);

        // Act
        UserLessonProgressDtos.LessonProgressResponseDTO result = userLessonProgressService.completeLearningLesson(updateStatusRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testProgressResponseDto.getId(), result.getId());
        verify(userLessonProgressRepository).save(any(UserLessonProgress.class));
        verify(userLessonProgressMapper).toDto(any(UserLessonProgress.class));
    }

    @Test
    void completeLearningLesson_NotStarted_ThrowsException() {
        // Arrange
        testUserLessonProgress.setStatus(ProgressStatus.NOT_STARTED);
        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> userLessonProgressService.completeLearningLesson(updateStatusRequest));
        verify(userLessonProgressRepository, never()).save(any(UserLessonProgress.class));
    }

    @Test
    void completeLearningLesson_AlreadyCompleted_ThrowsException() {
        // Arrange
        testUserLessonProgress.setStatus(ProgressStatus.COMPLETED);
        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> userLessonProgressService.completeLearningLesson(updateStatusRequest));
        verify(userLessonProgressRepository, never()).save(any(UserLessonProgress.class));
    }

    @Test
    void completeLearningLesson_UnauthorizedUser_ThrowsException() {
        // Arrange
        testUserLessonProgress.setStatus(ProgressStatus.IN_PROGRESS);
        User differentUser = new User();
        differentUser.setId(2L);
        differentUser.setUsername("differentUser");

        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.getCurrentUser()).thenReturn(differentUser);
        when(userSecurityUtils.isAdmin()).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> userLessonProgressService.completeLearningLesson(updateStatusRequest));
        verify(userLessonProgressRepository, never()).save(any(UserLessonProgress.class));
    }

    @Test
    void startLearningLesson_AdminAccess_Success() {
        // Arrange
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");

        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.isAdmin()).thenReturn(true);
        when(userLessonProgressRepository.save(any(UserLessonProgress.class))).thenReturn(testUserLessonProgress);
        when(userLessonProgressMapper.toDto(any(UserLessonProgress.class))).thenReturn(testProgressResponseDto);

        // Act
        UserLessonProgressDtos.LessonProgressResponseDTO result = userLessonProgressService.startLearningLesson(updateStatusRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testProgressResponseDto.getId(), result.getId());
        verify(userLessonProgressRepository).save(any(UserLessonProgress.class));
    }

    @Test
    void completeLearningLesson_AdminAccess_Success() {
        // Arrange
        testUserLessonProgress.setStatus(ProgressStatus.IN_PROGRESS);
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setUsername("admin");

        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.of(testUserLessonProgress));
        when(userSecurityUtils.isAdmin()).thenReturn(true);
        when(userLessonProgressRepository.save(any(UserLessonProgress.class))).thenReturn(testUserLessonProgress);
        when(userLessonProgressMapper.toDto(any(UserLessonProgress.class))).thenReturn(testProgressResponseDto);

        // Act
        UserLessonProgressDtos.LessonProgressResponseDTO result = userLessonProgressService.completeLearningLesson(updateStatusRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testProgressResponseDto.getId(), result.getId());
        verify(userLessonProgressRepository).save(any(UserLessonProgress.class));
    }

    @Test
    void startLearningLesson_ProgressNotFound_ThrowsException() {
        // Arrange
        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> userLessonProgressService.startLearningLesson(updateStatusRequest));
        verify(userLessonProgressRepository, never()).save(any(UserLessonProgress.class));
    }

    @Test
    void completeLearningLesson_ProgressNotFound_ThrowsException() {
        // Arrange
        when(userLessonProgressRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> userLessonProgressService.completeLearningLesson(updateStatusRequest));
        verify(userLessonProgressRepository, never()).save(any(UserLessonProgress.class));
    }
}