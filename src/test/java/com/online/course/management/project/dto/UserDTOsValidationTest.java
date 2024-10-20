package com.online.course.management.project.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class UserDTOsValidationTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void userRegistrationDto_ValidData() {
        UserDTOs.UserRegistrationDto dto = new UserDTOs.UserRegistrationDto();
        dto.setEmail("test@example.com");
        dto.setPassword("password123");

        Set<ConstraintViolation<UserDTOs.UserRegistrationDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void userRegistrationDto_InvalidEmail() {
        UserDTOs.UserRegistrationDto dto = new UserDTOs.UserRegistrationDto();
        dto.setEmail("invalid-email");
        dto.setPassword("password123");

        Set<ConstraintViolation<UserDTOs.UserRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("must be a well-formed email address", violations.iterator().next().getMessage());
    }

    @Test
    void userRegistrationDto_EmptyEmail() {
        UserDTOs.UserRegistrationDto dto = new UserDTOs.UserRegistrationDto();
        dto.setPassword("password123");

        Set<ConstraintViolation<UserDTOs.UserRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("must not be blank", violations.iterator().next().getMessage());
    }

    @Test
    void userRegistrationDto_PasswordTooShort() {
        UserDTOs.UserRegistrationDto dto = new UserDTOs.UserRegistrationDto();
        dto.setEmail("test@example.com");
        dto.setPassword("short");

        Set<ConstraintViolation<UserDTOs.UserRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Password must be between 6 and 40 characters", violations.iterator().next().getMessage());
    }

    @Test
    void userRegistrationDto_PasswordTooLong() {
        UserDTOs.UserRegistrationDto dto = new UserDTOs.UserRegistrationDto();
        dto.setEmail("test@example.com");
        dto.setPassword("a".repeat(41));

        Set<ConstraintViolation<UserDTOs.UserRegistrationDto>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        assertEquals("Password must be between 6 and 40 characters", violations.iterator().next().getMessage());
    }
}
