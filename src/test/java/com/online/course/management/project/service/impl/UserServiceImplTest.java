package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.UserDTOs;

import com.online.course.management.project.entity.Role;
import com.online.course.management.project.entity.User;

import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.enums.UserStatus;
import com.online.course.management.project.exception.business.ForbiddenException;
import com.online.course.management.project.exception.business.account.EmailAlreadyExistsException;
import com.online.course.management.project.mapper.UserMapper;

import com.online.course.management.project.repository.IRoleRepository;
import com.online.course.management.project.repository.IUserRepository;
import com.online.course.management.project.utils.user.UserServiceUtils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class UserServiceImplTest {

    @Mock
    private IUserRepository userRepository;
    @Mock
    private IRoleRepository roleRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserServiceUtils userServiceUtils;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerUser_SuccessfulRegistration() {
        UserDTOs.UserRegistrationDto registrationDto = new UserDTOs.UserRegistrationDto();
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setStatus(UserStatus.ACTIVE);

        UserDTOs.UserResponseDto userResponseDto = new UserDTOs.UserResponseDto();
        userResponseDto.setId(1L);
        userResponseDto.setEmail("test@example.com");
        userResponseDto.setUsername("test");
        userResponseDto.setStatus("ACTIVE");

        when(userServiceUtils.createUserFromDto(any())).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userResponseDto);

        UserDTOs.UserResponseDto result = userService.registerUser(registrationDto);

        assertNotNull(result);
        assertEquals("test@example.com", result.getEmail());
        assertEquals("test", result.getUsername());
        verify(userServiceUtils).validateNewUser(registrationDto);
        verify(userServiceUtils).createUserFromDto(registrationDto);
        verify(userServiceUtils).assignDefaultRole(user);
        verify(userRepository).save(user);
    }

    @Test
    void registerUser_EmailAlreadyExists() {
        UserDTOs.UserRegistrationDto registrationDto = new UserDTOs.UserRegistrationDto();
        registrationDto.setEmail("existing@example.com");
        registrationDto.setPassword("password123");

        doThrow(new EmailAlreadyExistsException("Email already exists"))
                .when(userServiceUtils).validateNewUser(any());

        assertThrows(EmailAlreadyExistsException.class, () -> userService.registerUser(registrationDto));
    }

    @Test
    void updateUserRoles_AssignAdminToNonAdmin_ThrowsForbiddenException() {
        Long userId = 1L;
        Long currentUserId = 2L;
        Set<RoleType> newRoles = new HashSet<>(Collections.singletonList(RoleType.ADMIN));

        User normalUser = new User();
        normalUser.setId(userId);

        User adminUser = new User();
        adminUser.setId(currentUserId);
        Role adminRole = new Role();
        adminRole.setName(RoleType.ADMIN);
        adminUser.addRole(adminRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(normalUser));
//        when(userRepository.findById(currentUserId)).thenReturn(Optional.of(adminUser));

        assertThrows(ForbiddenException.class, () -> userService.updateUserRoles(userId, newRoles, currentUserId));
    }

    @Test
    void updateUserRoles_RemoveAdminFromInitialAdmin_ThrowsForbiddenException() {
        Long userId = 1L;
        Long currentUserId = 2L;
        Set<RoleType> newRoles = new HashSet<>(Collections.singletonList(RoleType.USER));

        User initialAdminUser = new User();
        initialAdminUser.setId(userId);
        initialAdminUser.setEmail("admin@gmail.com");
        Role adminRole = new Role();
        adminRole.setName(RoleType.ADMIN);
        initialAdminUser.addRole(adminRole);

        User adminUser = new User();
        adminUser.setId(currentUserId);
        adminUser.addRole(adminRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(initialAdminUser));

        assertThrows(ForbiddenException.class, () -> userService.updateUserRoles(userId, newRoles, currentUserId));
    }

    @Test
    void updateUserRoles_AdminRemovingOwnAdminRole_ThrowsForbiddenException() {
        Long userId = 2L;
        Set<RoleType> newRoles = new HashSet<>(Collections.singletonList(RoleType.USER));

        User adminUser = new User();
        adminUser.setId(userId);
        Role adminRole = new Role();
        adminRole.setName(RoleType.ADMIN);
        adminUser.addRole(adminRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(adminUser));

        assertThrows(ForbiddenException.class, () -> userService.updateUserRoles(userId, newRoles, userId));
    }


    @Test
    void updateUserRoles_SuccessfulUpdate() {
        Long userId = 1L;
        Long currentUserId = 2L;
        Set<RoleType> newRoles = new HashSet<>(Arrays.asList(RoleType.USER, RoleType.INSTRUCTOR));

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName(RoleType.USER);

        Role instructorRole = new Role();
        instructorRole.setId(2L);
        instructorRole.setName(RoleType.INSTRUCTOR);

        Role adminRole = new Role();
        adminRole.setId(3L);
        adminRole.setName(RoleType.ADMIN);

        User normalUser = new User();
        normalUser.setId(userId);
        normalUser.addRole(userRole);

        User adminUser = new User();
        adminUser.setId(currentUserId);
        adminUser.addRole(adminRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(normalUser));
        //when(userRepository.findById(currentUserId)).thenReturn(Optional.of(adminUser));
        when(roleRepository.findByName(RoleType.USER)).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName(RoleType.INSTRUCTOR)).thenReturn(Optional.of(instructorRole));

        // This is the key change
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            // Clear existing roles and add new ones to simulate actual persistence
            savedUser.getUserRoles().clear();
            for (RoleType role : newRoles) {
                if (role == RoleType.USER) {
                    savedUser.addRole(userRole);
                }
                if (role == RoleType.INSTRUCTOR) {
                    savedUser.addRole(instructorRole);
                }
            }
            return savedUser;
        });

        Set<String> result = userService.updateUserRoles(userId, newRoles, currentUserId);

        log.info("Updated roles: {}", result);

        assertEquals(2, result.size(), "Expected 2 roles, but got: " + result.size() + " roles: " + result);
        assertTrue(result.contains("USER"), "USER role is missing from the result");
        assertTrue(result.contains("INSTRUCTOR"), "INSTRUCTOR role is missing from the result");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void getAllUsers_SuccessfulRetrieval() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setStatus(UserStatus.ACTIVE);

        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);
        when(userMapper.toUserWithRolesDto(any(User.class))).thenReturn(new UserDTOs.UserWithRolesResponseDto());

        Page<UserDTOs.UserWithRolesResponseDto> result = userService.getAllUsers(Pageable.unpaged());

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getUserById_UserFound() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserById(1L);

        assertTrue(result.isPresent());
        assertEquals(user, result.get());
    }

    @Test
    void getUserById_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserById(1L);

        assertFalse(result.isPresent());
    }

    @Test
    void softDeleteUser_SuccessfulDelete() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.softDeleteUser(1L);

        verify(userRepository).save(user);
        assertNotNull(user.getDeletedAt());
    }

    @Test
    void softDeleteUser_AdminUser() {
        User adminUser = new User();
        adminUser.setId(1L);
        Role adminRole = new Role();
        adminRole.setName(RoleType.ADMIN);
        adminUser.addRole(adminRole);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        assertThrows(ForbiddenException.class, () -> userService.softDeleteUser(1L));
    }
}
