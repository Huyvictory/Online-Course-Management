package com.online.course.management.project.repository;

import com.online.course.management.project.entity.UserCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Repository
public interface IUserCourseRepository extends JpaRepository<UserCourse, Long>, JpaSpecificationExecutor<UserCourse> {

    /**
     * Check if user is already enrolled in a course
     */
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    @Query(value = """
            SELECT uc.*
            FROM user_courses uc
            WHERE uc.user_id = :userId
            AND uc.course_id = :courseId
            """, nativeQuery = true)
    UserCourse findByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * Search enrolled courses for a user with detailed statistics.
     * If no search criteria provided, returns all enrolled courses with statistics.
     */
    @Query(value = """
            SELECT 
                uc.*
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
            AND (:totalLessons IS NULL OR
                (SELECT COUNT(DISTINCT l.id)
                 FROM lessons l 
                 JOIN chapters ch ON l.chapter_id = ch.id 
                 WHERE ch.course_id = uc.course_id 
                 AND l.deleted_at IS NULL) = :totalLessons)
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
                        AND (:totalLessons IS NULL OR
                            (SELECT COUNT(DISTINCT l.id)
                             FROM lessons l 
                             JOIN chapters ch ON l.chapter_id = ch.id 
                             WHERE ch.course_id = uc.course_id 
                             AND l.deleted_at IS NULL) = :totalLessons)
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
            @Param("totalLessons") Integer totalLessons,
            Pageable pageable
    );

    /**
     * Get detailed enrollment information for a specific user-course combination
     */
    @Query(value = """
            SELECT 
                COUNT(CASE WHEN ulp.status = 'IN_PROGRESS' THEN 1 END) as processing_lessons,
                COUNT(CASE WHEN ulp.status = 'COMPLETED' THEN 1 END) as completed_lessons
            FROM user_lesson_progress ulp
            WHERE ulp.user_id = :userId 
            AND ulp.course_id = :courseId
            """, nativeQuery = true)
    Map<String, Integer> getProgressStatistics(@Param("userId") Long userId, @Param("courseId") Long courseId);

    // Get total lessons for a course
    @Query(value = """
            SELECT COUNT(DISTINCT l.id)
            FROM lessons l
            JOIN chapters ch ON l.chapter_id = ch.id
            WHERE ch.course_id = :courseId
            AND l.deleted_at IS NULL
            """, nativeQuery = true)
    Integer getTotalLessons(@Param("courseId") Long courseId);

    // Get average rating for a course
    @Query(value = """
            SELECT COALESCE(AVG(cr.rating), 0.0)
            FROM course_ratings cr
            WHERE cr.course_id = :courseId
            AND cr.deleted_at IS NULL
            """, nativeQuery = true)
    Double getAverageCourseRating(@Param("courseId") Long courseId);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE user_lesson_progress
            SET status = 'DROPPED',
            last_accessed_at = CURRENT_TIMESTAMP
            WHERE user_id = :userId
            AND course_id = :courseId
            """, nativeQuery = true)
    void dropRelevantProgress(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Modifying
    @Query(value = """
            UPDATE user_lesson_progress
            SET status = 'IN_PROGRESS',
            last_accessed_at = CURRENT_TIMESTAMP
            WHERE user_id = :userId
            AND course_id = :courseId
            """, nativeQuery = true)
    void resumeRelevantProgress(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query(value = """
    WITH enrollment_check AS (
        SELECT 
            CASE 
                WHEN EXISTS (
                    SELECT 1 FROM user_courses 
                    WHERE user_id = :userId AND course_id = :courseId
                ) THEN 'ALREADY_ENROLLED'
                WHEN NOT EXISTS (
                    SELECT 1 FROM lessons l 
                    JOIN chapters ch ON l.chapter_id = ch.id
                    WHERE ch.course_id = :courseId 
                    AND l.deleted_at IS NULL 
                    AND ch.deleted_at IS NULL
                ) THEN 'NO_LESSONS'
                WHEN NOT EXISTS (
                    SELECT 1 FROM courses 
                    WHERE id = :courseId 
                    AND deleted_at IS NULL
                    AND status != 'ARCHIVED'
                            ) THEN 'INVALID_COURSE'
                            ELSE 'OK'
                        END as status
                )
                SELECT status FROM enrollment_check
            """, nativeQuery = true)
    String validateEnrollmentEligibility(
            @Param("userId") Long userId,
            @Param("courseId") Long courseId
    );
}