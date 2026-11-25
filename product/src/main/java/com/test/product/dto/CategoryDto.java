package com.test.product.dto;

public record CategoryDto(
        Long id,
        String name,
        String description,
        Boolean active
) {
}
