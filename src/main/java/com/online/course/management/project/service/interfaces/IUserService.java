package com.online.course.management.project.service.interfaces;

import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IUserService {
    UserDTOs.UserResponseDto registerUser(UserDTOs.UserRegistrationDto userRegistrationDto);

    Optional<User> getUserById(Long id);

    Optional<User> getUserByUsername(String username);

    Optional<User> getUserByEmail(String email);

    User updateUser(User user);

    void softDeleteUser(Long id);

    Page<User> searchUsers(String username, String name, String status, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean authenticateUser(String usernameOrEmail, String password);
}
