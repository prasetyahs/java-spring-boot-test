package com.test.product.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductSummaryResponse(
        long totalProducts,
        int totalStock,
        BigDecimal averagePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Map<String, List<ProductResponse>> productsByCategory
) {
}
