package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.UserLessonProgressDtos;
import com.online.course.management.project.entity.UserLessonProgress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserLessonProgressMapper {

    @Mapping(target = "id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "chapterId", source = "chapter.id")
    @Mapping(target = "lessonId", source = "lesson.id")
    @Mapping(target = "lastAccessedAt", source = "lastAccessedAt")
    @Mapping(target = "completionDate", source = "completionDate")
    @Mapping(target = "status", source = "status")
    UserLessonProgressDtos.LessonProgressResponseDTO toDto(UserLessonProgress userLessonProgress);
}
