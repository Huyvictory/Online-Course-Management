package com.online.course.management.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.aspect.RoleAuthorizationAspect;
import com.online.course.management.project.config.SecurityTestConfig;
import com.online.course.management.project.constants.UserConstants;
import com.online.course.management.project.dto.PaginationDto;
import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.entity.Role;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.entity.UserRole;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.enums.UserStatus;
import com.online.course.management.project.filter.JwtAuthenticationFilter;
import com.online.course.management.project.mapper.UserMapper;
import com.online.course.management.project.security.CustomUserDetails;
import com.online.course.management.project.security.JwtUtil;
import com.online.course.management.project.security.annotation.WithMockCustomUser;
import com.online.course.management.project.service.interfaces.IUserService;
import com.online.course.management.project.utils.user.UserControllerUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityTestConfig.class, RoleAuthorizationAspect.class})
@AutoConfigureMockMvc(addFilters = false)
@Slf4j
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUserService userService;

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserControllerUtils userControllerUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UserDTOs.UserRegistrationDto registrationDto;
    private UserDTOs.UserResponseDto userResponseDto;
    private UserDTOs.UserWithRolesResponseDto userWithRolesResponseDto;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRealName("Test User");
        testUser.setPasswordHash("hashedPassword");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // Setup user roles
        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleType.USER);
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(new UserRole(testUser, userRole));
        testUser.setUserRoles(userRoles);

        // Setup registration DTO
        registrationDto = new UserDTOs.UserRegistrationDto();
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password123");

        // Setup response DTOs
        userResponseDto = new UserDTOs.UserResponseDto();
        userResponseDto.setId(testUser.getId());
        userResponseDto.setUsername(testUser.getUsername());
        userResponseDto.setEmail(testUser.getEmail());
        userResponseDto.setRealName(testUser.getRealName());
        userResponseDto.setStatus(testUser.getStatus().name());

        userWithRolesResponseDto = new UserDTOs.UserWithRolesResponseDto();
        userWithRolesResponseDto.setId(testUser.getId());
        userWithRolesResponseDto.setUsername(testUser.getUsername());
        userWithRolesResponseDto.setEmail(testUser.getEmail());
        userWithRolesResponseDto.setRealName(testUser.getRealName());
        userWithRolesResponseDto.setStatus(testUser.getStatus().name());
        userWithRolesResponseDto.setRoles(Set.of("USER"));
    }

    @Test
    void registerUser_ShouldSucceed() throws Exception {
        when(userService.registerUser(any(UserDTOs.UserRegistrationDto.class)))
                .thenReturn(userResponseDto);

        mockMvc.perform(post(UserConstants.BASE_PATH + UserConstants.REGISTER_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(testUser.getEmail()));

        verify(userService).registerUser(any(UserDTOs.UserRegistrationDto.class));
    }

    @Test
    void loginUser_ShouldSucceed() throws Exception {
        UserDTOs.UserLoginDto loginDto = new UserDTOs.UserLoginDto();
        loginDto.setUsernameOrEmail("test@example.com");
        loginDto.setPassword("password123");

        CustomUserDetails userDetails = new CustomUserDetails(testUser);
        Authentication mockAuth = new UsernamePasswordAuthenticationToken(userDetails, null);
        String jwtToken = "test.jwt.token";

        when(userControllerUtils.authenticate(any(), any())).thenReturn(mockAuth);
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn(jwtToken);

        mockMvc.perform(post(UserConstants.BASE_PATH + UserConstants.LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(jwtToken));

        verify(userControllerUtils).authenticate(any(), any());
        verify(jwtUtil).generateToken(any(CustomUserDetails.class));
    }

    @Test
    @WithMockCustomUser(username = "admin@example.com", roles = {"ADMIN"})
    void getUserById_ShouldSucceed() throws Exception {
        when(userService.getUserById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userMapper.toDto(testUser)).thenReturn(userResponseDto);

        mockMvc.perform(get(UserConstants.BASE_PATH + "/{id}", testUser.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId()));

        verify(userService).getUserById(testUser.getId());
        verify(userMapper).toDto(testUser);
    }

    @Test
    @WithMockCustomUser(username = "admin@example.com", roles = {"ADMIN"})
    void getUserById_NotFound_ShouldReturn404() throws Exception {
        when(userService.getUserById(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get(UserConstants.BASE_PATH + "/{id}", 999L))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(userService).getUserById(anyLong());
    }

    @Test
    @WithMockCustomUser(username = "test@example.com", roles = {"USER"})
    void updateUserProfile_ShouldSucceed() throws Exception {
        UserDTOs.UpdateProfileDto updateProfileDto = new UserDTOs.UpdateProfileDto();
        updateProfileDto.setUsername("newusername");
        updateProfileDto.setEmail("newemail@example.com");

        when(userService.updateUserProfile(eq(1L), any(UserDTOs.UpdateProfileDto.class)))
                .thenReturn(userResponseDto);

        mockMvc.perform(put(UserConstants.BASE_PATH + UserConstants.PROFILE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(userResponseDto.getEmail()));

        verify(userService).updateUserProfile(eq(1L), any(UserDTOs.UpdateProfileDto.class));
    }

    @Test
    @WithMockCustomUser(username = "admin@example.com", roles = {"ADMIN"})
    void searchUsers_ShouldSucceed() throws Exception {
        PaginationDto.PaginationRequestDto paginationRequestDto = new PaginationDto.PaginationRequestDto();
        paginationRequestDto.setPage(1);
        paginationRequestDto.setLimit(10);

        UserDTOs.UserSearchRequestDto searchRequest = new UserDTOs.UserSearchRequestDto();
        searchRequest.setUsername("test");

        List<UserDTOs.UserWithRolesResponseDto> users = Collections.singletonList(userWithRolesResponseDto);
        Page<UserDTOs.UserWithRolesResponseDto> page = new PageImpl<>(users);

        when(userService.searchUsers(any(), any())).thenReturn(page);
        when(userService.countUsers(any())).thenReturn(1L);

        mockMvc.perform(get(UserConstants.BASE_PATH + UserConstants.SEARCH_PATH)
                        .param("username", "test")
                        .param("page", "1")
                        .param("limit", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.total").value(1));

        verify(userService).searchUsers(any(), any());
        verify(userService).countUsers(any());
    }

    @Test
    @WithMockCustomUser(username = "admin@example.com", roles = {"ADMIN"})
    void updateUserRoles_ShouldSucceed() throws Exception {
        UserDTOs.UpdateUserRolesDto updateRolesDto = new UserDTOs.UpdateUserRolesDto();
        updateRolesDto.setRoles(Set.of("USER", "INSTRUCTOR"));

        when(userService.updateUserRoles(eq(1L), any(), any())).thenReturn(Set.of("USER", "INSTRUCTOR"));

        mockMvc.perform(put(UserConstants.BASE_PATH + UserConstants.UPDATE_ROLES_PATH, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRolesDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Roles updated successfully"))
                .andExpect(jsonPath("$.updatedRoles").isArray());

        verify(userService).updateUserRoles(eq(1L), any(), any());
    }

    @Test
    @WithMockCustomUser(username = "admin@example.com", roles = {"ADMIN"})
    void deleteUser_ShouldSucceed() throws Exception {
        mockMvc.perform(delete(UserConstants.BASE_PATH + "/{id}", 1L))
                .andDo(print())
                .andExpect(status().isNoContent());

        verify(userService).softDeleteUser(1L);
    }

    @Test
    @WithMockCustomUser(username = "admin@example.com", roles = {"USER"})
    void deleteUser_Forbidden() throws Exception {
        mockMvc.perform(delete(UserConstants.BASE_PATH + "/{id}", 999L))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(userService, never()).softDeleteUser(anyLong());
    }
}