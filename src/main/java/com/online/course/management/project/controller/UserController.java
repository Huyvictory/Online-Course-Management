package com.online.course.management.project.controller;

import com.online.course.management.project.constants.UserConstants;
import com.online.course.management.project.dto.PaginationDto;
import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.enums.RoleType;
import com.online.course.management.project.mapper.UserMapper;
import com.online.course.management.project.security.CustomUserDetails;
import com.online.course.management.project.security.JwtUtil;
import com.online.course.management.project.security.RequiredRole;
import com.online.course.management.project.service.interfaces.IUserService;
import com.online.course.management.project.utils.user.UserControllerUtils;
import com.online.course.management.project.utils.user.UserServiceUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping(UserConstants.BASE_PATH)
@Slf4j
public class UserController {

    private final IUserService userService;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final UserControllerUtils userControllerUtils;

    @Autowired
    public UserController(IUserService userService, UserMapper userMapper, JwtUtil jwtUtil, UserControllerUtils userControllerUtils) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.userControllerUtils = userControllerUtils;
    }

    @PostMapping(UserConstants.REGISTER_PATH)
    public ResponseEntity<UserDTOs.UserResponseDto> registerUser(@Valid @RequestBody UserDTOs.UserRegistrationDto registrationDto) {
        log.info("Received registration request for email: {}", registrationDto.getEmail());
        UserDTOs.UserResponseDto responseDto = userService.registerUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @PostMapping(UserConstants.LOGIN_PATH)
    public ResponseEntity<UserDTOs.JwtResponseDto> loginUser(@Valid @RequestBody UserDTOs.UserLoginDto loginDto) {
        log.info("Received login request for username/email: {}", loginDto.getUsernameOrEmail());
        Authentication authentication = userControllerUtils.authenticate(loginDto.getUsernameOrEmail(), loginDto.getPassword());
        String jwt = jwtUtil.generateToken((CustomUserDetails) authentication.getPrincipal());
        return ResponseEntity.ok(new UserDTOs.JwtResponseDto(jwt));
    }

    @GetMapping(UserConstants.PATH_VARIABLE_PATH)
    @RequiredRole({"USER", "ADMIN"})
    public ResponseEntity<UserDTOs.UserResponseDto> getUserById(@PathVariable Long id) {
        log.info("Fetching user with id: {}", id);
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(userMapper.toDto(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(UserConstants.PROFILE_PATH)
    public ResponseEntity<UserDTOs.UserResponseDto> updateUserProfile(@Valid @RequestBody UserDTOs.UpdateProfileDto updateProfileDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        log.info("Updating profile for user id: {}", userId);
        UserDTOs.UserResponseDto updatedUser = userService.updateUserProfile(userId, updateProfileDto);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping(UserConstants.UPDATE_ROLES_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<UserDTOs.RoleUpdateResponseDto> updateUserRoles(@PathVariable Long id, @Valid @RequestBody UserDTOs.UpdateUserRolesDto updateUserRolesDto) {
        log.info("Updating roles for user id: {}", id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails currentUser = (CustomUserDetails) authentication.getPrincipal();

        Set<RoleType> validRoles = userControllerUtils.validateRoles(updateUserRolesDto.getRoles());

        var updatedRoles = userService.updateUserRoles(id, validRoles, currentUser.getId());

        UserDTOs.RoleUpdateResponseDto responseDto = new UserDTOs.RoleUpdateResponseDto(
                "Roles updated successfully",
                updatedRoles
        );
        return ResponseEntity.ok().body(responseDto);
    }

    @GetMapping(UserConstants.ALL_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<PaginationDto.PaginationResponseDto> getAllUsers(@Valid PaginationDto.PaginationRequestDto paginationRequestDto) {
        int page = paginationRequestDto.getPage();
        int limit = paginationRequestDto.getLimit();

        log.info("Fetching all users with roles, page: {}, limit: {}", page, limit);


        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<UserDTOs.UserWithRolesResponseDto> users = userService.getAllUsers(paginationRequestDto.toPageable());
        long totalUsers = userService.countUsers(Optional.empty());

        PaginationDto.PaginationResponseDto<UserDTOs.UserWithRolesResponseDto> response = new PaginationDto.PaginationResponseDto<>(
                users.getContent(),
                users.getNumber() + 1,
                users.getSize(),
                totalUsers
        );

        return ResponseEntity.ok().body(response);
    }

    @GetMapping(UserConstants.SEARCH_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<PaginationDto.PaginationResponseDto<UserDTOs.UserWithRolesResponseDto>> searchUsers(
            @Valid UserDTOs.UserSearchRequestDto searchRequest,
            @Valid PaginationDto.PaginationRequestDto paginationRequest) {

        log.info("Searching users with criteria: {}, page: {}, size: {}",
                searchRequest, paginationRequest.getPage(), paginationRequest.getLimit());

        Page<UserDTOs.UserWithRolesResponseDto> usersPage = userService.searchUsers(
                searchRequest, paginationRequest.toPageable());
        long totalSearchedUsers = userService.countUsers(Optional.of(searchRequest));

        PaginationDto.PaginationResponseDto<UserDTOs.UserWithRolesResponseDto> response = new PaginationDto.PaginationResponseDto<>(
                usersPage.getContent(),
                usersPage.getNumber() + 1,
                usersPage.getSize(),
                totalSearchedUsers
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping(UserConstants.PATH_VARIABLE_PATH)
    @RequiredRole({"ADMIN"})
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        log.info("Deleting user with id: {}", id);
        userService.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
