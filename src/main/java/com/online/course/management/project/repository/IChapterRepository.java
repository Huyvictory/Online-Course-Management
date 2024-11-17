package com.online.course.management.project.repository;

import com.online.course.management.project.entity.Chapter;
import com.online.course.management.project.entity.Lesson;
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
import java.util.Optional;

@Repository
public interface IChapterRepository extends JpaRepository<Chapter, Long>, JpaSpecificationExecutor<Chapter> {

    // Create & update operations
    @Override
    <S extends Chapter> S save(S chapter);

    // Get chapter details along with its lessons count
    @Query(value = """
            SELECT ch.*,
            c.title as course_title,
            c.status as course_status,
            COUNT(l.id) as lesson_count
            FROM chapters ch
            LEFT JOIN courses c on ch.course_id = c.id
            LEFT JOIN lessons l on l.chapter_id = ch.id
            WHERE ch.id = :id
            GROUP BY ch.id, c.id, c.title, c.status
            """, nativeQuery = true
    )
    Optional<Chapter> findChapterDetailsWithLessonsCount(@Param("id") Long id);

    @Query(value = """
            select l.*
            from chapters ch
            left join lessons l on ch.id = l.chapter_id
            where ch.id = 1
            and l.deleted_at is null
            """, nativeQuery = true)
    List<Lesson[]> findLessonsByChapterId(Long id);

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

    // Batch create chapters
    @Modifying
    @Query(value = """
            INSERT INTO chapters 
                (course_id, title, description, order_number, status, created_at, updated_at)
            VALUES 
                (:#{#chapters.courseId}, :#{#chapters.title}, :#{#chapters.description}, 
                 :#{#chapters.orderNumber}, :#{#chapters.status}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    void batchCreateChapters(@Param("chapters") List<Chapter> chapters);

    // Batch update chapters
    @Modifying
    @Query(value = """
            UPDATE chapters 
            SET title = CASE id 
                    :titleCases
                END,
                description = CASE id
                    :descriptionCases
                END,
                status = CASE id
                    :statusCases
                END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id IN :ids
            """, nativeQuery = true)
    void batchUpdateChapters(
            @Param("ids") List<Long> ids,
            @Param("titleCases") String titleCases,
            @Param("descriptionCases") String descriptionCases,
            @Param("statusCases") String statusCases
    );

    @Query(value = """ 
            select c.*
            from chapters c
            where c.chapter_id in (:chapterIds)
            and c.delete_at is null
            order by c.order_number
            """, nativeQuery = true)
    List<Chapter> findRecentUpdatedChapters(List<Long> chapterIds);

    // Batch soft delete chapters and their lessons
    @Modifying
    @Query(value = """
            WITH chapters_to_delete AS (
                SELECT id FROM chapters 
                WHERE id IN (:chapterIds)
                AND deleted_at IS NULL
            )
            UPDATE chapters
            SET 
                deleted_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP,
                status = 'ARCHIVED'
            WHERE id IN (SELECT id FROM chapters_to_delete);
                        
            UPDATE lessons
            SET 
                deleted_at = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP,
                status = 'ARCHIVED'
            WHERE lessons.chapter_id IN (SELECT id FROM chapters_to_delete)
            AND lessons.deleted_at IS NULL;
            """, nativeQuery = true)
    void batchSoftDeleteChapters(@Param("chapterIds") List<Long> chapterIds);

    // Restore soft deleted chapters and their lessons
    @Modifying
    @Query(value = """ 
            with chapters_to_restore as (
                select id from chapters
                where id in (:chapterIds)
                and deleted_at is not null
            )
                        
            update chapters
            set deleted_at = null,
                status = 'DRAFT',
                updated_at = CURRENT_TIMESTAMP
            where id in (select id from chapters_to_restore);

            update lessons
            set deleted_at = null,
                status = 'DRAFT',
                updated_at = CURRENT_TIMESTAMP
            where lessons.chapter_id in (select id from chapters_to_restore)
            and lessons.deleted_at is not null;
            """,
            nativeQuery = true)
    void batchRestoreChapters(@Param("chapterIds") List<Long> chapterIds);

    // Check if chapters exist
    @Query("SELECT COUNT(c) = :expectedCount FROM Chapter c WHERE c.id IN :ids")
    boolean validateChaptersExist(@Param("ids") List<Long> ids, @Param("expectedCount") int expectedCount);

    // Check if course exists
    @Query("SELECT COUNT(c) > 0 FROM Course c WHERE c.id = :courseId AND c.deletedAt IS NULL")
    boolean courseExists(@Param("courseId") Long courseId);

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
