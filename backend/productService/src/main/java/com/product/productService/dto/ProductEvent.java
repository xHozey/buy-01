package com.product.productService.dto;

public record ProductEvent(String operation, String productId, String[] imageUrls) {
}