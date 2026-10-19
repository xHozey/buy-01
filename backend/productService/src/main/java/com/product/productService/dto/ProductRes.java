package com.product.productService.dto;

public record ProductRes(String id, String name, String description, Double price, Integer quantity, String userId,
        String[] imageUrls) {
}