package com.online.course.management.project.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Set;

public class UserDTOs {
    @Data
    public static class UserRegistrationDto {
        @NotBlank
        @Size(max = 50)
        @Email
        private String email;

        @NotBlank
        @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
        private String password;


    }

    @Data
    public static class UserLoginDto {
        @NotBlank
        @Size(max = 50)
        private String usernameOrEmail;

        @NotBlank
        @Size(min = 6, max = 40)
        private String password;
    }

    @Data
    public static class UserResponseDto {
        private Long id;
        private String username;
        private String email;
        private String realName;
        private String status;
    }

    @Data
    public static class UpdateProfileDto {
        @Size(max = 50)
        private String username;

        @Size(max = 50)
        @Email
        private String email;

        @Size(max = 100)
        private String realName;

        @Size(min = 6, max = 40)
        private String password;

        private String status;
    }

    @Data
    public static class UpdateUserRolesDto {
        @NotEmpty
        private Set<String> roles;
    }

    @Data
    public static class JwtResponseDto {
        @NotNull
        private String token;
    }

    @Data
    @AllArgsConstructor
    public static class RoleUpdateResponseDto {
        private String message;
        private Set<String> updatedRoles;
    }

    @Data
    public static class UserWithRolesResponseDto {
        private Long id;
        private String username;
        private String email;
        private String realName;
        private String status;
        private Set<String> roles;
    }

    @Data
    public static class UserSearchRequestDto {
        @Size(max = 50)
        private String username;

        @Size(max = 50)
        @Email
        private String email;

        @Size(max = 100)
        private String realName;

        private String status;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime fromDate;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime toDate;
    }
}
