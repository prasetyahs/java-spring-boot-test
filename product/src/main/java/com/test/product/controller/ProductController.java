package com.test.product.controller;

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
    public ResponseEntity<ProductResponse> create(@RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStockOnly,
            @RequestParam(required = false) String sortBy
    ) {
        return ResponseEntity.ok(productService.findAll(keyword, categoryId, minPrice, maxPrice, inStockOnly, sortBy));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/by-category")
    public ResponseEntity<List<CategoryPriceStatProjection>> getCategoryStats(
            @RequestParam(defaultValue = "1") long minProductCount
    ) {
        return ResponseEntity.ok(productService.getCategoryStats(minProductCount));
    }

    @GetMapping("/top-expensive")
    public ResponseEntity<List<ProductResponse>> getTopExpensivePerCategory(
            @RequestParam(defaultValue = "3") int topN
    ) {
        return ResponseEntity.ok(productService.getTopExpensivePerCategory(topN));
    }

    @GetMapping("/summary")
    public ResponseEntity<ProductSummaryResponse> getSummary() {
        return ResponseEntity.ok(productService.getSummary());
    }
}
