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


    @Query("""
            SELECT c FROM Course c 
            WHERE c.instructor.id = :instructorId 
            """)
    Page<Course> findByInstructorId(@Param("instructorId") Long instructorId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.instructor.id = :instructorId AND c.deletedAt IS NULL")
    long countByInstructorId(@Param("instructorId") Long instructorId);

    @Query("""
            SELECT c FROM Course c 
            LEFT JOIN FETCH c.categories 
            LEFT JOIN FETCH c.instructor i 
            LEFT JOIN FETCH i.userRoles r 
            LEFT JOIN FETCH r.role 
            WHERE c.id = :id
            """)
    Optional<Course> findByIdWithCategories(@Param("id") Long id);

    @Query("""
            SELECT COUNT(DISTINCT c) FROM Course c 
            JOIN c.categories cat 
            WHERE cat.id = :categoryId 
            AND c.deletedAt IS NULL
            """)
    Long countCoursesInCategory(@Param("categoryId") Long categoryId);


    @Query("""
            SELECT DISTINCT c FROM Course c 
            LEFT JOIN FETCH c.instructor i 
            LEFT JOIN FETCH i.userRoles r 
            LEFT JOIN FETCH r.role 
            LEFT JOIN FETCH c.categories cat
            WHERE c.status != 'ARCHIVED' AND c.deletedAt IS NULL 
            AND (:title IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:status IS NULL OR c.status = :status)
            AND (:instructorName IS NULL OR LOWER(i.realName) LIKE LOWER(CONCAT('%', :instructorName, '%')))
            AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
            AND (:toDate IS NULL OR c.createdAt <= :toDate)
            AND (:categoryIds IS NULL OR cat.id IN :categoryIds)
            """)
    Page<Course> searchCourses(
            @Param("title") String title,
            @Param("status") CourseStatus status,
            @Param("instructorName") String instructorName,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("categoryIds") Set<Long> categoryIds,
            Pageable pageable
    );

    // Update operations
    @Modifying
    @Query("""
            UPDATE Course c 
            SET c.title = :title, 
                c.description = :description,
                c.status = :status,
                c.updatedAt = CURRENT_TIMESTAMP 
            WHERE c.id = :id
            """)
    int updateCourse(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("description") String description,
            @Param("status") CourseStatus status
    );

    // Archive operation (special form of soft delete)
    @Modifying
    @Query("""
            UPDATE Course c 
            SET c.status = 'ARCHIVED', 
                c.deletedAt = CURRENT_TIMESTAMP,
                c.updatedAt = CURRENT_TIMESTAMP
            WHERE c.id = :courseId
            """)
    void archiveCourse(@Param("courseId") Long courseId);

    @Modifying
    @Query("""
            UPDATE Course c 
            SET c.status = 'DRAFT',
                c.updatedAt = CURRENT_TIMESTAMP
            WHERE c.id = :courseId AND c.status = 'ARCHIVED'
            """)
    void unarchiveCourse(@Param("courseId") Long courseId);

    @Query("""
            SELECT c FROM Course c 
            LEFT JOIN FETCH c.instructor i 
            LEFT JOIN FETCH i.userRoles r 
            LEFT JOIN FETCH r.role 
            WHERE c.status = :status 
            AND c.deletedAt IS NULL
            """)
    Page<Course> findByStatus(@Param("status") CourseStatus status, Pageable pageable);

    @Query("SELECT COUNT(c) FROM Course c WHERE c.status = :status")
    long countCoursesByStatus(@Param("status") CourseStatus status);

    // Category relationship operations
    @Modifying
    @Query(value = "INSERT INTO course_categories (course_id, category_id) VALUES (:courseId, :categoryId)", nativeQuery = true)
    void addCourseCategories(@Param("courseId") Long courseId, @Param("categoryId") Set<Long> categoryId);

    @Modifying
    @Query(value = "DELETE FROM course_categories WHERE course_id = :courseId AND category_id = :categoryId", nativeQuery = true)
    void removeCourseCategories(@Param("courseId") Long courseId, @Param("categoryId") Set<Long> categoryId);

    @Query("""
            SELECT c FROM Course c 
            LEFT JOIN FETCH c.instructor i 
            LEFT JOIN FETCH i.userRoles r 
            LEFT JOIN FETCH r.role 
            WHERE c.status != 'ARCHIVED' 
            AND c.deletedAt IS NULL 
            ORDER BY c.createdAt DESC
            """)
    List<Course> findLatestCourses(Pageable pageable);
}
