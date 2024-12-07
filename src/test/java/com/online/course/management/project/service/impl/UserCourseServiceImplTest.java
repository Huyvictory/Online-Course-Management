package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.UserCourseDTOs;
import com.online.course.management.project.entity.*;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.EnrollmentStatus;
import com.online.course.management.project.enums.ProgressStatus;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.mapper.UserCourseMapper;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.repository.IUserCourseRepository;
import com.online.course.management.project.repository.IUserRepository;
import com.online.course.management.project.utils.user.UserSecurityUtils;
import com.online.course.management.project.utils.usercourse.UserCourseServiceUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCourseServiceImplTest {

    @Mock
    private IUserCourseRepository userCourseRepository;

    @Mock
    private ICourseRepository courseRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private UserCourseMapper userCourseMapper;

    @Mock
    private UserSecurityUtils userSecurityUtils;

    @Mock
    private UserCourseServiceUtils userCourseServiceUtils;

    @InjectMocks
    private UserCourseServiceImpl userCourseService;

    private User testUser;
    private Course testCourse;
    private UserCourse testUserCourse;
    private UserCourseDTOs.UserCourseResponseDto testUserCourseResponseDto;
    private UserCourseDTOs.UserCourseRequestDTO enrollmentRequest;
    private List<UserLessonProgress> testUserLessonProgressList;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        Role userRole = new Role();
        userRole.setName(RoleType.USER);
        testUser.addRole(userRole);

        // Setup test course
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("Test Course");
        testCourse.setStatus(CourseStatus.PUBLISHED);
        testCourse.setCreatedAt(LocalDateTime.now());
        testCourse.setUpdatedAt(LocalDateTime.now());

        // Setup test user course
        testUserCourse = new UserCourse();
        testUserCourse.setId(1L);
        testUserCourse.setUser(testUser);
        testUserCourse.setCourse(testCourse);
        testUserCourse.setStatus(EnrollmentStatus.ENROLLED);
        testUserCourse.setEnrollmentDate(LocalDateTime.now());

        // Setup test user course response DTO
        testUserCourseResponseDto = new UserCourseDTOs.UserCourseResponseDto();
        testUserCourseResponseDto.setId(1L);
        testUserCourseResponseDto.setUserId(testUser.getId());
        testUserCourseResponseDto.setCourseId(testCourse.getId());
        testUserCourseResponseDto.setStatus(EnrollmentStatus.ENROLLED);
        testUserCourseResponseDto.setEnrollmentDate(LocalDateTime.now());

        // Setup enrollment request
        enrollmentRequest = new UserCourseDTOs.UserCourseRequestDTO();
        enrollmentRequest.setCourseId(1L);

        // Setup test user lesson progress list
        testUserLessonProgressList = new ArrayList<>();
        UserLessonProgress progress = new UserLessonProgress();
        progress.setUser(testUser);
        progress.setCourse(testCourse);
        progress.setStatus(ProgressStatus.NOT_STARTED);
        testUserLessonProgressList.add(progress);
    }

    @Test
    void enrollInCourse_Success() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(testCourse));
        when(userCourseMapper.toEntity(any(User.class), any(Course.class))).thenReturn(testUserCourse);
        when(userCourseRepository.save(any(UserCourse.class))).thenReturn(testUserCourse);
        when(userCourseMapper.toDto(any(UserCourse.class))).thenReturn(testUserCourseResponseDto);

        // Act
        UserCourseDTOs.UserCourseResponseDto result = userCourseService.enrollInCourse(enrollmentRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testUserCourseResponseDto.getId(), result.getId());
        assertEquals(testUserCourseResponseDto.getUserId(), result.getUserId());
        assertEquals(testUserCourseResponseDto.getCourseId(), result.getCourseId());
        assertEquals(testUserCourseResponseDto.getStatus(), result.getStatus());

        verify(userCourseServiceUtils).validateEnrollment(testUser.getId(), enrollmentRequest.getCourseId());
        verify(userCourseRepository).save(any(UserCourse.class));
    }

    @Test
    void enrollInCourse_AlreadyEnrolled_ThrowsException() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        doThrow(new InvalidRequestException("User is already enrolled in this course"))
                .when(userCourseServiceUtils).validateEnrollment(anyLong(), anyLong());

        // Act & Assert
        assertThrows(InvalidRequestException.class, () ->
                userCourseService.enrollInCourse(enrollmentRequest));
        verify(userCourseRepository, never()).save(any(UserCourse.class));
    }

    @Test
    void getEnrollmentDetails_Success() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userCourseRepository.findByUserIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(testUserCourse);
        when(userCourseMapper.toDto(any(UserCourse.class)))
                .thenReturn(testUserCourseResponseDto);

        // Act
        UserCourseDTOs.UserCourseResponseDto result =
                userCourseService.getEnrollmentDetails(testCourse.getId());

        // Assert
        assertNotNull(result);
        assertEquals(testUserCourseResponseDto.getId(), result.getId());
        assertEquals(testUserCourseResponseDto.getStatus(), result.getStatus());
        verify(userCourseRepository).findByUserIdAndCourseId(testUser.getId(), testCourse.getId());
    }

    @Test
    void getEnrollmentDetails_NotFound_ThrowsException() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userCourseRepository.findByUserIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                userCourseService.getEnrollmentDetails(testCourse.getId()));
    }

    @Test
    void searchUserEnrollments_Success() {
        // Arrange
        UserCourseDTOs.UserCourseSearchDTO searchRequest = new UserCourseDTOs.UserCourseSearchDTO();
        searchRequest.setName("Test");
        searchRequest.setStatus(EnrollmentStatus.ENROLLED);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        List<UserCourse> userCourses = Collections.singletonList(testUserCourse);
        Page<UserCourse> userCoursePage = new PageImpl<>(userCourses);

        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userCourseServiceUtils.createUserCourseSort(any()))
                .thenReturn(Sort.by(Sort.Direction.DESC, "enrollment_date"));
        when(userCourseRepository.searchUserEnrollments(
                eq(testUser.getId()),
                any(), any(), any(), any(), any(), any(), any(), any(), any(PageRequest.class)))
                .thenReturn(userCoursePage);
        when(userCourseMapper.toDto(any(UserCourse.class)))
                .thenReturn(testUserCourseResponseDto);

        // Act
        Page<UserCourseDTOs.UserCourseResponseDto> result =
                userCourseService.searchUserEnrollments(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testUserCourseResponseDto.getId(), result.getContent().get(0).getId());
    }

    @Test
    void dropEnrollment_Success() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userCourseRepository.findByUserIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(testUserCourse);
        doNothing().when(userCourseRepository)
                .dropRelevantProgress(anyLong(), anyLong());

        // Act
        userCourseService.dropEnrollment(testCourse.getId());

        // Assert
        verify(userCourseRepository).dropRelevantProgress(testUser.getId(), testCourse.getId());
    }

    @Test
    void dropEnrollment_CompletedCourse_ThrowsException() {
        // Arrange
        testUserCourse.setStatus(EnrollmentStatus.COMPLETED);
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userCourseRepository.findByUserIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(testUserCourse);

        // Act & Assert
        assertThrows(InvalidRequestException.class, () ->
                userCourseService.dropEnrollment(testCourse.getId()));
        verify(userCourseRepository, never()).dropRelevantProgress(anyLong(), anyLong());
    }

    @Test
    void resumeEnrollment_Success() {
        // Arrange
        testUserCourse.setStatus(EnrollmentStatus.DROPPED);
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userCourseRepository.findByUserIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(testUserCourse);
        doNothing().when(userCourseRepository)
                .resumeRelevantProgress(anyLong(), anyLong());

        // Act
        userCourseService.resumeEnrollment(testCourse.getId());

        // Assert
        verify(userCourseRepository).resumeRelevantProgress(testUser.getId(), testCourse.getId());
    }

    @Test
    void resumeEnrollment_NotDropped_ThrowsException() {
        // Arrange
        testUserCourse.setStatus(EnrollmentStatus.ENROLLED);
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(userCourseRepository.findByUserIdAndCourseId(anyLong(), anyLong()))
                .thenReturn(testUserCourse);

        // Act & Assert
        assertThrows(InvalidRequestException.class, () ->
                userCourseService.resumeEnrollment(testCourse.getId()));
        verify(userCourseRepository, never()).resumeRelevantProgress(anyLong(), anyLong());
    }
}