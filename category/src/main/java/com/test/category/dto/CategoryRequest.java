package com.test.category.dto;

public record CategoryRequest(
        String name,
        String description,
        Boolean active
) {
}
