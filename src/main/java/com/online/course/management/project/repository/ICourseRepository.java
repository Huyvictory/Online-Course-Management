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
                u.id as instructor_id,
                u.username as instructor_username,
                u.email as instructor_email,
                u.real_name as instructor_name,
                GROUP_CONCAT(DISTINCT cat.name ORDER BY cat.name) as category_names
            FROM courses c
            LEFT JOIN users u ON c.instructor_id = u.id
            LEFT JOIN course_categories cc ON c.id = cc.course_id
            LEFT JOIN categories cat ON cc.category_id = cat.id
            WHERE c.deleted_at IS NULL
            AND c.instructor_id = :instructorId
            AND (:includeArchived = true OR c.status != 'ARCHIVED')
            GROUP BY c.id, u.id, u.username, u.email, u.real_name
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT c.id)
                    FROM courses c
                    WHERE c.instructor_id = :instructorId
                    AND c.deleted_at IS NULL
                    AND (:includeArchived = true OR c.status != 'ARCHIVED')
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
            SELECT COUNT(DISTINCT c.id)
            FROM courses c
            WHERE c.instructor_id = :instructorId
            """, nativeQuery = true)
    long countByInstructorId(@Param("instructorId") Long instructorId);

    @Query(value = """
            SELECT COUNT(DISTINCT c.id)
            FROM courses c
            INNER JOIN course_categories cc ON c.id = cc.course_id
            WHERE cc.category_id = :categoryId
            """, nativeQuery = true)
    long countCoursesInCategory(@Param("categoryId") Long categoryId);

    @Query(value = """
            SELECT COUNT(DISTINCT c.id)
            FROM courses c
            WHERE c.status = :status
            AND c.deleted_at IS NULL
            """, nativeQuery = true)
    long countByStatus(@Param("status") String status);


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
            WHERE c.deleted_at IS NULL
            AND (:includeArchived = true OR c.status != 'ARCHIVED')
            AND (:title IS NULL OR LOWER(c.title) LIKE CONCAT('%', LOWER(:title), '%'))
            AND (:status IS NULL OR c.status = :status)
            AND (:instructorName IS NULL OR LOWER(u.real_name) LIKE CONCAT('%', LOWER(:instructorName), '%'))
            AND (:fromDate IS NULL OR c.created_at >= :fromDate)
            AND (:toDate IS NULL OR c.created_at <= :toDate)
            AND (COALESCE(:categoryIds) IS NULL OR cc.category_id IN (:categoryIds))
            GROUP BY c.id, u.username, u.email, u.real_name
            """,
            countQuery = """
                        SELECT COUNT(DISTINCT c.id)
                        FROM courses c
                        LEFT JOIN users u ON c.instructor_id = u.id
                        LEFT JOIN course_categories cc ON c.id = cc.course_id
                        WHERE c.deleted_at IS NULL
                        AND (:includeArchived = true OR c.status != 'ARCHIVED')
                        AND (:title IS NULL OR LOWER(c.title) LIKE CONCAT('%', LOWER(:title), '%'))
                        AND (:status IS NULL OR c.status = :status)
                        AND (:instructorName IS NULL OR LOWER(u.real_name) LIKE CONCAT('%', LOWER(:instructorName), '%'))
                        AND (:fromDate IS NULL OR c.created_at >= :fromDate)
                        AND (:toDate IS NULL OR c.created_at <= :toDate)
                        AND (COALESCE(:categoryIds) IS NULL OR cc.category_id IN (:categoryIds))
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

    // Update operations
    @Modifying
    @Query(value = """
            UPDATE courses 
            SET title = :title,
                description = :description,
                status = :status,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = :id
            """, nativeQuery = true)
    int updateCourse(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("description") String description,
            @Param("status") String status
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
            INSERT INTO course_categories (course_id, category_id)
            VALUES (:courseId, :categoryId)
            """, nativeQuery = true)
    void addCourseCategory(
            @Param("courseId") Long courseId,
            @Param("categoryId") Long categoryId
    );

}
