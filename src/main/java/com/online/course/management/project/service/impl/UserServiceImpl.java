package com.online.course.management.project.service.impl;

import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.entity.Role;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.exception.ForbiddenException;
import com.online.course.management.project.exception.ResourceNotFoundException;
import com.online.course.management.project.mapper.UserMapper;
import com.online.course.management.project.repository.IRoleRepository;
import com.online.course.management.project.repository.IUserRepository;
import com.online.course.management.project.service.interfaces.IUserService;
import com.online.course.management.project.utils.user.UserServiceUtils;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final UserServiceUtils userServiceUtils;

    @Autowired
    public UserServiceImpl(IUserRepository userRepository, IRoleRepository roleRepository, PasswordEncoder passwordEncoder, UserMapper userMapper, UserServiceUtils userServiceUtils) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userServiceUtils = userServiceUtils;
    }

    @Override
    @Transactional
    public UserDTOs.UserResponseDto registerUser(UserDTOs.UserRegistrationDto registrationDto) {
        log.info("Registering new user with email: {}", registrationDto.getEmail());
        userServiceUtils.validateNewUser(registrationDto);
        User user = userServiceUtils.createUserFromDto(registrationDto);
        userServiceUtils.assignDefaultRole(user);
        User savedUser = userRepository.save(user);

        log.info("User registered successfully with id: {}", savedUser.getId());
        return userMapper.toDto(savedUser);
    }


    /**
     * @param userId
     * @param updateProfileDto
     * @return
     */
    @Override
    @Transactional
    public UserDTOs.UserResponseDto updateUserProfile(Long userId, UserDTOs.UpdateProfileDto updateProfileDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userMapper.updateUserFromDto(updateProfileDto, user);

        if (updateProfileDto.getPassword() != null) {

            if (user.getUserRoles().stream().anyMatch(role -> role.getRole().getName() == RoleType.ADMIN)) {
                throw new ForbiddenException("Can not change password for admin user");
            }

            user.setPasswordHash(passwordEncoder.encode(updateProfileDto.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    /**
     * @param userId
     * @param newRoles
     * @param currentUserId
     */
    @Override
    @Transactional
    public Set<String> updateUserRoles(Long userId, Set<RoleType> newRoles, Long currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        boolean isInitialAdmin = "admin@gmail.com".equals(user.getEmail());
        boolean isCurrentlyAdmin = user.getUserRoles().stream()
                .anyMatch(userRole -> userRole.getRole().getName() == RoleType.ADMIN);
        boolean isAttemptingToAssignAdminRole = newRoles.contains(RoleType.ADMIN);

        // Prevent assigning ADMIN role to non-admin users
        if (!isCurrentlyAdmin && isAttemptingToAssignAdminRole) {
            throw new ForbiddenException("Cannot assign ADMIN role to a non-admin user");
        }

        boolean isRemovingAdminRole = isCurrentlyAdmin && !isAttemptingToAssignAdminRole;

        // Prevent removing ADMIN role from the initial admin account
        if (isInitialAdmin && isRemovingAdminRole) {
            throw new ForbiddenException("Cannot remove ADMIN role from the initial admin account");
        }

        // If the current user is updating their own roles
        if (userId.equals(currentUserId)) {
            // Prevent users from removing their own ADMIN role
            if (isRemovingAdminRole) {
                throw new ForbiddenException("You cannot remove your own ADMIN role");
            }
        }

        Set<Role> rolesToSet = newRoles.stream()
                .map(roleType -> roleRepository.findByName(roleType)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleType)))
                .collect(Collectors.toSet());

        user.getUserRoles().clear();
        rolesToSet.forEach(user::addRole);

        userRepository.save(user);

        return user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName().name())
                .collect(Collectors.toSet());
    }

    /**
     * @param pageable
     * @return
     */
    @Override
    public Page<UserDTOs.UserWithRolesResponseDto> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toUserWithRolesDto);
    }


    @Override
    public Page<UserDTOs.UserWithRolesResponseDto> searchUsers(UserDTOs.UserSearchRequestDto searchUsersPayload, Pageable pageable) {
        Specification<User> querySpecification = userServiceUtils.createSpecification(searchUsersPayload);

        Page<User> users = userRepository.findAll(querySpecification, pageable);
        return users.map(userMapper::toUserWithRolesDto);
    }

    @Override
    public long countUsers(Optional<UserDTOs.UserSearchRequestDto> searchUsersPayload) {
        if (searchUsersPayload.isPresent()) {
            Specification<User> querySpecification = userServiceUtils.createSpecification(searchUsersPayload.get());
            return userRepository.count(querySpecification);
        } else {
            return userRepository.count();
        }
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    public Optional<User> getUserById(Long id) {
        log.debug("Fetching user with id: {}", id);
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user) {
        log.info("Updating user with id: {}", user.getId());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void softDeleteUser(Long id) {


        userRepository.findById(id).ifPresent(user -> {

            if (user.getUserRoles().stream().anyMatch(role -> role.getRole().getName() == RoleType.ADMIN)) {
                throw new ForbiddenException("Can not delete admin user");
            }

            user.setDeletedAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
