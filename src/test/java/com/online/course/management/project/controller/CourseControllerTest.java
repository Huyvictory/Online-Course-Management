package com.online.course.management.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.aspect.RoleAuthorizationAspect;
import com.online.course.management.project.config.SecurityTestConfig;
import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.entity.Role;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.CourseStatus;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.ResourceNotFoundException;
import com.online.course.management.project.filter.JwtAuthenticationFilter;
import com.online.course.management.project.security.CustomUserDetails;
import com.online.course.management.project.security.annotation.WithMockCustomUser;
import com.online.course.management.project.service.interfaces.ICourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourseController.class)
@Import({SecurityTestConfig.class, RoleAuthorizationAspect.class})
@AutoConfigureMockMvc(addFilters = false)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICourseService courseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private CourseDTOS.CourseDetailsResponseDto testCourseResponse;
    private CourseDTOS.CreateCourseRequestDTO createCourseRequest;
    private CourseDTOS.UpdateCourseRequestDTO updateCourseRequest;
    private CourseDTOS.InstructorDetailsDto instructorDetails;

    @BeforeEach
    void setUp() {
        // Setup instructor details
        instructorDetails = new CourseDTOS.InstructorDetailsDto();
        instructorDetails.setId(1L);
        instructorDetails.setUsername("instructor");
        instructorDetails.setEmail("instructor@test.com");
        instructorDetails.setRealName("Test Instructor");
        instructorDetails.setRoles(Set.of("INSTRUCTOR"));

        // Setup course response
        testCourseResponse = new CourseDTOS.CourseDetailsResponseDto();
        testCourseResponse.setId(1L);
        testCourseResponse.setTitle("Test Course");
        testCourseResponse.setDescription("Test Description");
        testCourseResponse.setStatus(CourseStatus.DRAFT);
        testCourseResponse.setInstructor(instructorDetails);
        testCourseResponse.setCategoryNames(Set.of("Test Category"));
        testCourseResponse.setCreatedAt(LocalDateTime.now());
        testCourseResponse.setUpdatedAt(LocalDateTime.now());

        // Setup create course request
        createCourseRequest = new CourseDTOS.CreateCourseRequestDTO();
        createCourseRequest.setTitle("New Course");
        createCourseRequest.setDescription("New Description");
        createCourseRequest.setInstructorId(1L);
        createCourseRequest.setCategoryIds(Set.of(1L));
        createCourseRequest.setStatus(CourseStatus.DRAFT);

        // Setup update course request
        updateCourseRequest = new CourseDTOS.UpdateCourseRequestDTO();
        updateCourseRequest.setTitle("Updated Course");
        updateCourseRequest.setDescription("Updated Description");
        updateCourseRequest.setStatus(CourseStatus.PUBLISHED);
    }

    private ResultActions performRequestWithRole(String roleType, String method, String url, Object content) throws Exception {
        // Create Role entity
        Role role = new Role();
        role.setName(RoleType.valueOf(roleType));

        // Create User entity with role
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(roleType.toLowerCase());
        mockUser.setEmail(roleType.toLowerCase() + "@example.com");
        mockUser.addRole(role);

        // Create CustomUserDetails
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);

        // Create Authentication with both principal and authorities
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + roleType))
        );

        // Set up SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Perform request based on method type
        switch (method.toUpperCase()) {
            case "POST":
                return mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content != null ? objectMapper.writeValueAsString(content) : ""));
            case "PUT":
                return mockMvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content != null ? objectMapper.writeValueAsString(content) : ""));
            case "PATCH":
                return mockMvc.perform(patch(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content != null ? objectMapper.writeValueAsString(content) : ""));
            case "DELETE":
                return mockMvc.perform(delete(url));
            default:
                return mockMvc.perform(get(url));
        }
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void createCourse_Success() throws Exception {
        when(courseService.createCourse(any(CourseDTOS.CreateCourseRequestDTO.class)))
                .thenReturn(testCourseResponse);

        mockMvc.perform(post("/api/v1/courses/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCourseRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testCourseResponse.getId()))
                .andExpect(jsonPath("$.title").value(testCourseResponse.getTitle()))
                .andExpect(jsonPath("$.status").value(testCourseResponse.getStatus().name()));

        verify(courseService).createCourse(any(CourseDTOS.CreateCourseRequestDTO.class));
    }

    @Test
    void createCourse_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/courses/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCourseRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(courseService, never()).createCourse(any());
    }

    @Test
    @WithMockCustomUser(roles = "USER")
    void createCourse_Forbidden() throws Exception {
        mockMvc.perform(post("/api/v1/courses/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCourseRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(courseService, never()).createCourse(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN", "INSTRUCTOR"})
    void updateCourse_Success() throws Exception {
        when(courseService.updateCourse(eq(1L), any(CourseDTOS.UpdateCourseRequestDTO.class)))
                .thenReturn(testCourseResponse);

        mockMvc.perform(put("/api/v1/courses/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCourseRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCourseResponse.getId()))
                .andExpect(jsonPath("$.title").value(testCourseResponse.getTitle()));

        verify(courseService).updateCourse(eq(1L), any(CourseDTOS.UpdateCourseRequestDTO.class));
    }

    @Test
    @WithMockCustomUser(roles = "ADMIN")
    void updateCourse_NotFound() throws Exception {
        when(courseService.updateCourse(eq(999L), any(CourseDTOS.UpdateCourseRequestDTO.class)))
                .thenThrow(new ResourceNotFoundException("Course not found"));

        mockMvc.perform(put("/api/v1/courses/999/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCourseRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found"));
    }

    @Test
    @WithMockCustomUser(roles = "ADMIN")
    void archiveCourse_Success() throws Exception {
        doNothing().when(courseService).archiveCourse(1L);

        mockMvc.perform(patch("/api/v1/courses/1/archive"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Course archived successfully"));

        verify(courseService).archiveCourse(1L);
    }

    @Test
    @WithMockCustomUser(roles = "ADMIN")
    void unarchiveCourse_Success() throws Exception {
        doNothing().when(courseService).unarchiveCourse(1L);

        mockMvc.perform(patch("/api/v1/courses/1/unarchive"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("Course unarchived successfully"));

        verify(courseService).unarchiveCourse(1L);
    }

    @Test
    void getCourseById_Success() throws Exception {
        when(courseService.getCourseById(1L)).thenReturn(testCourseResponse);

        mockMvc.perform(post("/api/v1/courses/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCourseResponse.getId()))
                .andExpect(jsonPath("$.title").value(testCourseResponse.getTitle()));

        verify(courseService).getCourseById(1L);
    }

    @Test
    void getCourseById_NotFound() throws Exception {
        when(courseService.getCourseById(999L))
                .thenThrow(new ResourceNotFoundException("Course not found"));

        mockMvc.perform(post("/api/v1/courses/999"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found"));
    }

    @Test
    void searchCourses_Success() throws Exception {
        List<CourseDTOS.CourseDetailsResponseDto> courses = Collections.singletonList(testCourseResponse);

        CourseDTOS.SearchCourseRequestDTO searchRequest = new CourseDTOS.SearchCourseRequestDTO();
        searchRequest.setTitle("Test");
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        var coursePage = new PageImpl<>(courses, searchRequest.toPageable(), courses.size());

        when(courseService.searchCourses(any(CourseDTOS.SearchCourseRequestDTO.class), any(Pageable.class)))
                .thenReturn(coursePage);

        mockMvc.perform(post("/api/v1/courses/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void searchCoursesByInstructor_Success() throws Exception {
        List<CourseDTOS.CourseDetailsResponseDto> courses = Collections.singletonList(testCourseResponse);

        CourseDTOS.SearchInstructorCourseRequestDTO searchRequest = new CourseDTOS.SearchInstructorCourseRequestDTO();
        searchRequest.setInstructorId(1L);
        searchRequest.setIncludeArchived(false);
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        var coursePage = new PageImpl<>(courses, searchRequest.toPageable(), courses.size());

        when(courseService.getCoursesByInstructor(eq(1L), eq(false), any()))
                .thenReturn(coursePage);

        mockMvc.perform(post("/api/v1/courses/search-instructor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void searchCoursesByStatus_Success() throws Exception {
        List<CourseDTOS.CourseDetailsResponseDto> courses = Collections.singletonList(testCourseResponse);


        CourseDTOS.SearchStatusRequestDTO searchRequest = new CourseDTOS.SearchStatusRequestDTO();
        searchRequest.setStatus("PUBLISHED");
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        var coursePage = new PageImpl<>(courses, searchRequest.toPageable(), courses.size());

        when(courseService.getCoursesByStatus(eq(CourseStatus.PUBLISHED), any()))
                .thenReturn(coursePage);

        mockMvc.perform(post("/api/v1/courses/search-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void searchCoursesByStatus_InvalidStatus() throws Exception {
        CourseDTOS.SearchStatusRequestDTO searchRequest = new CourseDTOS.SearchStatusRequestDTO();
        searchRequest.setStatus("INVALID_STATUS");
        searchRequest.setPage(1);
        searchRequest.setLimit(10);

        mockMvc.perform(post("/api/v1/courses/search-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No enum constant com.online.course.management.project.enums.CourseStatus.INVALID_STATUS"));

        verify(courseService, never()).getCoursesByStatus(any(), any());
    }

    @Test
    void getLatestCourses_Success() throws Exception {
        List<CourseDTOS.CourseDetailsResponseDto> courses = Collections.singletonList(testCourseResponse);

        CourseDTOS.SearchLatestCoursesRequestDTO searchRequest = new CourseDTOS.SearchLatestCoursesRequestDTO();
        searchRequest.setLimit(10);

        when(courseService.getLatestCourses(10)).thenReturn(courses);

        mockMvc.perform(post("/api/v1/courses/search-latest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(testCourseResponse.getId()))
                .andExpect(jsonPath("$[0].title").value(testCourseResponse.getTitle()));
    }

    @Test
    void searchCourses_WithInvalidPagination() throws Exception {
        CourseDTOS.SearchCourseRequestDTO searchRequest = new CourseDTOS.SearchCourseRequestDTO();
        searchRequest.setPage(0); // Invalid page number
        searchRequest.setLimit(10);

        mockMvc.perform(post("/api/v1/courses/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("page: Page index must not be less than one"));
    }

    @Test
    void searchCourses_WithInvalidDateRange() throws Exception {
        CourseDTOS.SearchCourseRequestDTO searchRequest = new CourseDTOS.SearchCourseRequestDTO();
        searchRequest.setPage(1);
        searchRequest.setLimit(10);
        searchRequest.setFromDate(LocalDateTime.now().plusDays(1));
        searchRequest.setToDate(LocalDateTime.now()); // ToDate before FromDate

        mockMvc.perform(post("/api/v1/courses/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("dateRangeValid: toDate must be after fromDate"));
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void updateCourse_ValidationError() throws Exception {
        updateCourseRequest.setTitle(""); // Empty title should fail validation

        mockMvc.perform(put("/api/v1/courses/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCourseRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0]").value("title: Title is required"));

        verify(courseService, never()).updateCourse(any(), any());
    }

    @Test
    @WithMockCustomUser(roles = {"USER"})
    void archiveCourse_ForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(patch("/api/v1/courses/1/archive"))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User does not have the required role"));

        verify(courseService, never()).archiveCourse(any());
    }

    @Test
    @WithMockCustomUser(roles = {"ADMIN"})
    void archiveCourse_NotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Course not found"))
                .when(courseService).archiveCourse(999L);

        mockMvc.perform(patch("/api/v1/courses/999/archive"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found"));
    }

    @Test
    @WithMockCustomUser(roles = {"INSTRUCTOR"})
    void updateCourse_InstructorCanUpdateOwnCourse() throws Exception {
        when(courseService.updateCourse(eq(1L), any(CourseDTOS.UpdateCourseRequestDTO.class)))
                .thenReturn(testCourseResponse);

        mockMvc.perform(put("/api/v1/courses/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCourseRequest)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(courseService).updateCourse(eq(1L), any(CourseDTOS.UpdateCourseRequestDTO.class));
    }

    @Test
    @WithMockCustomUser(roles = {"INSTRUCTOR"})
    void updateCourse_InstructorCannotUpdateOthersCourse() throws Exception {
        when(courseService.updateCourse(eq(1L), any(CourseDTOS.UpdateCourseRequestDTO.class)))
                .thenThrow(new ForbiddenException("You don't have permission to modify this course"));

        mockMvc.perform(put("/api/v1/courses/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCourseRequest)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You don't have permission to modify this course"));
    }
}