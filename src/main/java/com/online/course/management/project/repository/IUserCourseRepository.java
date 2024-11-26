package com.online.course.management.project.repository;

import com.online.course.management.project.entity.UserCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface IUserCourseRepository extends JpaRepository<UserCourse, Long>, JpaSpecificationExecutor<UserCourse> {

    /**
     * Check if user is already enrolled in a course
     */
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * Search enrolled courses for a user with detailed statistics.
     * If no search criteria provided, returns all enrolled courses with statistics.
     */
    @Query(value = """
        SELECT 
            uc.*,
            COUNT(DISTINCT CASE WHEN ulp.status = 'IN_PROGRESS' THEN ulp.lesson_id END) as processing_lessons,
            COUNT(DISTINCT CASE WHEN ulp.status = 'COMPLETED' THEN ulp.lesson_id END) as completed_lessons,
            (
                SELECT COUNT(DISTINCT l.id)
                FROM lessons l
                JOIN chapters ch ON l.chapter_id = ch.id
                WHERE ch.course_id = uc.course_id
                AND l.deleted_at IS NULL
            ) as total_lessons,
            (
                SELECT COALESCE(AVG(cr.rating), 0)
                FROM course_ratings cr
                WHERE cr.course_id = uc.course_id
                AND cr.deleted_at IS NULL
            ) as avg_rating
        FROM user_courses uc
        INNER JOIN courses c ON uc.course_id = c.id
        LEFT JOIN users u ON c.instructor_id = u.id
        LEFT JOIN user_lesson_progress ulp ON 
            uc.user_id = ulp.user_id AND 
            uc.course_id = ulp.course_id
        WHERE uc.user_id = :userId
        AND (:name IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:status IS NULL OR uc.status = :status)
        AND (:instructorName IS NULL OR LOWER(u.real_name) LIKE LOWER(CONCAT('%', :instructorName, '%')))
        AND (:fromDate IS NULL OR uc.enrollment_date >= :fromDate)
        AND (:toDate IS NULL OR uc.enrollment_date <= :toDate)
        AND (:minRating IS NULL OR 
            (SELECT COALESCE(AVG(cr.rating), 0)
             FROM course_ratings cr 
             WHERE cr.course_id = uc.course_id 
             AND cr.deleted_at IS NULL) >= :minRating)
        AND (:maxRating IS NULL OR 
            (SELECT COALESCE(AVG(cr.rating), 0)
             FROM course_ratings cr 
             WHERE cr.course_id = uc.course_id 
             AND cr.deleted_at IS NULL) <= :maxRating)
        AND (:lessonCount IS NULL OR 
            (SELECT COUNT(DISTINCT l.id)
             FROM lessons l 
             JOIN chapters ch ON l.chapter_id = ch.id 
             WHERE ch.course_id = uc.course_id 
             AND l.deleted_at IS NULL) = :lessonCount)
        GROUP BY uc.id, uc.course_id, c.id, u.id
        """,
            countQuery = """
            SELECT COUNT(DISTINCT uc.id)
            FROM user_courses uc
            INNER JOIN courses c ON uc.course_id = c.id
            LEFT JOIN users u ON c.instructor_id = u.id
            WHERE uc.user_id = :userId
            AND (:name IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:status IS NULL OR uc.status = :status)
            AND (:instructorName IS NULL OR LOWER(u.real_name) LIKE LOWER(CONCAT('%', :instructorName, '%')))
            AND (:fromDate IS NULL OR uc.enrollment_date >= :fromDate)
            AND (:toDate IS NULL OR uc.enrollment_date <= :toDate)
            AND (:minRating IS NULL OR 
                (SELECT COALESCE(AVG(cr.rating), 0)
                 FROM course_ratings cr 
                 WHERE cr.course_id = uc.course_id 
                 AND cr.deleted_at IS NULL) >= :minRating)
            AND (:maxRating IS NULL OR 
                (SELECT COALESCE(AVG(cr.rating), 0)
                 FROM course_ratings cr 
                 WHERE cr.course_id = uc.course_id 
                 AND cr.deleted_at IS NULL) <= :maxRating)
            AND (:lessonCount IS NULL OR 
                (SELECT COUNT(DISTINCT l.id)
                 FROM lessons l 
                 JOIN chapters ch ON l.chapter_id = ch.id 
                 WHERE ch.course_id = uc.course_id 
                 AND l.deleted_at IS NULL) = :lessonCount)
        """,
            nativeQuery = true)
    Page<UserCourse> searchUserEnrollments(
            @Param("userId") Long userId,
            @Param("name") String name,
            @Param("status") String status,
            @Param("instructorName") String instructorName,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating,
            @Param("lessonCount") Integer lessonCount,
            Pageable pageable
    );

    /**
     * Get detailed enrollment information for a specific user-course combination
     */
    @Query(value = """
            SELECT 
                uc.*,
                COUNT(DISTINCT CASE WHEN ulp.status = 'IN_PROGRESS' THEN ulp.lesson_id END) as processing_lessons,
                COUNT(DISTINCT CASE WHEN ulp.status = 'COMPLETED' THEN ulp.lesson_id END) as completed_lessons,
                (
                    SELECT COUNT(DISTINCT l.id)
                    FROM lessons l
                    JOIN chapters ch ON l.chapter_id = ch.id
                    WHERE ch.course_id = uc.course_id
                    AND l.deleted_at IS NULL
                ) as total_lessons,
                (
                    SELECT COALESCE(AVG(cr.rating), 0)
                    FROM course_ratings cr
                    WHERE cr.course_id = uc.course_id
                    AND cr.deleted_at IS NULL
                ) as avg_rating
            FROM user_courses uc
            LEFT JOIN user_lesson_progress ulp ON 
                uc.user_id = ulp.user_id AND 
                uc.course_id = ulp.course_id
            WHERE uc.user_id = :userId 
            AND uc.course_id = :courseId
            GROUP BY uc.id, uc.course_id
            """,
            nativeQuery = true)
    Optional<UserCourse> findEnrollmentWithStats(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );
}