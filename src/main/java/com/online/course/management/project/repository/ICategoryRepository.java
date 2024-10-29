package com.online.course.management.project.repository;

import com.online.course.management.project.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ICategoryRepository extends JpaRepository<Category, Long>, JpaSpecificationExecutor<Category> {
    // Create & update operations
    @Override
    <S extends Category> S save(S category);

    // Read operations
    Optional<Category> findById(Long id);

    boolean existsByName(String name);

    @Query("""
            SELECT c FROM Category c 
            WHERE (:archived IS NULL 
                OR (:archived = true AND c.deletedAt IS NOT NULL)
                OR (:archived = false AND c.deletedAt IS NULL)
            )
            AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
            AND (:toDate IS NULL OR c.createdAt <= :toDate)
            """)
    Page<Category> searchCategories(
            @Param("name") String name,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("archived") Boolean archived,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(c) FROM Category c 
            WHERE (:archived IS NULL 
                OR (:archived = true AND c.deletedAt IS NOT NULL)
                OR (:archived = false AND c.deletedAt IS NULL)
            )
            AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
            AND (:toDate IS NULL OR c.createdAt <= :toDate)
            """)
    long countCategories(

            @Param("name") String name,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("archived") Boolean archived
    );

    // Delete operations (soft delete)
    @Modifying
    @Query("UPDATE Category c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void softDeleteCategory(@Param("id") Long id);

    // Restore operation
    @Modifying
    @Query("UPDATE Category c SET c.deletedAt = NULL, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void restoreCategory(@Param("id") Long id);

}
