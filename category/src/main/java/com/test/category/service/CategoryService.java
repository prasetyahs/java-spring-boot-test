package com.test.category.service;

import com.test.category.dto.CategoryRequest;
import com.test.category.dto.CategoryResponse;
import com.test.category.dto.CategoryStatsProjection;

import java.util.List;
import java.util.Map;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    List<CategoryResponse> findAll(String keyword, Boolean active, String sortBy);

    CategoryResponse findById(Long id);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);

    List<CategoryStatsProjection> getStats();

    Map<Boolean, List<CategoryResponse>> groupByActiveStatus();
}
