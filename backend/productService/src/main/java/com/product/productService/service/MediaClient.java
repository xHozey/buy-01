package com.product.productService.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MediaClient {
    private final RestClient restClient;

    public MediaClient(@Value("${media.service.url:http://media:8083}") String mediaServiceUrl) {
        this.restClient = RestClient.create(mediaServiceUrl);
    }

    public String[] getImageUrls(String productId) {
        String[] imageUrls = restClient.get()
                .uri("/api/v1/media/products/{productId}", productId)
                .retrieve()
                .body(String[].class);
        return imageUrls == null ? new String[0] : imageUrls;
    }
}