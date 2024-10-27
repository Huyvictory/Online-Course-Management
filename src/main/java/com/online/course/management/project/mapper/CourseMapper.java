package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.CourseDTOS;
import com.online.course.management.project.entity.Category;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.User;
import org.mapstruct.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CourseMapper {

    @Mapping(target = "instructor", source = "instructor")
    @Mapping(target = "categoryNames", expression = "java(mapCategoryNames(course))")
    CourseDTOS.CourseDetailsResponseDto toDto(Course course);

    @Mapping(target = "instructorId", source = "instructor.id")
    @Mapping(target = "categoryNames", expression = "java(mapCategoryNames(course))")
    CourseDTOS.CourseListResponseDto toListDto(Course course);

    @Mapping(target = "roles", expression = "java(mapUserRoles(user))")
    CourseDTOS.InstructorDto toInstructorDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instructor", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Course toEntity(CourseDTOS.CreateCourseRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "instructor", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateCourseFromDto(CourseDTOS.UpdateCourseRequestDTO dto, @MappingTarget Course course);

    default Set<String> mapCategoryNames(Course course) {
        if (course.getCategories() == null) return new HashSet<>();
        return course.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());
    }

    default Set<String> mapUserRoles(User user) {
        if (user.getUserRoles() == null) return new HashSet<>();
        return user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getName().name())
                .collect(Collectors.toSet());
    }
}