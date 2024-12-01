package com.online.course.management.project.repository;

import com.online.course.management.project.entity.CourseRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public interface ICourseRatingRepository extends JpaRepository<CourseRating, Long> {

    @Override
    <S extends CourseRating> S save(S courseRating);

    /**
     * Find user's rating for a course
     */
    @Query(value = """
            SELECT cr.*
            FROM course_ratings cr
            WHERE cr.user_id = :userId
            AND cr.course_id = :courseId
            AND cr.id = :id
            """, nativeQuery = true)
    Optional<CourseRating> findByUserIdAndCourseIdAndId(@Param("userId") Long userId, @Param("courseId") Long courseId, @Param("id") Long id);

    /**
     * Check if user has already rated a course
     */
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsById(Long id);

    /**
     * Soft delete a rating
     */
    @Modifying
    @Query("UPDATE CourseRating cr SET cr.deletedAt = CURRENT_TIMESTAMP WHERE cr.id = :id")
    void softDeleteRating(@Param("id") Long id);

    /**
     * Search course ratings with filtering and pagination
     */
    @Query(value = """
            SELECT cr.*, u.real_name as reviewer_name
            FROM course_ratings cr
            INNER JOIN courses c ON cr.course_id = c.id
            LEFT JOIN users u ON cr.user_id = u.id
            WHERE cr.course_id = :courseId
            AND (:minRating IS NULL OR (SELECT AVG(cr.rating) FROM course_ratings cr2 WHERE cr2.course_id = :courseId and cr2.deleted_at IS NULL) >= :minRating)
            AND (:maxRating IS NULL OR (SELECT AVG(cr.rating) FROM course_ratings cr2 WHERE cr2.course_id = :courseId and cr2.deleted_at IS NULL) <= :maxRating)
            AND cr.deleted_at IS NULL
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM course_ratings cr
                    WHERE  cr.course_id = :courseId
                    AND (:minRating IS NULL OR (SELECT AVG(cr.rating) FROM course_ratings cr2 WHERE cr2.course_id = :courseId and cr2.deleted_at IS NULL) >= :minRating)
                    AND (:maxRating IS NULL OR (SELECT AVG(cr.rating) FROM course_ratings cr2 WHERE cr2.course_id = :courseId and cr2.deleted_at IS NULL) <= :maxRating)
                    AND cr.deleted_at IS NULL
                    """,
            nativeQuery = true)
    Page<CourseRating> searchRatings(
            @Param("courseId") Long courseId,
            @Param("minRating") Integer minRating,
            @Param("maxRating") Integer maxRating,
            Pageable pageable
    );

    /**
     * Get average rating for a course (excluding deleted ratings)
     */
    @Query("""
            SELECT COALESCE(AVG(cr.rating), 0.0)
            FROM CourseRating cr
            WHERE cr.course.id = :courseId
            AND cr.deletedAt IS NULL
            """)
    Double getAverageRating(@Param("courseId") Long courseId);

    /**
     * Get count of ratings by star value (1-5) for a course
     */
    @Query("""
            SELECT cr.rating as stars, COUNT(cr) as count
            FROM CourseRating cr
            WHERE cr.course.id = :courseId
            AND cr.deletedAt IS NULL
            GROUP BY cr.rating
            ORDER BY cr.rating DESC
            """)
    Map<Object, Object> getRatingDistribution(@Param("courseId") Long courseId);
}
