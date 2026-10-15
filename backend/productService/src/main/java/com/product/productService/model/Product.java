package com.product.productService.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("product")
public class Product {
    @Id
    String id;
    String name;
    String description;
    Double price;
    Integer quantity;
    String userId;
}