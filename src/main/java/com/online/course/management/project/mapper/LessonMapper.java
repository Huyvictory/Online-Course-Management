package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Lesson;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LessonMapper {

    @Mapping(target = "chapterId", source = "chapter.id")
    @Mapping(target = "chapterTitle", source = "chapter.title")
    LessonDTOs.LessonResponseDto toDto(Lesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "chapter", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Lesson toEntity(LessonDTOs.CreateLessonDTO dto);

    @Mapping(target = "chapterId", source = "chapter.id")
    @Mapping(target = "chapterTitle", source = "chapter.title")
    @Mapping(target = "courseId", source = "chapter.course.id")
    @Mapping(target = "courseTitle", source = "chapter.course.title")
    @Mapping(target = "completedByUsers", expression = "java(getCompletedUsersCount(lesson))")
    @Mapping(target = "inProgressByUsers", expression = "java(getInProgressUsersCount(lesson))")
    LessonDTOs.LessonDetailResponseDto toDetailDto(Lesson lesson);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "chapter", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateLessonFromDto(LessonDTOs.UpdateLessonDTO dto, @MappingTarget Lesson lesson);

    default void setChapter(Lesson lesson, Chapter chapter) {
        lesson.setChapter(chapter);
    }

    // These methods would be implemented when we add user progress tracking
    default Integer getCompletedUsersCount(Lesson lesson) {
        // TODO: Implement when user progress tracking is added
        return 0;
    }

    default Integer getInProgressUsersCount(Lesson lesson) {
        // TODO: Implement when user progress tracking is added
        return 0;
    }
}