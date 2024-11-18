package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.ChapterDTOs;
import com.online.course.management.project.dto.LessonDTOs;
import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.Lesson;
import com.online.course.management.project.enums.CourseStatus;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {LessonMapper.class})
public interface ChapterMapper {

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "totalLessons", expression = "java(getActiveLessonCount(chapter))")
    @Mapping(target = "completedLessons", expression = "java(getCompletedLessonCount(chapter))")
    @Mapping(target = "inProgressLessons", expression = "java(getInProgressLessonCount(chapter))")
    ChapterDTOs.ChapterResponseDto toDto(Chapter chapter);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "lessons", expression = "java(mapCreateLessonDTOs(dto.getLessons(), chapter))")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Chapter toEntity(ChapterDTOs.CreateChapterDTO dto);

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "courseStatus", source = "course.status")
    @Mapping(target = "completedLessons", expression = "java(getCompletedLessonCount(chapter))")
    @Mapping(target = "inProgressLessons", expression = "java(getInProgressLessonCount(chapter))")
    ChapterDTOs.ChapterDetailResponseDto toDetailDto(Chapter chapter);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateChapterFromDto(ChapterDTOs.UpdateChapterDTO dto, @MappingTarget Chapter chapter);

    List<ChapterDTOs.ChapterResponseDto> toDtoList(List<Chapter> chapters);

    default void setCourse(Chapter chapter, Course course) {
        chapter.setCourse(course);
    }

    default Integer getActiveLessonCount(Chapter chapter) {
        if (chapter.getLessons() == null) return 0;
        return (int) chapter.getLessons().stream()
                .filter(lesson -> lesson.getDeletedAt() == null)
                .count();
    }

    default Integer getCompletedLessonCount(Chapter chapter) {
        // TODO: Implement when user progress tracking is added
        return 0;
    }

    default Integer getInProgressLessonCount(Chapter chapter) {
        // TODO: Implement when user progress tracking is added
        return 0;
    }


    @Named("mapCreateLessonDTOs")
    default List<Lesson> mapCreateLessonDTOs(List<LessonDTOs.CreateLessonDTO> lessonDtos, Chapter chapter) {
        if (lessonDtos == null) {
            return null;
        }

        return lessonDtos.stream()
                .map(dto -> {
                    Lesson lesson = new Lesson();
                    lesson.setTitle(dto.getTitle());
                    lesson.setContent(dto.getContent());
                    lesson.setOrder(dto.getOrder());
                    lesson.setType(dto.getType());
                    lesson.setStatus(CourseStatus.DRAFT);
                    lesson.setChapter(chapter);
                    return lesson;
                })
                .toList();
    }
}