package com.test.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient categoryRestClient(@Value("${category.service.url}") String categoryServiceUrl) {
        return RestClient.builder()
                .baseUrl(categoryServiceUrl)
                .build();
    }
}
