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
    // Create operations
    @Override
    <S extends Category> S save(S category);

    // Read operations
    Optional<Category> findById(Long id);

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    @Query("""
            SELECT c FROM Category c 
            WHERE c.deletedAt IS NULL 
            AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%')))
            AND (:fromDate IS NULL OR c.createdAt >= :fromDate)
            AND (:toDate IS NULL OR c.createdAt <= :toDate)
            """)
    Page<Category> searchCategories(
            @Param("name") String name,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("SELECT c FROM Category c WHERE c.deletedAt IS NULL")
    Page<Category> findNonArchivedCategories(Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c WHERE c.id = :id AND c.deletedAt IS NULL")
    boolean isActiveCategory(@Param("id") Long id);

    // Update operations
    @Modifying
    @Query("UPDATE Category c SET c.name = :name, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    int updateCategory(@Param("id") Long id, @Param("name") String name);

    // Delete operations (soft delete)
    @Modifying
    @Query("UPDATE Category c SET c.deletedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void softDeleteCategory(@Param("id") Long id);

    // Restore operation
    @Modifying
    @Query("UPDATE Category c SET c.deletedAt = NULL, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void restoreCategory(@Param("id") Long id);

}
