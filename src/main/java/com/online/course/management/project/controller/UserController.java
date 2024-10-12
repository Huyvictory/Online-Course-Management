package com.online.course.management.project.controller;

import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.exception.InvalidRoleInfoException;
import com.online.course.management.project.mapper.UserMapper;
import com.online.course.management.project.security.CustomUserDetails;
import com.online.course.management.project.security.JwtUtil;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.IUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@Slf4j
public class UserController {

    private final IUserService userService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserController(IUserService userService, UserMapper userMapper, AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDTOs.UserResponseDto> registerUser(@Valid @RequestBody UserDTOs.UserRegistrationDto registrationDto) {
        log.info("Received registration request for email: {}", registrationDto.getEmail());
        UserDTOs.UserResponseDto responseDto = userService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<UserDTOs.JwtResponseDto> loginUser(@Valid @RequestBody UserDTOs.UserLoginDto loginDto) {
        log.info("Received login request for username/email: {}", loginDto.getUsernameOrEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDto.getUsernameOrEmail(), loginDto.getPassword())
            );


            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();


            String jwt = jwtUtil.generateToken(userDetails);


            UserDTOs.JwtResponseDto jwtResponseDto = new UserDTOs.JwtResponseDto();
            jwtResponseDto.setToken(jwt);


            return ResponseEntity.ok(jwtResponseDto);
        } catch (Exception e) {
            log.error("Error during login process", e);
            throw e; // or handle it appropriately
        }
    }

    @GetMapping("/{id}")
    @RequiredRole({"USER", "ADMIN"})
    public ResponseEntity<UserDTOs.UserResponseDto> getUserById(@PathVariable Long id) {
        log.info("Fetching user with id: {}", id);
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(userMapper.toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTOs.UserResponseDto> updateUserProfile(@Valid @RequestBody UserDTOs.UpdateProfileDto updateProfileDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("Updating profile for user id: {}", userId);
        UserDTOs.UserResponseDto updatedUser = userService.updateUserProfile(userId, updateProfileDto);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/roles")
    @RequiredRole({"ADMIN"})
    public ResponseEntity<UserDTOs.RoleUpdateResponseDto> updateUserRoles(@PathVariable Long id, @Valid @RequestBody UserDTOs.UpdateUserRolesDto updateUserRolesDto) {
        log.info("Updating roles for user id: {}", id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();

        Set<RoleType> validRoles = new HashSet<>();
        Set<String> invalidRoles = new HashSet<>();

        for (String role : updateUserRolesDto.getRoles()) {
            try {
                validRoles.add(RoleType.valueOf(role.toUpperCase()));
            } catch (IllegalArgumentException e) {
                invalidRoles.add(role);
            }
        }

        if (!invalidRoles.isEmpty()) {
            throw new InvalidRoleInfoException("Invalid role(s) provided: " + String.join(", ", invalidRoles));
        }



        var updatedRoles = userService.updateUserRoles(id, validRoles, currentUser.getId());

        UserDTOs.RoleUpdateResponseDto responseDto = new UserDTOs.RoleUpdateResponseDto(
                "Roles updated successfully",
                updatedRoles
        );
        return ResponseEntity.ok().body(responseDto);
    }

    @GetMapping("/all")
    @RequiredRole({"ADMIN"})
    public ResponseEntity<Page<UserDTOs.UserResponseDto>> getAllUsers(Pageable pageable) {
        log.info("Fetching all users");
        Page<UserDTOs.UserResponseDto> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/{id}")
    @RequiredRole({"ADMIN"})
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with id: {}", id);
        userService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
