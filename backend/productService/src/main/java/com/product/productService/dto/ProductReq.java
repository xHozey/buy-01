package com.product.productService.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductReq(
        @NotBlank @Size(max = 500, message = "name must be 500 characters or fewer") String name,
        @NotBlank @Size(max = 500, message = "description must be 500 characters or fewer") String description,
        @NotNull @DecimalMin("0.0") Double price,
        @NotNull @Min(0) Integer quantity,
        @Size(max = 5, message = "imageUrls must contain 5 items or fewer")
        String[] imageUrls) {
}