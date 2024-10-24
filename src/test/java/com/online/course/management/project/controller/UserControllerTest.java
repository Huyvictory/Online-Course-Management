package com.online.course.management.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.online.course.management.project.aspect.RoleAuthorizationAspect;
import com.online.course.management.project.config.SecurityTestConfig;
import com.online.course.management.project.dto.PaginationDto;
import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.entity.Role;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.mapper.UserMapper;
import com.online.course.management.project.security.CustomUserDetails;
import com.online.course.management.project.security.CustomUserDetailsService;
import com.online.course.management.project.security.JwtUtil;
import com.online.course.management.project.service.interfaces.IUserService;
import com.online.course.management.project.utils.user.UserControllerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityTestConfig.class, RoleAuthorizationAspect.class})  // Add RoleAuthorizationAspect
@EnableAspectJAutoProxy(proxyTargetClass = true)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUserService userService;

    @MockBean
    private UserMapper userMapper;


    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserControllerUtils userControllerUtils;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    private ResultActions performDeleteWithRole(Long userId, String roleType) throws Exception {
        // Create Role entity
        Role role = new Role();
        role.setName(RoleType.valueOf(roleType));

        // Create User entity with role
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername(roleType.toLowerCase());
        mockUser.setEmail(roleType.toLowerCase() + "@example.com");
        mockUser.addRole(role);  // Important: Add role to user entity

        // Create CustomUserDetails
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);

        // Create Authentication with both principal and authorities
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + roleType)
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,  // This must be CustomUserDetails with proper User entity
                null,
                authorities
        );

        // Set up SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Perform request with authentication
        return mockMvc.perform(delete("/api/v1/users/{id}", userId)
                .with(authentication(auth)));
    }

    @Test
    void registerUser_Success() throws Exception {
        // Arrange
        UserDTOs.UserRegistrationDto registrationDto = new UserDTOs.UserRegistrationDto();
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password123");

        UserDTOs.UserResponseDto responseDto = new UserDTOs.UserResponseDto();
        responseDto.setId(1L);
        responseDto.setEmail("test@example.com");
        responseDto.setUsername("test");

        when(userService.registerUser(any(UserDTOs.UserRegistrationDto.class)))
                .thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void loginUser_Success() throws Exception {
        // Arrange
        UserDTOs.UserLoginDto loginDto = new UserDTOs.UserLoginDto();
        loginDto.setUsernameOrEmail("test@example.com");
        loginDto.setPassword("password123");

        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null);
        String token = "test-jwt-token";

        when(userControllerUtils.authenticate(anyString(), anyString())).thenReturn(authentication);
        when(jwtUtil.generateToken(any(CustomUserDetails.class))).thenReturn(token);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(token));
    }

    @Test
    void updateUserRoles_Success() throws Exception {
        // Arrange
        Long userId = 1L;
        Set<String> roles = new HashSet<>();
        roles.add("USER");
        roles.add("INSTRUCTOR");

        UserDTOs.UpdateUserRolesDto updateRolesDto = new UserDTOs.UpdateUserRolesDto();
        updateRolesDto.setRoles(roles);

        Role adminRole = new Role();
        adminRole.setName(RoleType.ADMIN);

        // Create CustomUserDetails instead of using default User
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("admin");
        mockUser.setEmail("admin@example.com");
        mockUser.addRole(adminRole);
        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // Mock the SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Mock the validateRoles method
        when(userControllerUtils.validateRoles(anySet())).thenReturn(
                new HashSet<>(Arrays.asList(RoleType.USER, RoleType.INSTRUCTOR))
        );

        // Mock the service call
        when(userService.updateUserRoles(eq(userId), anySet(), any())).thenReturn(roles);

        // Act & Assert
        mockMvc.perform(put("/api/v1/users/{id}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRolesDto))
                        .with(authentication(auth))) // Add authentication to the request
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Roles updated successfully"))
                .andExpect(jsonPath("$.updatedRoles").isArray())
                .andExpect(jsonPath("$.updatedRoles", containsInAnyOrder("USER", "INSTRUCTOR")));

        verify(userService).updateUserRoles(eq(userId), any(), eq(1L));
    }


    @Test
    void deleteUser_RoleBasedAccess() throws Exception {
        Long userId = 1L;
        doNothing().when(userService).softDeleteUser(userId);

        // Test ADMIN access
        performDeleteWithRole(userId, "ADMIN")
                .andExpect(status().isNoContent());

        // Test USER access
        performDeleteWithRole(userId, "USER")
                .andExpect(status().isForbidden());

        // Test INSTRUCTOR access
        performDeleteWithRole(userId, "INSTRUCTOR")
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_Unauthorized() throws Exception {
        // Arrange
        Long userId = 1L;

        // Act & Assert
        mockMvc.perform(delete("/api/v1/users/{id}", userId)
                        .header("Authorization", "Bearer invalid-token"))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(userService, never()).softDeleteUser(anyLong());
    }

    @Test
    void getAllUsers_Success() throws Exception {
        // Arrange

        Role adminRole = new Role();
        adminRole.setName(RoleType.ADMIN);

        // Create CustomUserDetails instead of using default User
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("admin");
        mockUser.setEmail("admin@example.com");
        mockUser.addRole(adminRole);

        CustomUserDetails userDetails = new CustomUserDetails(mockUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // Mock the SecurityContext
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        // Mock pagination request
        PaginationDto.PaginationRequestDto paginationRequest = new PaginationDto.PaginationRequestDto();
        paginationRequest.setPage(1);
        paginationRequest.setLimit(10);

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(userService.getAllUsers(any()))
                .thenReturn(new PageImpl<>(new ArrayList<>(), pageRequest, 0));
        when(userService.countUsers(any())).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/users/all")
                        .param("page", "1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }
}