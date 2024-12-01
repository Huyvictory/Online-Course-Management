package com.online.course.management.project.mapper;

import com.online.course.management.project.dto.UserCourseDTOs;
import com.online.course.management.project.entity.Course;
import com.online.course.management.project.entity.User;
import com.online.course.management.project.entity.UserCourse;
import com.online.course.management.project.entity.UserLessonProgress;
import com.online.course.management.project.enums.EnrollmentStatus;
import com.online.course.management.project.enums.ProgressStatus;
import com.online.course.management.project.repository.IUserLessonProgressRepository;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.online.course.management.project.repository.IUserCourseRepository;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
@Slf4j
public abstract class UserCourseMapper {

    @Autowired
    protected IUserCourseRepository userCourseRepository;

    @Autowired
    protected IUserLessonProgressRepository userLessonProgressRepository;

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "instructorName", source = "course.instructor.realName")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "enrollmentDate", source = "enrollmentDate")
    @Mapping(target = "completionDate", source = "completionDate")
    @Mapping(target = "processingLessons", constant = "0")
    @Mapping(target = "completedLessons", constant = "0")
    @Mapping(target = "totalLessons", constant = "0")
    @Mapping(target = "averageRating", constant = "0.0")
    @Mapping(target = "averageCompletionTime", constant = "0.0")
    @Mapping(target = "completionRate", constant = "0.0")
    abstract UserCourseDTOs.UserCourseResponseDto toBaseDto(UserCourse userCourse);

    // Add a non-abstract method to handle the full mapping including statistics
    public UserCourseDTOs.UserCourseResponseDto toDto(UserCourse userCourse) {
        UserCourseDTOs.UserCourseResponseDto baseDto = toBaseDto(userCourse);
        mapStatistics(userCourse, baseDto);
        return baseDto;
    }

    public void mapStatistics(UserCourse userCourse, @MappingTarget UserCourseDTOs.UserCourseResponseDto dto) {
        System.out.println("AfterMapping is running for userCourse: " + userCourse.getId());

        log.debug("Mapping statistics for userCourse: {}", userCourse.getId());

        try {
            // Get lesson progress statistics
            List<UserLessonProgress> progressList = userLessonProgressRepository
                    .findAllByUserIdAndCourseId(userCourse.getUser().getId(), userCourse.getCourse().getId());

            // Count lessons by status
            int processingCount = 0;
            int completedCount = 0;
            for (UserLessonProgress progress : progressList) {
                if (progress.getStatus() == ProgressStatus.IN_PROGRESS) {
                    processingCount++;
                } else if (progress.getStatus() == ProgressStatus.COMPLETED) {
                    completedCount++;
                }
            }

            // Calculate total lessons in the course
            int totalLessons = (int) userCourseRepository.getTotalLessons(userCourse.getCourse().getId());

            // Get average rating from course ratings
            double avgRating = userCourseRepository.getAverageCourseRating(userCourse.getCourse().getId());

            double avgCompletionTime = calculateAverageCompletionTime(userCourse);
            double completionRate = calculateCompletionRate(userCourse);

            // Set the statistics
            dto.setProcessingLessons(processingCount);
            dto.setCompletedLessons(completedCount);
            dto.setTotalLessons(totalLessons);
            dto.setAverageRating(avgRating);
            dto.setAverageCompletionTime(avgCompletionTime);
            dto.setCompletionRate(completionRate);

            log.debug("Statistics mapped - Processing: {}, Completed: {}, Total: {}, Rating: {}",
                    processingCount, completedCount, totalLessons, avgRating);

        } catch (Exception e) {
            log.error("Error mapping statistics for userCourse {}: {}", userCourse.getId(), e.getMessage());
            // Set default values in case of error
            dto.setProcessingLessons(0);
            dto.setCompletedLessons(0);
            dto.setTotalLessons(0);
            dto.setAverageRating(0.0);
        }
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", source = "user")
    @Mapping(target = "course", source = "course")
    @Mapping(target = "enrollmentDate", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "completionDate", ignore = true)
    @Mapping(target = "status", constant = "ENROLLED")
    public abstract UserCourse toEntity(User user, Course course);

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