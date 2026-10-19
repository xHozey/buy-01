package com.product.productService.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductReq(
        @NotBlank String name,
        @NotBlank String description,
        @NotNull @DecimalMin("0.0") Double price,
        @NotNull @Min(0) Integer quantity,
        String[] imageUrls) {
}