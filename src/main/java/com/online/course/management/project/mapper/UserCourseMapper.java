package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.UserCourseDTOs;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.entity.UserCourse;
import com.online.course.management.project.entity.UserLessonProgress;
import com.online.course.management.project.enums.EnrollmentStatus;
import com.online.course.management.project.enums.ProgressStatus;
import com.online.course.management.project.repository.IUserLessonProgressRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.online.course.management.project.repository.IUserCourseRepository;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class UserCourseMapper {

    @Autowired
    protected IUserCourseRepository userCourseRepository;

    @Autowired
    protected IUserLessonProgressRepository userLessonProgressRepository;

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "instructorName", source = "user.realName")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "enrollmentDate", source = "enrollmentDate")
    @Mapping(target = "completionDate", source = "completionDate")
    @Mapping(target = "processingLessons", ignore = true)
    @Mapping(target = "completedLessons", ignore = true)
    @Mapping(target = "totalLessons", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    public abstract UserCourseDTOs.UserCourseResponseDto toDto(UserCourse userCourse);

    @AfterMapping
    protected void mapStatistics(UserCourse userCourse, @MappingTarget UserCourseDTOs.UserCourseResponseDto dto) {
        if (userCourse == null) return;

        var enrollmentWithStats = userCourseRepository
                .findEnrollmentWithStats(userCourse.getUser().getId(), userCourse.getCourse().getId())
                .orElse(null);

        if (enrollmentWithStats != null) {
            // These fields are populated by the native query
            try {
                dto.setProcessingLessons((Integer) enrollmentWithStats.getClass()
                        .getMethod("getProcessingLessons").invoke(enrollmentWithStats));
                dto.setCompletedLessons((Integer) enrollmentWithStats.getClass()
                        .getMethod("getCompletedLessons").invoke(enrollmentWithStats));
                dto.setTotalLessons((Integer) enrollmentWithStats.getClass()
                        .getMethod("getTotalLessons").invoke(enrollmentWithStats));
                dto.setAverageRating((Double) enrollmentWithStats.getClass()
                        .getMethod("getAverageRating").invoke(enrollmentWithStats));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "course", source = "course")
    @Mapping(target = "enrollmentDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "completionDate", ignore = true)
    @Mapping(target = "status", constant = "ENROLLED")
    public abstract UserCourse toEntity(User user, Course course);

    public abstract List<UserCourseDTOs.UserCourseResponseDto> toDtoList(List<UserCourse> userCourses);

    @Mapping(target = "totalInProgress", expression = "processing_lessons")
    @Mapping(target = "totalCompleted", expression = "completed_lessons")
    @Mapping(target = "averageCompletionTime", expression = "java(calculateAverageCompletionTime(userCourse))")
    @Mapping(target = "completionRate", expression = "java(calculateCompletionRate(userCourse))")
    public abstract UserCourseDTOs.UserCourseStatisticsDTO toStatisticsDto(UserCourse userCourse);

    protected Double calculateAverageCompletionTime(UserCourse userCourse) {
        if (userCourse.getCompletionDate() == null || userCourse.getEnrollmentDate() == null) {
            return 0.0;
        }
        return (double) ChronoUnit.DAYS.between(
                userCourse.getEnrollmentDate(),
                userCourse.getCompletionDate()
        );
    }

    protected Double calculateCompletionRate(UserCourse userCourse) {

        List<UserLessonProgress> userLessonProgresses = userLessonProgressRepository.findAllByUserIdAndCourseId(userCourse.getUser().getId(), userCourse.getCourse().getId());

        var TotalCompletedLessons = userLessonProgresses.stream()
                .filter(ulp -> ulp.getStatus() == ProgressStatus.COMPLETED)
                .count();

        var completionRate = TotalCompletedLessons / userLessonProgresses.size();
        return completionRate * 100.0;
    }
}