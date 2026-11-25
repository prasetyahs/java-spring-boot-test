package com.test.product.dto;

import java.math.BigDecimal;

public interface CategoryPriceStatProjection {
    Long getCategoryId();
    Long getTotalProducts();
    Long getTotalStock();
    BigDecimal getAvgPrice();
    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
}
