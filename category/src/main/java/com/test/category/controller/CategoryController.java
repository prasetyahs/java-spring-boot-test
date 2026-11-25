package com.test.category.controller;

import com.test.category.dto.ApiResponse;
import com.test.category.dto.CategoryRequest;
import com.test.category.dto.CategoryResponse;
import com.test.category.dto.CategoryStatsProjection;
import com.test.category.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    // Spring IoC: Constructor Injection via interface
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String sortBy
    ) {
        List<CategoryResponse> categories = categoryService.findAll(keyword, active, sortBy);
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(@PathVariable Long id) {
        CategoryResponse category = categoryService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved successfully", category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @RequestBody CategoryRequest request
    ) {
        CategoryResponse updated = categoryService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<CategoryStatsProjection>>> getStats() {
        List<CategoryStatsProjection> stats = categoryService.getStats();
        return ResponseEntity.ok(ApiResponse.success("Category statistics retrieved successfully", stats));
    }

    @GetMapping("/grouped")
    public ResponseEntity<ApiResponse<Map<Boolean, List<CategoryResponse>>>> groupByActiveStatus() {
        Map<Boolean, List<CategoryResponse>> grouped = categoryService.groupByActiveStatus();
        return ResponseEntity.ok(ApiResponse.success("Grouped categories retrieved successfully", grouped));
    }
}
