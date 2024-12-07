package com.online.course.management.project.repository;

import com.online.course.management.project.entity.Course;
import com.online.course.management.project.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ICourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    // Create & update operations
    @Override
    <S extends Course> S save(S course);


    @Query(value = """
             SELECT DISTINCT
                 c.*,
                 u.id as instructor_user_id,
                 u.username as instructor_username,
                 u.email as instructor_email,
                 u.real_name as instructor_name,
                 GROUP_CONCAT(DISTINCT cat.name ORDER BY cat.name) as category_names
             FROM courses c
             LEFT JOIN users u ON c.instructor_id = u.id
             LEFT JOIN course_categories cc ON c.id = cc.course_id
             LEFT JOIN categories cat ON cc.category_id = cat.id
            WHERE c.instructor_id = :instructorId
             AND (IF(:includeArchived = true, true, c.status != 'ARCHIVED'))
             GROUP BY c.id, u.id, u.username, u.email, u.real_name
             """,
            countQuery = """
                    SELECT COUNT(DISTINCT c.id)
                    FROM courses c
                    WHERE c.instructor_id = :instructorId
                    AND (CASE\s
                        WHEN :includeArchived = true THEN true
                        ELSE c.status != 'ARCHIVED'
                    END)
                    """,
            nativeQuery = true)
    Page<Course> findByInstructorId(
            @Param("instructorId") Long instructorId,
            @Param("includeArchived") Boolean includeArchived,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT c.*,
                u.username as instructor_username,
                u.email as instructor_email,
                u.real_name as instructor_name,
                GROUP_CONCAT(DISTINCT cat.name ORDER BY cat.name) as category_names
            FROM courses c
            LEFT JOIN users u ON c.instructor_id = u.id
            LEFT JOIN course_categories cc ON c.id = cc.course_id
            LEFT JOIN categories cat ON cc.category_id = cat.id
            WHERE c.id = :id 
            GROUP BY c.id, u.username, u.email, u.real_name
            HAVING c.id IS NOT NULL
            """, nativeQuery = true)
    Optional<Course> findByIdWithDetails(@Param("id") Long id);

    @Query(value = """
            SELECT DISTINCT
                c.*,
                u.username as instructor_username,
                u.email as instructor_email,
                u.real_name as instructor_name,
                GROUP_CONCAT(DISTINCT cat.name ORDER BY cat.name) as category_names
            FROM courses c
            LEFT JOIN users u ON c.instructor_id = u.id
            LEFT JOIN course_categories cc ON c.id = cc.course_id
            LEFT JOIN categories cat ON cc.category_id = cat.id
            WHERE c.status = :status
            GROUP BY c.id, u.username, u.email, u.real_name
            """,
            countQuery = """
                        SELECT COUNT(DISTINCT c.id)
                        FROM courses c
                        WHERE c.status = :status
                    """,
            nativeQuery = true)
    Page<Course> findByStatus(
            @Param("status") String status,
            Pageable pageable
    );

    @Query(value = """
            SELECT DISTINCT
                c.*,
                u.username as instructor_username,
                u.email as instructor_email,
                u.real_name as instructor_name,
                GROUP_CONCAT(DISTINCT cat.name ORDER BY cat.name) as category_names
            FROM courses c
            LEFT JOIN users u ON c.instructor_id = u.id
            LEFT JOIN course_categories cc ON c.id = cc.course_id
            LEFT JOIN categories cat ON cc.category_id = cat.id
            WHERE c.status != 'ARCHIVED'
            AND c.deleted_at IS NULL
            GROUP BY c.id, u.username, u.email, u.real_name
            ORDER BY c.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Course> findLatestCourses(@Param("limit") int limit);

    @Query(value = """
            SELECT DISTINCT
                        c.*,
                        u.username as instructor_username,
                        u.email as instructor_email,
                        u.real_name as instructor_name,
                        GROUP_CONCAT(DISTINCT cat.name ORDER BY cat.name) as category_names
                    FROM courses c
                    LEFT JOIN users u ON c.instructor_id = u.id
                    LEFT JOIN course_categories cc ON c.id = cc.course_id
                    LEFT JOIN categories cat ON cc.category_id = cat.id
                    WHERE (:title IS NULL OR LOWER(c.title) LIKE CONCAT('%', LOWER(:title), '%'))
                    AND (IF(:includeArchived = true, true, c.status != 'ARCHIVED'))
                    AND (:status IS NULL OR c.status = :status)
                    AND (:instructorName IS NULL OR LOWER(u.real_name) LIKE CONCAT('%', LOWER(:instructorName), '%'))
                    AND (:fromDate IS NULL OR c.created_at >= :fromDate)
                    AND (:toDate IS NULL OR c.created_at <= :toDate)
                    AND cc.category_id IN (:categoryIds)
                    GROUP BY\s
                        c.id,
                        u.id,
                        u.username,
                        u.email,
                        u.real_name,
                        c.status,
                        c.updated_at,
                        c.created_at
            """,
            countQuery = """
                    SELECT COUNT(c.id)
                        FROM courses c
                        JOIN course_categories cc ON c.id = cc.course_id
                        LEFT JOIN users u ON c.instructor_id = u.id
                        WHERE (:title IS NULL OR LOWER(c.title) LIKE CONCAT('%', LOWER(:title), '%'))
                        AND (IF(:includeArchived = true, true, c.status != 'ARCHIVED'))
                        AND (:status IS NULL OR c.status = :status)
                        AND (:instructorName IS NULL OR LOWER(u.real_name) LIKE CONCAT('%', LOWER(:instructorName), '%'))
                        AND (:fromDate IS NULL OR c.created_at >= :fromDate)
                        AND (:toDate IS NULL OR c.created_at <= :toDate)
                        AND cc.category_id IN (:categoryIds)
                    """,
            nativeQuery = true)
    Page<Course> searchCourses(
            @Param("title") String title,
            @Param("status") String status,
            @Param("instructorName") String instructorName,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("categoryIds") Set<Long> categoryIds,
            @Param("includeArchived") Boolean includeArchived,
            Pageable pageable
    );

    // Archive operation (special form of soft delete)
    @Modifying
    @Query(value = """
            UPDATE courses
            SET status = 'ARCHIVED',
                deleted_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :courseId
            """, nativeQuery = true)
    void archiveCourse(@Param("courseId") Long courseId);

    @Modifying
    @Query(value = """
            UPDATE chapters
            SET status = 'ARCHIVED',
                deleted_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE course_id = :courseId
            """, nativeQuery = true)
    void archiveFollowingChapters(@Param("courseId") Long courseId);

    @Modifying
    @Query(value = """
            UPDATE lessons
            SET status = 'ARCHIVED',
                deleted_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE chapter_id IN (
                SELECT id FROM chapters 
                WHERE course_id = :courseId 
            )
            """, nativeQuery = true)
    void archiveFollowingLessons(@Param("courseId") Long courseId);


    @Modifying
    @Query(value = """
            UPDATE courses
            SET status = 'DRAFT',
                deleted_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :courseId
            AND status = 'ARCHIVED'
            """, nativeQuery = true)
    void unarchiveCourse(@Param("courseId") Long courseId);

    @Modifying
    @Query(value = """
            UPDATE chapters
            SET status = 'DRAFT',
                deleted_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE course_id = :courseId
            """, nativeQuery = true)
    void unarchiveFollowingChapters(@Param("courseId") Long courseId);

    @Modifying
    @Query(value = """
            UPDATE lessons
            SET status = 'DRAFT',
                deleted_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE chapter_id IN (
                SELECT id FROM chapters 
                WHERE course_id = :courseId 
            )
            """, nativeQuery = true)
    void unarchiveFollowingLessons(@Param("courseId") Long courseId);

    @Modifying
    @Query(value = """
            INSERT INTO course_categories (course_id, category_id)
            VALUES (:courseId, :categoryId)
            """, nativeQuery = true)
    void addCourseCategory(
            @Param("courseId") Long courseId,
            @Param("categoryId") Long categoryId
    );

}
