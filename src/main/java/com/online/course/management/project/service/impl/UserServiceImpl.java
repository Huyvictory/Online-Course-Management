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
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Autowired
    public UserServiceImpl(IUserRepository userRepository, IRoleRepository roleRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserDTOs.UserResponseDto registerUser(UserDTOs.UserRegistrationDto registrationDto) {
        log.info("Registering new user with email: {}", registrationDto.getEmail());

        validateNewUser(registrationDto);
        User user = createUserFromDto(registrationDto);
        assignDefaultRole(user);
        User savedUser = userRepository.save(user);

        log.info("User registered successfully with id: {}", savedUser.getId());
        return userMapper.toDto(savedUser);
    }

    private void validateNewUser(UserDTOs.UserRegistrationDto registrationDto) {
        if (existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private User createUserFromDto(UserDTOs.UserRegistrationDto registrationDto) {
        User user = userMapper.toEntity(registrationDto);
        user.setUsername(generateUsernameFromEmail(registrationDto.getEmail()));
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        return user;
    }

    private void assignDefaultRole(User user) {
        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default user role not found"));
        user.addRole(userRole);
    }

    private String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int suffix = 1;

        while (existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }

        return username;
    }

    @Override
    public boolean authenticateUser(String usernameOrEmail, String password) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return passwordEncoder.matches(password, user.getPasswordHash());
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

        userMapper.updateUserFromDto(user, updateProfileDto);

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
    public Page<UserDTOs.UserResponseDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toDto);
    }

    @Override
    public Optional<User> getUserById(Long id) {
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
    public User updateUser(User user) {
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
    public Page<User> searchUsers(String username, String name, String status, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (username != null && !username.isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("username"), "%" + username + "%"));
            }
            if (name != null && !name.isEmpty()) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }
            if (status != null && !status.isEmpty()) {
                if (status.equalsIgnoreCase("active")) {
                    predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
                } else if (status.equalsIgnoreCase("inactive")) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("deletedAt")));
                }
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable);
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