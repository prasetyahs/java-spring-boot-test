package com.test.product.repository;

import com.test.product.dto.CategoryPriceStatProjection;
import com.test.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = """
            SELECT p.id, p.name, p.description, p.price, p.stock, p.category_id, p.created_at, p.updated_at
            FROM products p
            WHERE (CAST(:keyword AS TEXT) IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%'))
                   OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS TEXT), '%')))
              AND (CAST(:categoryId AS BIGINT) IS NULL OR p.category_id = CAST(:categoryId AS BIGINT))
              AND (CAST(:minPrice AS NUMERIC) IS NULL OR p.price >= CAST(:minPrice AS NUMERIC))
              AND (CAST(:maxPrice AS NUMERIC) IS NULL OR p.price <= CAST(:maxPrice AS NUMERIC))
              AND (CAST(:inStockOnly AS BOOLEAN) IS NULL OR CAST(:inStockOnly AS BOOLEAN) = false OR p.stock > 0)
            ORDER BY p.created_at DESC, p.id ASC
            """, nativeQuery = true)
    List<Product> searchProductsNative(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("inStockOnly") Boolean inStockOnly
    );

    @Query(value = """
            SELECT id, name, description, price, stock, category_id, created_at, updated_at
            FROM products
            WHERE id = :id
            """, nativeQuery = true)
    Optional<Product> findByIdNative(@Param("id") Long id);

    // Intermediate Native SQL: GROUP BY + HAVING + Aggregate Functions
    @Query(value = """
            SELECT p.category_id AS categoryId,
                   COUNT(p.id) AS totalProducts,
                   COALESCE(SUM(p.stock), 0) AS totalStock,
                   ROUND(AVG(p.price), 2) AS avgPrice,
                   MIN(p.price) AS minPrice,
                   MAX(p.price) AS maxPrice
            FROM products p
            GROUP BY p.category_id
            HAVING COUNT(p.id) >= :minProductCount
            ORDER BY totalProducts DESC, avgPrice DESC
            """, nativeQuery = true)
    List<CategoryPriceStatProjection> findCategoryStatsNative(@Param("minProductCount") long minProductCount);

    // Intermediate Native SQL: CTE + Window Function (DENSE_RANK)
    @Query(value = """
            WITH ranked_products AS (
                SELECT p.id,
                       p.name,
                       p.description,
                       p.price,
                       p.stock,
                       p.category_id,
                       p.created_at,
                       p.updated_at,
                       DENSE_RANK() OVER (
                           PARTITION BY p.category_id
                           ORDER BY p.price DESC, p.id ASC
                       ) AS price_rank
                FROM products p
            )
            SELECT id, name, description, price, stock, category_id, created_at, updated_at
            FROM ranked_products
            WHERE price_rank <= :topN
            ORDER BY category_id ASC, price DESC, id ASC
            """, nativeQuery = true)
    List<Product> findTopExpensivePerCategoryNative(@Param("topN") int topN);

    @Modifying
    @Query(value = """
            UPDATE products
            SET name = :name,
                description = :description,
                price = :price,
                stock = :stock,
                category_id = :categoryId,
                updated_at = :updatedAt
            WHERE id = :id
            """, nativeQuery = true)
    int updateProductNative(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("price") BigDecimal price,
            @Param("stock") Integer stock,
            @Param("categoryId") Long categoryId,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying
    @Query(value = "DELETE FROM products WHERE id = :id", nativeQuery = true)
    int deleteByIdNative(@Param("id") Long id);
}
