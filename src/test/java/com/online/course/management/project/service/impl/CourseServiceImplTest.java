package com.online.course.management.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.entity.Category;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.Role;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.mapper.CourseMapper;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.repository.IUserRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseServiceImplTest {

    @Mock
    private ICourseRepository courseRepository;

    @Mock
    private IUserRepository userRepository;


    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CourseServiceUtils courseServiceUtils;

    @InjectMocks
    private CourseServiceImpl courseService;

    private Course testCourse;
    private User testInstructor;
    private Category testCategory;
    private CourseDTOS.CourseDetailsResponseDto testCourseResponseDto;
    private CourseDTOS.CreateCourseRequestDTO createCourseRequest;
    private CourseDTOS.UpdateCourseRequestDTO updateCourseRequest;


    @BeforeEach
    void setUp() {
        // Setup instructor test data
        testInstructor = new User();
        testInstructor.setId(1L);
        testInstructor.setUsername("instructor");
        testInstructor.setEmail("instructor@test.com");
        Role instructorRole = new Role();
        instructorRole.setName(RoleType.INSTRUCTOR);
        testInstructor.addRole(instructorRole);

        // Setup category test data
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");
        testCategory.setCourses(new java.util.HashSet<>());

        // Setup course test data
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("Test Course");
        testCourse.setDescription("This is a test course");
        testCourse.setInstructor(testInstructor);
        testCourse.setStatus(CourseStatus.DRAFT);
        testCourse.setCreatedAt(LocalDateTime.now());
        testCourse.setUpdatedAt(LocalDateTime.now());
        testCourse.getCategories().add(testCategory);

        // Setup create course response DTO
        testCourseResponseDto = new CourseDTOS.CourseDetailsResponseDto();
        testCourseResponseDto.setId(1L);
        testCourseResponseDto.setTitle("Test Course");
        testCourseResponseDto.setDescription("This is a test course");
        testCourseResponseDto.setStatus(CourseStatus.DRAFT);
        testCourseResponseDto.setCreatedAt(LocalDateTime.now());
        testCourseResponseDto.setUpdatedAt(LocalDateTime.now());

        // Setup create course request DTO
        createCourseRequest = new CourseDTOS.CreateCourseRequestDTO();
        createCourseRequest.setTitle("Test Course");
        createCourseRequest.setDescription("Test Description");
        createCourseRequest.setInstructorId(1L);
        createCourseRequest.setCategoryIds(Set.of(1L));
        createCourseRequest.setStatus(CourseStatus.DRAFT);

        // Setup update course request DTO
        updateCourseRequest = new CourseDTOS.UpdateCourseRequestDTO();
        updateCourseRequest.setTitle("Updated Course");
        updateCourseRequest.setDescription("This is an updated course");
        updateCourseRequest.setStatus(CourseStatus.PUBLISHED);
    }

    @Test
    void createCourse_Success() {
        // Arrange
        when(courseMapper.toEntity(any())).thenReturn(testCourse);
        when(courseServiceUtils.determineInstructor(anyLong())).thenReturn(testInstructor);
        when(courseServiceUtils.validateCategories(any())).thenReturn(Set.of(testCategory));
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);
        when(courseMapper.toDto(any(Course.class))).thenReturn(testCourseResponseDto);

        // Act
        CourseDTOS.CourseDetailsResponseDto result = courseService.createCourse(createCourseRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testCourseResponseDto.getId(), result.getId());
        assertEquals(testCourseResponseDto.getTitle(), result.getTitle());
        assertEquals(testCourseResponseDto.getDescription(), result.getDescription());
        assertEquals(testCourseResponseDto.getStatus(), result.getStatus());

        verify(courseMapper).toEntity(createCourseRequest);
        verify(courseServiceUtils).determineInstructor(createCourseRequest.getInstructorId());
        verify(courseServiceUtils).validateCategories(createCourseRequest.getCategoryIds());
        verify(courseRepository).save(testCourse);
        verify(courseMapper).toDto(testCourse);
    }

    @Test
    void createCourse_InstructorNotFound() {
        // Arrange
        when(courseMapper.toEntity(any(CourseDTOS.CreateCourseRequestDTO.class))).thenReturn(testCourse);
        when(courseServiceUtils.determineInstructor(anyLong())).thenThrow(new ResourceNotFoundException("Instructor not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> courseService.createCourse(createCourseRequest));

        verify(courseRepository, never()).save(testCourse);
        verify(courseMapper, never()).toDto(testCourse);
    }

    @Test
    void createCourse_CategoryNotFound() {
        // Arrange
        when(courseMapper.toEntity(any(CourseDTOS.CreateCourseRequestDTO.class))).thenReturn(testCourse);
        when(courseServiceUtils.validateCategories(any())).thenThrow(new ResourceNotFoundException("Category not found"));

        // Act
        assertThrows(ResourceNotFoundException.class, () -> courseService.createCourse(createCourseRequest));

        // Arrange
        verify(courseRepository, never()).save(testCourse);
        verify(courseMapper, never()).toDto(testCourse);
    }

    @Test
    void updateCourse_Success() throws JsonProcessingException {
        // Arrange
        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);

        doNothing().when(courseServiceUtils).validateCourseStatus(any(), any());
        doNothing().when(courseMapper).updateCourseFromDto(any(), any());

        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);
        when(courseMapper.toDto(any(Course.class))).thenReturn(testCourseResponseDto);

        // Act
        CourseDTOS.CourseDetailsResponseDto result = courseService.updateCourse(1L, updateCourseRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testCourseResponseDto.getId(), result.getId());
        assertEquals(testCourseResponseDto.getTitle(), result.getTitle());
        assertEquals(testCourseResponseDto.getDescription(), result.getDescription());
        assertEquals(testCourseResponseDto.getStatus(), result.getStatus());

        verify(courseRepository).save(testCourse);
        verify(courseMapper).toDto(testCourse);

    }

    @Test
    void updateCourse_InvalidStatus_ThrowsException() throws JsonProcessingException {
        // Arrange
        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);
        doThrow(new InvalidRequestException("Invalid status transition"))
                .when(courseServiceUtils).validateCourseStatus(any(), any());

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> courseService.updateCourse(1L, updateCourseRequest));
        verify(courseRepository, never()).save(any());
    }

    @Test
    void archiveCourse_Success() {
        // Arrange
        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);
        doNothing().when(courseRepository).archiveCourse(anyLong());

        // Act
        courseService.archiveCourse(1L);

        // Assert
        verify(courseRepository).archiveCourse(1L);
    }

    @Test
    void archiveCourse_AlreadyArchived_ThrowsException() {
        // Arrange
        testCourse.setStatus(CourseStatus.ARCHIVED);
        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> courseService.archiveCourse(1L));

        verify(courseRepository, never()).archiveCourse(anyLong());
    }

    @Test
    void archiveCourse_NotFound_ThrowsException() {
        // Arrange
        when(courseServiceUtils.getCourseWithValidation(anyLong()))
                .thenThrow(new ResourceNotFoundException("Course not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.archiveCourse(1L));
        verify(courseRepository, never()).archiveCourse(anyLong());
    }

    @Test
    void unarchiveCourse_Success() {
        // Arrange
        testCourse.setStatus(CourseStatus.ARCHIVED);
        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);
        doNothing().when(courseRepository).unarchiveCourse(anyLong());

        // Act
        courseService.unarchiveCourse(1L);

        // Assert
        verify(courseRepository).unarchiveCourse(1L);
    }

    @Test
    void unmarchiveCourse_UnarchivedCourse_ThrowsException() {
        // Arrange
        testCourse.setStatus(CourseStatus.PUBLISHED);
        when(courseServiceUtils.getCourseWithValidation(anyLong())).thenReturn(testCourse);

        // Act & Assert
        assertThrows(InvalidRequestException.class,
                () -> courseService.unarchiveCourse(1L));

        verify(courseRepository, never()).archiveCourse(anyLong());
    }

    @Test
    void getCourseById_Success() {
        // Arrange
        when(courseServiceUtils.GetCourseWithoutValidation(anyLong())).thenReturn(testCourse);
        when(courseMapper.toDto(any())).thenReturn(testCourseResponseDto);

        // Act
        CourseDTOS.CourseDetailsResponseDto result = courseService.getCourseById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testCourseResponseDto.getId(), result.getId());
    }

    @Test
    void getCourseById_NotFound() {
        // Arrange
        when(courseServiceUtils.GetCourseWithoutValidation(anyLong()))
                .thenThrow(new ResourceNotFoundException("Course not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.getCourseById(1L));
    }

    @Test
    void searchCourses_Success() {
        // Arrange
        List<Course> courses = Collections.singletonList(testCourse);
        Page<Course> coursePage = new PageImpl<>(courses);
        CourseDTOS.SearchCourseRequestDTO searchRequest = new CourseDTOS.SearchCourseRequestDTO();
        searchRequest.setTitle("Test");
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        // Mock the CourseServiceUtils sort handling
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "created_at");
        when(courseServiceUtils.handleCreateDefaultSort()).thenReturn(defaultSort);

        when(courseRepository.searchCourses(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(coursePage);
        when(courseMapper.toDto(any())).thenReturn(testCourseResponseDto);

        // Act
        Page<CourseDTOS.CourseDetailsResponseDto> result = courseService.searchCourses(
                searchRequest, PageRequest.of(searchRequest.getPage(), searchRequest.getLimit()));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testCourseResponseDto.getId(), result.getContent().get(0).getId());
    }

    @Test
    void searchCourses_WithSortParams_Success() {
        // Arrange
        List<Course> courses = Collections.singletonList(testCourse);
        Page<Course> coursePage = new PageImpl<>(courses);

        CourseDTOS.SearchCourseRequestDTO searchRequest = new CourseDTOS.SearchCourseRequestDTO();
        searchRequest.setTitle("Test");
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        Map<String, String> sortParams = new HashMap<>();
        sortParams.put("title", "asc");
        searchRequest.setSort(sortParams);

        Sort customSort = Sort.by(Sort.Direction.ASC, "title");
        when(courseServiceUtils.createSort(any())).thenReturn(customSort);
        doNothing().when(courseServiceUtils).validateSortFields(any());

        when(courseRepository.searchCourses(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        )).thenReturn(coursePage);

        when(courseMapper.toDto(any(Course.class))).thenReturn(testCourseResponseDto);

        // Act
        Page<CourseDTOS.CourseDetailsResponseDto> result = courseService.searchCourses(
                searchRequest,
                searchRequest.toPageable()
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(courseServiceUtils).validateSortFields(sortParams);
        verify(courseServiceUtils).createSort(sortParams);
        verify(courseRepository).searchCourses(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void searchCourses_WithInvalidSortField_ThrowsException() {
        // Arrange
        CourseDTOS.SearchCourseRequestDTO searchRequest = new CourseDTOS.SearchCourseRequestDTO();
        searchRequest.setTitle("Test");
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        Map<String, String> sortParams = new HashMap<>();
        sortParams.put("invalidField", "asc");
        searchRequest.setSort(sortParams);

        doThrow(new InvalidRequestException("Invalid sort fields"))
                .when(courseServiceUtils).validateSortFields(any());

        // Act & Assert
        assertThrows(InvalidRequestException.class, () ->
                courseService.searchCourses(searchRequest, searchRequest.toPageable())
        );

        verify(courseServiceUtils).validateSortFields(sortParams);
        verify(courseRepository, never()).searchCourses(
                any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    @Test
    void searchCourses_WithAllParameters_Success() {
        // Arrange
        List<Course> courses = Collections.singletonList(testCourse);
        Page<Course> coursePage = new PageImpl<>(courses);

        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();

        CourseDTOS.SearchCourseRequestDTO searchRequest = new CourseDTOS.SearchCourseRequestDTO();
        searchRequest.setTitle("Test");
        searchRequest.setStatus(CourseStatus.PUBLISHED);
        searchRequest.setInstructorName("Test Instructor");
        searchRequest.setFromDate(fromDate);
        searchRequest.setToDate(toDate);
        searchRequest.setCategoryIds(Set.of(1L));
        searchRequest.setIncludeArchived(true);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        Sort defaultSort = Sort.by(Sort.Direction.DESC, "created_at");
        when(courseServiceUtils.handleCreateDefaultSort()).thenReturn(defaultSort);

        when(courseRepository.searchCourses(
                eq("Test"),
                eq("PUBLISHED"),
                eq("Test Instructor"),
                eq(fromDate),
                eq(toDate),
                eq(Set.of(1L)),
                eq(true),
                any(Pageable.class)
        )).thenReturn(coursePage);

        when(courseMapper.toDto(any(Course.class))).thenReturn(testCourseResponseDto);

        // Act
        Page<CourseDTOS.CourseDetailsResponseDto> result = courseService.searchCourses(
                searchRequest,
                searchRequest.toPageable()
        );

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(courseRepository).searchCourses(
                eq("Test"),
                eq("PUBLISHED"),
                eq("Test Instructor"),
                eq(fromDate),
                eq(toDate),
                eq(Set.of(1L)),
                eq(true),
                any(Pageable.class)
        );
    }

    @Test
    void getCoursesByInstructor_Success() {
        // Arrange
        List<Course> courses = Collections.singletonList(testCourse);
        Page<Course> coursePage = new PageImpl<>(courses);

        when(userRepository.existsById(anyLong())).thenReturn(true);
        when(courseRepository.findByInstructorId(anyLong(), anyBoolean(), any(Pageable.class)))
                .thenReturn(coursePage);
        Sort defaultSort = Sort.by(Sort.Direction.DESC, "created_at");
        when(courseServiceUtils.handleCreateDefaultSort()).thenReturn(defaultSort);
        when(courseMapper.toDto(any())).thenReturn(testCourseResponseDto);

        // Act
        Page<CourseDTOS.CourseDetailsResponseDto> result = courseService.getCoursesByInstructor(
                1L, false, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getCoursesByInstructor_InstructorNotFound() {
        // Arrange
        when(userRepository.existsById(anyLong())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> courseService.getCoursesByInstructor(1L, false, PageRequest.of(0, 10)));
        verify(courseRepository, never()).findByInstructorId(anyLong(), anyBoolean(), any(Pageable.class));
    }

    @Test
    void getCoursesByStatus_Success() {
        // Arrange
        List<Course> courses = Collections.singletonList(testCourse);
        Page<Course> coursePage = new PageImpl<>(courses);

        Sort defaultSort = Sort.by(Sort.Direction.DESC, "created_at");
        when(courseServiceUtils.handleCreateDefaultSort()).thenReturn(defaultSort);

        when(courseRepository.findByStatus(anyString(), any(Pageable.class)))
                .thenReturn(coursePage);

        when(courseMapper.toDto(any())).thenReturn(testCourseResponseDto);

        // Act
        Page<CourseDTOS.CourseDetailsResponseDto> result = courseService.getCoursesByStatus(
                CourseStatus.DRAFT, PageRequest.of(0, 10));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getLatestCourses_Success() {
        // Arrange
        List<Course> courses = Collections.singletonList(testCourse);
        when(courseRepository.findLatestCourses(anyInt())).thenReturn(courses);
        when(courseMapper.toDto(any())).thenReturn(testCourseResponseDto);

        // Act
        List<CourseDTOS.CourseDetailsResponseDto> result = courseService.getLatestCourses(10);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testCourseResponseDto.getId(), result.get(0).getId());
    }

}
