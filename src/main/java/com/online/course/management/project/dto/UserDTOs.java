package com.online.course.management.project.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
}
