package com.test.product.service;

import com.test.product.dto.CategoryPriceStatProjection;
import com.test.product.dto.ProductRequest;
import com.test.product.dto.ProductResponse;
import com.test.product.dto.ProductSummaryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductResponse create(ProductRequest request);

    List<ProductResponse> findAll(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            String sortBy
    );

    ProductResponse findById(Long id);

    ProductResponse update(Long id, ProductRequest request);

    void delete(Long id);

    List<CategoryPriceStatProjection> getCategoryStats(long minProductCount);

    List<ProductResponse> getTopExpensivePerCategory(int topN);

    ProductSummaryResponse getSummary();
}
