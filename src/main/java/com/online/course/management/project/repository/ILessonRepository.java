package com.online.course.management.project.repository;

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
public interface ILessonRepository extends JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {

    @Override
    <S extends Lesson> S save(S lesson);

    // Find Lesson details by id
    @Query(value = """
            select l.*,
                   c.title  as course_title,
                   c.id     as course_id,
                   ch.title as chapter_title,
                   ch.id    as chapterId
            from lessons l
                     left join chapters ch on ch.id = l.chapter_id
                     left join courses c on c.id = ch.course_id
            where l.id = :lesson_id
            """, nativeQuery = true)
    Optional<Lesson> findLessonDetailsById(@Param("lesson_id") Long lesson_id);

    // Search lessons with filters
    @Query(value = """
            SELECT l.*,
                   c.id as courseId,
                   ch.id as chapterId
            FROM lessons l
            LEFT JOIN chapters ch ON l.chapter_id = ch.id
            LEFT JOIN courses c ON ch.course_id = c.id
            WHERE (:title IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :title, '%')))
            AND (:status IS NULL OR l.status = :status)
            AND (
                :#{#courseIds.size()} = 0\s
                OR c.id IN (:courseIds)
            )
            AND (
                :#{#chapterIds.size()} = 0\s
                OR ch.id IN (:chapterIds)
            )
            AND (:type IS NULL OR l.type = :type)
            AND (:fromDate IS NULL OR l.created_at >= :fromDate)
            AND (:toDate IS NULL OR l.created_at <= :toDate)
            AND l.deleted_at IS NULL
            """,
            countQuery = """
                    SELECT COUNT(l.id)
                    FROM lessons l
                    LEFT JOIN chapters ch ON l.chapter_id = ch.id
                    LEFT JOIN courses c ON ch.course_id = c.id
                    WHERE (:title IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :title, '%')))
                    AND (:status IS NULL OR l.status = :status)
                    AND (
                        :#{#courseIds.size()} = 0\s
                        OR c.id IN (:courseIds)
                    )
                    AND (
                        :#{#chapterIds.size()} = 0\s
                        OR ch.id IN (:chapterIds)
                    )
                    AND (:type IS NULL OR l.type = :type)
                    AND (:fromDate IS NULL OR l.created_at >= :fromDate)
                    AND (:toDate IS NULL OR l.created_at <= :toDate)
                    AND l.deleted_at IS NULL
                    """,
            nativeQuery = true)
    Page<Lesson> searchLessons(
            @Param("title") String title,
            @Param("status") String status,
            @Param("courseIds") List<Long> courseIds,
            @Param("chapterIds") List<Long> chapterIds,
            @Param("type") String type,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    // Remove soft deleted lessons
    @Modifying
    @Query(value = """
            update lessons
            set deleted_at = CURRENT_TIMESTAMP,
                 updated_at = CURRENT_TIMESTAMP,
                status = 'ARCHIVED'
            where id in (:lessonIds)
            and deleted_at is null;
            """,
            nativeQuery = true)
    void batchSoftDeleteLessons(@Param("lessonIds") List<Long> lessonsIds);

    // Restore soft deleted lessons
    @Modifying
    @Query(value = """         
            update lessons
            set deleted_at = null,
                 updated_at = CURRENT_TIMESTAMP,
                status = 'DRAFT'
            where id in (:lessonIds)
            and deleted_at is not null;
            """,
            nativeQuery = true)
    void batchRestoreLessons(@Param("lessonIds") List<Long> lessonsIds);

    @Query("""
            SELECT exists (
            FROM Lesson l 
            WHERE l.chapter.id = :chapterId 
            AND l.order = :order 
            AND l.deletedAt IS NULL )
            """)
    boolean isOrderNumberLessonTaken(@Param("chapterId") Long chapterId, @Param("order") Integer order);

    // Validate lessons belong to chapter
    @Query("""
            SELECT COUNT(l) = :expectedCount 
            FROM Lesson l 
            WHERE l.id IN :lessonIds 
            """)
    Boolean validateLessonsExists(
            @Param("lessonIds") List<Long> lessonIds,
            @Param("expectedCount") int expectedCount
    );

    // Call stored procedure to reorder lessons
    @Procedure(procedureName = "sp_reorder_lessons")
    void reorderLessons(@Param("p_chapter_id") Long chapterId);
}
