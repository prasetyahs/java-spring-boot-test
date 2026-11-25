package com.test.product.controller;

import com.test.product.dto.ApiResponse;
import com.test.product.dto.CategoryPriceStatProjection;
import com.test.product.dto.ProductRequest;
import com.test.product.dto.ProductResponse;
import com.test.product.dto.ProductSummaryResponse;
import com.test.product.service.ProductService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    // Spring IoC: Constructor Injection via interface
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStockOnly,
            @RequestParam(required = false) String sortBy
    ) {
        List<ProductResponse> products = productService.findAll(keyword, categoryId, minPrice, maxPrice, inStockOnly, sortBy);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable Long id) {
        ProductResponse product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable Long id,
            @RequestBody ProductRequest request
    ) {
        ProductResponse updated = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/stats/by-category")
    public ResponseEntity<ApiResponse<List<CategoryPriceStatProjection>>> getCategoryStats(
            @RequestParam(defaultValue = "1") long minProductCount
    ) {
        List<CategoryPriceStatProjection> stats = productService.getCategoryStats(minProductCount);
        return ResponseEntity.ok(ApiResponse.success("Product category statistics retrieved successfully", stats));
    }

    @GetMapping("/top-expensive")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getTopExpensivePerCategory(
            @RequestParam(defaultValue = "3") int topN
    ) {
        List<ProductResponse> topProducts = productService.getTopExpensivePerCategory(topN);
        return ResponseEntity.ok(ApiResponse.success("Top expensive products retrieved successfully", topProducts));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ProductSummaryResponse>> getSummary() {
        ProductSummaryResponse summary = productService.getSummary();
        return ResponseEntity.ok(ApiResponse.success("Product summary retrieved successfully", summary));
    }
}
