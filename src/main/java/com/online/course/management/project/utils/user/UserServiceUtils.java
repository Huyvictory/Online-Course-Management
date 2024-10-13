package com.online.course.management.project.utils.user;

import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.entity.Role;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.enums.UserStatus;
import com.online.course.management.project.exception.InvalidRoleInfoException;
import com.online.course.management.project.exception.ResourceNotFoundException;
import com.online.course.management.project.exception.UnauthorizedException;
import com.online.course.management.project.mapper.UserMapper;
import com.online.course.management.project.repository.IRoleRepository;
import com.online.course.management.project.repository.IUserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class UserServiceUtils {
    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UserServiceUtils(IUserRepository userRepository, IRoleRepository roleRepository, UserMapper userMapper, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public String generateUsernameFromEmail(String email) {
        String baseUsername = email.split("@")[0];
        String username = baseUsername;
        int suffix = 1;

        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix;
            suffix++;
        }

        return username;
    }

    public void assignDefaultRole(User user) {
        Role userRole = roleRepository.findByName(RoleType.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default user role not found"));
        user.addRole(userRole);
    }

    public void validateNewUser(UserDTOs.UserRegistrationDto registrationDto) {
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    public User createUserFromDto(UserDTOs.UserRegistrationDto registrationDto) {
        User user = userMapper.toEntity(registrationDto);
        user.setUsername(generateUsernameFromEmail(registrationDto.getEmail()));
        user.setPasswordHash(passwordEncoder.encode(registrationDto.getPassword()));
        return user;
    }

    public Specification<User> createSpecification(UserDTOs.UserSearchRequestDto searchUsersPayload) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchUsersPayload.getUsername() != null && !searchUsersPayload.getUsername().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + searchUsersPayload.getUsername().toLowerCase() + "%"));
            }
            if (searchUsersPayload.getEmail() != null && !searchUsersPayload.getEmail().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + searchUsersPayload.getEmail().toLowerCase() + "%"));
            }
            if (searchUsersPayload.getRealName() != null && !searchUsersPayload.getRealName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("realName")), "%" + searchUsersPayload.getRealName().toLowerCase() + "%"));
            }
            if (searchUsersPayload.getStatus() != null && !searchUsersPayload.getStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), UserStatus.valueOf(searchUsersPayload.getStatus().toUpperCase())));
            }
            if (searchUsersPayload.getFromDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), searchUsersPayload.getFromDate()));
            }
            if (searchUsersPayload.getToDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), searchUsersPayload.getToDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


}
