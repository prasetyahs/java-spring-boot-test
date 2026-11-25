package com.test.category.repository;

import com.test.category.dto.CategoryStatsProjection;
import com.test.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query(value = """
            SELECT c.id, c.name, c.description, c.active, c.created_at, c.updated_at
            FROM categories c
            WHERE (CAST(:keyword AS TEXT) IS NULL
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%'))
                   OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
              AND (CAST(:active AS BOOLEAN) IS NULL OR c.active = CAST(:active AS BOOLEAN))
            ORDER BY c.created_at DESC, c.id ASC
            """, nativeQuery = true)
    List<Category> searchCategoriesNative(
            @Param("keyword") String keyword,
            @Param("active") Boolean active
    );

    @Query(value = """
            SELECT id, name, description, active, created_at, updated_at
            FROM categories
            WHERE id = :id
            """, nativeQuery = true)
    Optional<Category> findByIdNative(@Param("id") Long id);

    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
            FROM categories
            WHERE LOWER(name) = LOWER(:name)
              AND (CAST(:excludeId AS BIGINT) IS NULL OR id <> CAST(:excludeId AS BIGINT))
            """, nativeQuery = true)
    boolean existsByNameIgnoreCaseNative(
            @Param("name") String name,
            @Param("excludeId") Long excludeId
    );

    @Query(value = """
            SELECT c.active AS active,
                   COUNT(c.id) AS totalCategories
            FROM categories c
            GROUP BY c.active
            ORDER BY c.active DESC
            """, nativeQuery = true)
    List<CategoryStatsProjection> findCategoryStatsNative();

    @Modifying
    @Query(value = """
            UPDATE categories
            SET name = :name,
                description = :description,
                active = :active,
                updated_at = :updatedAt
            WHERE id = :id
            """, nativeQuery = true)
    int updateCategoryNative(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("active") Boolean active,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying
    @Query(value = "DELETE FROM categories WHERE id = :id", nativeQuery = true)
    int deleteByIdNative(@Param("id") Long id);
}
