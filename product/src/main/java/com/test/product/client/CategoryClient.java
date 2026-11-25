package com.test.product.client;

import com.test.product.dto.CategoryDto;
import com.test.product.exception.ExternalServiceException;
import com.test.product.exception.ResourceNotFoundException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class CategoryClient {

    private final RestClient categoryRestClient;

    // Spring IoC: Constructor Injection of @Bean RestClient
    public CategoryClient(RestClient categoryRestClient) {
        this.categoryRestClient = categoryRestClient;
    }

    public CategoryDto getCategoryById(Long categoryId) {
        try {
            CategoryDto category = categoryRestClient.get()
                    .uri("/api/categories/{id}", categoryId)
                    .retrieve()
                    .body(CategoryDto.class);
            if (category == null) {
                throw new ResourceNotFoundException("Category not found with id: " + categoryId);
            }
            return category;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Category not found with id: " + categoryId);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to communicate with category-service: " + ex.getMessage(), ex);
        }
    }

    public List<CategoryDto> getAllCategories() {
        try {
            List<CategoryDto> categories = categoryRestClient.get()
                    .uri("/api/categories")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return categories != null ? categories : Collections.emptyList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }
}
