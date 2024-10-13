package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.UserDTOs;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.entity.UserRole;
import com.online.course.management.project.enums.UserStatus;
import org.mapstruct.*;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    User toEntity(UserDTOs.UserRegistrationDto dto);


    UserDTOs.UserResponseDto toDto(User user);

    @Mapping(target = "roles", expression = "java(mapRoles(user.getUserRoles()))")
    UserDTOs.UserWithRolesResponseDto toUserWithRolesDto(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDto(UserDTOs.UpdateProfileDto dto, @MappingTarget User user);

    default Set<String> mapRoles(Set<UserRole> userRoles) {
        return userRoles.stream()
                .map(userRole -> userRole.getRole().getName().name())
                .collect(Collectors.toSet());
    }


}
