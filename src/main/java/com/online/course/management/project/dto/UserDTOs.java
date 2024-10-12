package com.online.course.management.project.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

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
    }

    @Data
    public static class UpdateUserRolesDto {
        @NotNull
        private Long userId;

        @NotEmpty
        private Set<String> roles;
    }

    @Data
    public static class JwtResponseDto {
        @NotNull
        private String token;
    }
}
