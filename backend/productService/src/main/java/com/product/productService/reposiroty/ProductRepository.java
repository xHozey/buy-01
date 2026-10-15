package com.product.productService.reposiroty;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.product.productService.model.Product;

public interface ProductRepository extends MongoRepository<String, Product> {
}