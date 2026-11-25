package com.test.product.service.impl;

import com.test.product.client.CategoryClient;
import com.test.product.dto.CategoryDto;
import com.test.product.dto.CategoryPriceStatProjection;
import com.test.product.dto.ProductRequest;
import com.test.product.dto.ProductResponse;
import com.test.product.dto.ProductSummaryResponse;
import com.test.product.entity.Product;
import com.test.product.exception.ResourceNotFoundException;
import com.test.product.repository.ProductRepository;
import com.test.product.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryClient categoryClient;

    // Spring IoC: Constructor Injection
    public ProductServiceImpl(ProductRepository productRepository, CategoryClient categoryClient) {
        this.productRepository = productRepository;
        this.categoryClient = categoryClient;
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        validateRequest(request);
        CategoryDto category = categoryClient.getCategoryById(request.categoryId());

        Product product = new Product(
                request.name().trim(),
                request.description() != null ? request.description().trim() : null,
                request.price(),
                request.stock(),
                category.id()
        );
        Product saved = productRepository.save(product);
        return toResponse(saved, category.name());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            String sortBy
    ) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Map<Long, String> categoryNameMap = loadCategoryNameMap();
        Comparator<Product> comparator = resolveComparator(sortBy);

        return productRepository.searchProductsNative(normalizedKeyword, categoryId, minPrice, maxPrice, inStockOnly)
                .stream()
                .sorted(comparator)
                .map(product -> toResponse(product, categoryNameMap.getOrDefault(product.getCategoryId(), "Unknown")))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findByIdNative(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        String categoryName = resolveCategoryNameSafely(product.getCategoryId());
        return toResponse(product, categoryName);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        validateRequest(request);

        Product existing = productRepository.findByIdNative(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        CategoryDto category = categoryClient.getCategoryById(request.categoryId());

        String updatedName = request.name().trim();
        String updatedDescription = request.description() != null ? request.description().trim() : null;
        LocalDateTime now = LocalDateTime.now();

        productRepository.updateProductNative(
                id,
                updatedName,
                updatedDescription,
                request.price(),
                request.stock(),
                category.id(),
                now
        );

        return new ProductResponse(
                existing.getId(),
                updatedName,
                updatedDescription,
                request.price(),
                request.stock(),
                category.id(),
                category.name(),
                existing.getCreatedAt(),
                now
        );
    }

    @Override
    @Transactional
    public void delete(Long id) {
        int deleted = productRepository.deleteByIdNative(id);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryPriceStatProjection> getCategoryStats(long minProductCount) {
        if (minProductCount < 1) {
            throw new IllegalArgumentException("minProductCount must be at least 1");
        }
        return productRepository.findCategoryStatsNative(minProductCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getTopExpensivePerCategory(int topN) {
        if (topN < 1) {
            throw new IllegalArgumentException("topN must be at least 1");
        }
        Map<Long, String> categoryNameMap = loadCategoryNameMap();

        return productRepository.findTopExpensivePerCategoryNative(topN)
                .stream()
                .map(product -> toResponse(product, categoryNameMap.getOrDefault(product.getCategoryId(), "Unknown")))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSummaryResponse getSummary() {
        List<Product> products = productRepository.searchProductsNative(null, null, null, null, null);
        Map<Long, String> categoryNameMap = loadCategoryNameMap();

        List<ProductResponse> responses = products.stream()
                .map(product -> toResponse(product, categoryNameMap.getOrDefault(product.getCategoryId(), "Unknown")))
                .toList();

        long totalProducts = responses.size();
        int totalStock = responses.stream()
                .mapToInt(ProductResponse::stock)
                .sum();

        BigDecimal totalPrice = responses.stream()
                .map(ProductResponse::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averagePrice = totalProducts == 0
                ? BigDecimal.ZERO
                : totalPrice.divide(BigDecimal.valueOf(totalProducts), 2, RoundingMode.HALF_UP);

        BigDecimal minPrice = responses.stream()
                .map(ProductResponse::price)
                .min(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = responses.stream()
                .map(ProductResponse::price)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        Map<String, List<ProductResponse>> productsByCategory = responses.stream()
                .sorted(Comparator.comparing(ProductResponse::price).reversed())
                .collect(Collectors.groupingBy(
                        ProductResponse::categoryName,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return new ProductSummaryResponse(
                totalProducts,
                totalStock,
                averagePrice,
                minPrice,
                maxPrice,
                productsByCategory
        );
    }

    private Map<Long, String> loadCategoryNameMap() {
        return categoryClient.getAllCategories()
                .stream()
                .collect(Collectors.toMap(
                        CategoryDto::id,
                        CategoryDto::name,
                        (existing, replacement) -> existing
                ));
    }

    private String resolveCategoryNameSafely(Long categoryId) {
        try {
            return categoryClient.getCategoryById(categoryId).name();
        } catch (RuntimeException ex) {
            return "Unknown";
        }
    }

    private void validateRequest(ProductRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Product price must be greater than or equal to 0");
        }
        if (request.stock() == null || request.stock() < 0) {
            throw new IllegalArgumentException("Product stock must be greater than or equal to 0");
        }
        if (request.categoryId() == null) {
            throw new IllegalArgumentException("Category id is required");
        }
    }

    private Comparator<Product> resolveComparator(String sortBy) {
        if ("priceAsc".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(Product::getPrice).thenComparing(Product::getId);
        }
        if ("priceDesc".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(Product::getPrice, Comparator.reverseOrder()).thenComparing(Product::getId);
        }
        if ("name".equalsIgnoreCase(sortBy)) {
            return Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
        }
        return Comparator.comparing(Product::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(Product::getId);
    }

    private ProductResponse toResponse(Product product, String categoryName) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategoryId(),
                categoryName,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
