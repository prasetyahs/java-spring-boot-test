package com.test.category.service.impl;

import com.test.category.dto.CategoryRequest;
import com.test.category.dto.CategoryResponse;
import com.test.category.dto.CategoryStatsProjection;
import com.test.category.entity.Category;
import com.test.category.exception.DuplicateResourceException;
import com.test.category.exception.ResourceNotFoundException;
import com.test.category.repository.CategoryRepository;
import com.test.category.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    // Spring IoC: Constructor Injection
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        validateRequest(request);
        String trimmedName = request.name().trim();

        if (categoryRepository.existsByNameIgnoreCaseNative(trimmedName, null)) {
            throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists");
        }

        Category category = new Category(
                trimmedName,
                request.description() != null ? request.description().trim() : null,
                request.active() != null ? request.active() : true
        );
        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll(String keyword, Boolean active, String sortBy) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Comparator<Category> comparator = resolveComparator(sortBy);

        return categoryRepository.searchCategoriesNative(normalizedKeyword, active)
                .stream()
                .sorted(comparator)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return categoryRepository.findByIdNative(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        validateRequest(request);

        Category existing = categoryRepository.findByIdNative(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String trimmedName = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseNative(trimmedName, id)) {
            throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists");
        }

        String updatedDescription = request.description() != null ? request.description().trim() : null;
        Boolean updatedActive = request.active() != null ? request.active() : existing.getActive();
        LocalDateTime now = LocalDateTime.now();

        categoryRepository.updateCategoryNative(id, trimmedName, updatedDescription, updatedActive, now);

        return new CategoryResponse(
                existing.getId(),
                trimmedName,
                updatedDescription,
                updatedActive,
                existing.getCreatedAt(),
                now
        );
    }

    @Override
    @Transactional
    public void delete(Long id) {
        int deleted = categoryRepository.deleteByIdNative(id);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryStatsProjection> getStats() {
        return categoryRepository.findCategoryStatsNative();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Boolean, List<CategoryResponse>> groupByActiveStatus() {
        return categoryRepository.searchCategoriesNative(null, null)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.groupingBy(CategoryResponse::active));
    }

    private void validateRequest(CategoryRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
    }

    private Comparator<Category> resolveComparator(String sortBy) {
        if ("name".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER);
        }
        if ("oldest".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(Category::getCreatedAt).thenComparing(Category::getId);
        }
        return Comparator.comparing(Category::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(Category::getId);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
