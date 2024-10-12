package com.online.course.management.project.controller;

import com.online.course.management.project.dto.UserDTOs;
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
import org.springframework.web.bind.annotation.*;

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
            log.info("Authentication successful");

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            log.info("User details retrieved: {}", userDetails.getUsername());

            String jwt = jwtUtil.generateToken(userDetails);
            log.info("JWT token generated successfully");

            UserDTOs.JwtResponseDto jwtResponseDto = new UserDTOs.JwtResponseDto();
            jwtResponseDto.setToken(jwt);
            log.info("Returning JWT response");

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

    @PutMapping("/{id}/profile")
    @RequiredRole({"USER"})
    public ResponseEntity<UserDTOs.UserResponseDto> updateUserProfile(@PathVariable Long id, @Valid @RequestBody UserDTOs.UpdateProfileDto updateProfileDto) {
        log.info("Updating profile for user id: {}", id);
        UserDTOs.UserResponseDto updatedUser = userService.updateUserProfile(id, updateProfileDto);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/roles")
    @RequiredRole({"ADMIN"})
    public ResponseEntity<Void> updateUserRoles(@PathVariable Long id, @Valid @RequestBody UserDTOs.UpdateUserRolesDto updateUserRolesDto) {
        log.info("Updating roles for user id: {}", id);
        userService.updateUserRoles(id, updateUserRolesDto.getRoles());
        return ResponseEntity.ok().build();
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
