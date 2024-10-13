package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

public interface IUserService {
    UserDTOs.UserResponseDto registerUser(UserDTOs.UserRegistrationDto userRegistrationDto);

    Optional<User> getUserById(Long id);

    Optional<User> getUserByUsername(String username);

    Optional<User> getUserByEmail(String email);

    User updateUser(User user);

    void softDeleteUser(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean authenticateUser(String usernameOrEmail, String password);

    UserDTOs.UserResponseDto updateUserProfile(Long userId, UserDTOs.UpdateProfileDto updateProfileDto);

    Set<String> updateUserRoles(Long userId, Set<RoleType> roleNames, Long currentUserId);

    Page<UserDTOs.UserWithRolesResponseDto> getAllUsers(Pageable pageable);

    Page<UserDTOs.UserWithRolesResponseDto> searchUsers(
            UserDTOs.UserSearchRequestDto searchUsersPayload,
            Pageable pageable
    );

    long countUsers(Optional<UserDTOs.UserSearchRequestDto> searchUsersPayload);
}
