package com.online.course.management.project.repository;

import com.online.course.management.project.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IChapterRepository extends JpaRepository<Chapter, Long>, JpaSpecificationExecutor<Chapter> {

    // Create & update operations
    @Override
    <S extends Chapter> S save(S chapter);

    // Search chapters with basic filters
    @Query(value = """
            SELECT ch.*
            FROM chapters ch
            WHERE (:title IS NULL OR LOWER(ch.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:status IS NULL OR ch.status = :status)
            AND (:courseId IS NULL OR ch.course_id = :courseId)
            AND (:fromDate IS NULL OR ch.created_at >= :fromDate)
            AND (:toDate IS NULL OR ch.created_at <= :toDate)
            AND ch.deleted_at IS NULL
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM chapters ch
                    WHERE (:title IS NULL OR LOWER(ch.title) LIKE LOWER(CONCAT('%', :title, '%')))
                    AND (:status IS NULL OR ch.status = :status)
                    AND (:courseId IS NULL OR ch.course_id = :courseId)
                    AND (:fromDate IS NULL OR ch.created_at >= :fromDate)
                    AND (:toDate IS NULL OR ch.created_at <= :toDate)
                    AND ch.deleted_at IS NULL
                    """,
            nativeQuery = true)
    Page<Chapter> searchChapters(
            @Param("title") String title,
            @Param("status") String status,
            @Param("courseId") Long courseId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    // Find all chapters by course ID
    @Query(value = """ 
            select *
            from chapters ch
            where ch.course_id = :courseId
            and ch.deleted_at is null
            order by ch.order_number
            """
            ,
            nativeQuery = true)
    List<Chapter> findAllChaptersByCourseId(@Param("courseId") Long courseId);


    // Batch soft delete chapters and their lessons
    @Modifying
    @Query(value = """
                   UPDATE chapters
                   SET\s
                       deleted_at = CURRENT_TIMESTAMP,
                       updated_at = CURRENT_TIMESTAMP,
                       status = 'ARCHIVED'
                   WHERE id IN (:chapterIds)
            """, nativeQuery = true)
    void batchSoftDeleteChapters(@Param("chapterIds") List<Long> chapterIds);

    @Modifying
    @Query(value = """
            UPDATE lessons
                    SET\s
                        deleted_at = CURRENT_TIMESTAMP,
                        updated_at = CURRENT_TIMESTAMP,
                        status = 'ARCHIVED'
                    WHERE chapter_id IN (:chapterIds)
                    AND deleted_at IS NULL
            """, nativeQuery = true)
    void batchSoftDeleteLessonsChapters(@Param("chapterIds") List<Long> chapterIds);

    // Restore soft deleted chapters and their lessons
    @Modifying
    @Query(value = """ 
            update chapters
            set deleted_at = null,
                status = 'DRAFT',
                updated_at = CURRENT_TIMESTAMP
            where id in (:chapterIds)
            and deleted_at is not null;
            """,
            nativeQuery = true)
    void batchRestoreChapters(@Param("chapterIds") List<Long> chapterIds);

    @Modifying
    @Query(value = """
            UPDATE lessons
                    SET\s
                        deleted_at = null,
                        updated_at = CURRENT_TIMESTAMP,
                        status = 'DRAFT'
                    WHERE chapter_id IN (:chapterIds)
                    AND deleted_at IS NOT NULL
            """, nativeQuery = true)
    void batchRestoreLessonsChapters(@Param("chapterIds") List<Long> chapterIds);

    // Check if chapters exist
    @Query("SELECT COUNT(c) = :expectedCount FROM Chapter c WHERE c.id IN :ids")
    boolean validateChaptersExist(@Param("ids") List<Long> ids, @Param("expectedCount") int expectedCount);

    // Validate chapter order in course
    @Query("""
            SELECT exists (
            select 1
            FROM Chapter ch
            WHERE ch.course.id = :courseId
            AND ch.order = :order
            AND ch.deletedAt IS NULL )
            """)
    boolean isOrderNumberChapterTaken(
            @Param("courseId") Long courseId,
            @Param("order") Integer order
    );

    // Call stored procedure to reorder chapters
    @Procedure(procedureName = "sp_reorder_chapters")
    void reorderChapters(@Param("p_course_id") Long courseId);
}
