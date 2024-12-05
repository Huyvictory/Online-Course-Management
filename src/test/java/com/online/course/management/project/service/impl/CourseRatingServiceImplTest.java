package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.CourseRatingDTOs;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.CourseRating;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.mapper.CourseRatingMapper;
import com.online.course.management.project.repository.ICourseRatingRepository;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.utils.courserating.CourseRatingServiceUtils;
import com.online.course.management.project.utils.user.UserSecurityUtils;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseRatingServiceImplTest {

    @Mock
    private ICourseRatingRepository courseRatingRepository;

    @Mock
    private CourseRatingMapper courseRatingMapper;

    @Mock
    private UserSecurityUtils userSecurityUtils;

    @Mock
    private CourseRatingServiceUtils courseRatingServiceUtils;

    @Mock
    private ICourseRepository courseRepository;

    @InjectMocks
    private CourseRatingServiceImpl courseRatingService;

    private User testUser;
    private Course testCourse;
    private CourseRating testCourseRating;
    private CourseRatingDTOs.CourseRatingResponseDTO testRatingResponseDto;
    private CourseRatingDTOs.CourseRatingCreateDTO createRatingRequest;
    private CourseRatingDTOs.CourseRatingUpdateDTO updateRatingRequest;

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

        // Setup test course rating
        testCourseRating = new CourseRating();
        testCourseRating.setId(1L);
        testCourseRating.setUser(testUser);
        testCourseRating.setCourse(testCourse);
        testCourseRating.setRating(5);
        testCourseRating.setReviewText("Great course!");
        testCourseRating.setCreatedAt(LocalDateTime.now());
        testCourseRating.setUpdatedAt(LocalDateTime.now());

        // Setup test rating response DTO
        testRatingResponseDto = new CourseRatingDTOs.CourseRatingResponseDTO();
        testRatingResponseDto.setId(1L);
        testRatingResponseDto.setUserId(1L);
        testRatingResponseDto.setReviewerName("Test User");
        testRatingResponseDto.setCourseId(1L);
        testRatingResponseDto.setRating(5);
        testRatingResponseDto.setReviewText("Great course!");
        testRatingResponseDto.setCreatedAt(LocalDateTime.now());
        testRatingResponseDto.setUpdatedAt(LocalDateTime.now());

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
    }

    @Test
    void createCourseRating_Success() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(courseRatingRepository.existsByUserIdAndCourseId(anyLong(), anyLong())).thenReturn(false);
        when(courseRepository.findById(anyLong())).thenReturn(Optional.of(testCourse));
        when(courseRatingRepository.save(any(CourseRating.class))).thenReturn(testCourseRating);
        when(courseRatingMapper.toDto(any(CourseRating.class))).thenReturn(testRatingResponseDto);

        // Act
        CourseRatingDTOs.CourseRatingResponseDTO result = courseRatingService.createCourseRating(createRatingRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testRatingResponseDto.getId(), result.getId());
        assertEquals(testRatingResponseDto.getRating(), result.getRating());
        assertEquals(testRatingResponseDto.getReviewText(), result.getReviewText());

        verify(courseRatingRepository).save(any(CourseRating.class));
        verify(courseRatingMapper).toDto(any(CourseRating.class));
    }

    @Test
    void createCourseRating_AlreadyExists() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(courseRatingRepository.existsByUserIdAndCourseId(anyLong(), anyLong())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> courseRatingService.createCourseRating(createRatingRequest));

        verify(courseRatingRepository, never()).save(any());
    }

    @Test
    void updateCourseRating_Success() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(courseRatingRepository.findByUserIdAndCourseIdAndId(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.of(testCourseRating));
        when(courseRatingRepository.save(any(CourseRating.class))).thenReturn(testCourseRating);
        when(courseRatingMapper.toDto(any(CourseRating.class))).thenReturn(testRatingResponseDto);

        // Act
        CourseRatingDTOs.CourseRatingResponseDTO result = courseRatingService.updateCourseRating(updateRatingRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testRatingResponseDto.getId(), result.getId());
        verify(courseRatingRepository).save(any(CourseRating.class));
    }

    @Test
    void updateCourseRating_NotFound() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(courseRatingRepository.findByUserIdAndCourseIdAndId(anyLong(), anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> courseRatingService.updateCourseRating(updateRatingRequest));

        verify(courseRatingRepository, never()).save(any());
    }

    @Test
    void getCourseRatings_Success() {
        // Arrange
        CourseRatingDTOs.CourseRatingSearchDTO searchRequest = new CourseRatingDTOs.CourseRatingSearchDTO();
        searchRequest.setCourseId(1L);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        List<CourseRating> ratings = Collections.singletonList(testCourseRating);
        Page<CourseRating> ratingPage = new PageImpl<>(ratings);

        when(courseRatingServiceUtils.createCourseRatingSort(any())).thenReturn(Sort.by(Sort.Direction.DESC, "rating"));

        when(courseRatingRepository.searchRatings(
                anyLong(), any(), any(), any(PageRequest.class)))
                .thenReturn(ratingPage);
        when(courseRatingMapper.toDto(any(CourseRating.class))).thenReturn(testRatingResponseDto);

        // Act
        Page<CourseRatingDTOs.CourseRatingResponseDTO> result = courseRatingService.getCourseRatings(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testRatingResponseDto.getId(), result.getContent().get(0).getId());
    }

    @Test
    void deleteCourseRating_Success() {
        // Arrange
        when(userSecurityUtils.getCurrentUser()).thenReturn(testUser);
        when(courseRatingRepository.existsById(anyLong())).thenReturn(true);
        when(courseRatingRepository.findById(anyLong())).thenReturn(Optional.of(testCourseRating));

        // Act
        courseRatingService.deleteCourseRating(1L);

        // Assert
        verify(courseRatingRepository).softDeleteRating(1L);
    }

    @Test
    void deleteCourseRating_NotFound() {
        // Arrange
        when(courseRatingRepository.existsById(anyLong())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> courseRatingService.deleteCourseRating(1L));

        verify(courseRatingRepository, never()).softDeleteRating(anyLong());
    }

    @Test
    void deleteCourseRating_UnauthorizedUser() {
        // Arrange
        User differentUser = new User();
        differentUser.setId(2L);

        when(userSecurityUtils.getCurrentUser()).thenReturn(differentUser);
        when(courseRatingRepository.existsById(anyLong())).thenReturn(true);
        when(courseRatingRepository.findById(anyLong())).thenReturn(Optional.of(testCourseRating));

        // Act & Assert
        assertThrows(ForbiddenException.class,
                () -> courseRatingService.deleteCourseRating(1L));

        verify(courseRatingRepository, never()).softDeleteRating(anyLong());
    }

    @Test
    void getCourseRatingDistribution_Success() {
        // Arrange
        Map<Long, Long> distribution = new HashMap<>();
        distribution.put(5L, 10L);
        distribution.put(4L, 5L);

        Map<Long, Double> percentages = new HashMap<>();
        percentages.put(5L, 66.7);
        percentages.put(4L, 33.3);

        when(courseRatingServiceUtils.getRatingDistributionFormatted(anyLong())).thenReturn(distribution);
        when(courseRatingServiceUtils.getRatingPercentages(anyLong())).thenReturn(percentages);

        // Act
        CourseRatingDTOs.RatingDistributionDTO result = courseRatingService.getCourseRatingDistribution(1L);

        // Assert
        assertNotNull(result);
        assertEquals(distribution, result.getDistribution());
        assertEquals(percentages, result.getPercentages());
    }

    @Test
    void getCourseRatings_WithSorting() {
        // Arrange
        CourseRatingDTOs.CourseRatingSearchDTO searchRequest = new CourseRatingDTOs.CourseRatingSearchDTO();
        searchRequest.setCourseId(1L);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);
        Map<String, String> sort = new HashMap<>();
        sort.put("rating", "desc");
        searchRequest.setSort(sort);

        List<CourseRating> ratings = Collections.singletonList(testCourseRating);
        Page<CourseRating> ratingPage = new PageImpl<>(ratings);

        Sort customSort = Sort.by(Sort.Direction.DESC, "rating");
        when(courseRatingServiceUtils.createCourseRatingSort(any())).thenReturn(customSort);
        when(courseRatingRepository.searchRatings(anyLong(), any(), any(), any()))
                .thenReturn(ratingPage);
        when(courseRatingMapper.toDto(any(CourseRating.class))).thenReturn(testRatingResponseDto);

        // Act
        Page<CourseRatingDTOs.CourseRatingResponseDTO> result = courseRatingService.getCourseRatings(searchRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(courseRatingServiceUtils).validateSortFields(sort);
        verify(courseRatingServiceUtils).createCourseRatingSort(sort);
    }
}