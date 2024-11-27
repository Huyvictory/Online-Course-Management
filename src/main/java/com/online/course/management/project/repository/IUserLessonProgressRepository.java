package com.online.course.management.project.repository;

import com.online.course.management.project.entity.UserLessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IUserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    @Override
    <S extends UserLessonProgress> S save(S userLessonProgress);

    @Query(value = """
            SELECT ulp.*
            FROM user_lesson_progress ulp
            WHERE ulp.user_id = :userId
            AND ulp.course_id = :courseId
            """, nativeQuery = true)
    List<UserLessonProgress> findAllByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);
}