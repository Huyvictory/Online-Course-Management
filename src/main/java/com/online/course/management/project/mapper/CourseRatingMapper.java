package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.CourseRatingDTOs;
import com.online.course.management.project.entity.CourseRating;
import com.online.course.management.project.repository.ICourseRatingRepository;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
@Slf4j
public abstract class CourseRatingMapper {

    @Autowired
    private ICourseRatingRepository courseRatingRepository;

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "reviewerName", source = "user.realName")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "deletedAt", source = "deletedAt")
    public abstract CourseRatingDTOs.CourseRatingResponseDTO toDto(CourseRating courseRating);
}
