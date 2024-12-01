package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.CourseRatingDTOs;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.CourseRating;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.exception.business.InvalidRequestException;
import com.online.course.management.project.repository.ICourseRepository;
import com.online.course.management.project.repository.IUserRepository;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class CourseRatingMapper {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private ICourseRepository courseRepository;

    @Mapping(target = "user", expression = "java(getUser(userId))")
    @Mapping(target = "courseId", expression = "java(getCourse(courseId))")
    @Mapping(target = "rating", source = "rating")
    @Mapping(target = "reviewText", source = "reviewText")
    public abstract CourseRating toEntity(CourseRatingDTOs.CourseRatingCreateDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "course", source = "course.id")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    public abstract CourseRatingDTOs.CourseRatingResponseDTO toDto(CourseRating courseRating);

    protected User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new InvalidRequestException("User not found"));
    }

    protected Course getCourse(Long courseId) {
        return courseRepository.findById(courseId).orElseThrow(() -> new InvalidRequestException("Course not found"));
    }

}
