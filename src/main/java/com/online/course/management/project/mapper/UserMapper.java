package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(UserDTOs.UserRegistrationDto dto) {
        User user = new User();
        user.setEmail(dto.getEmail());
        return user;
    }

    public UserDTOs.UserResponseDto toDto(User user) {
        UserDTOs.UserResponseDto dto = new UserDTOs.UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRealName(user.getRealName());
        return dto;
    }
}
