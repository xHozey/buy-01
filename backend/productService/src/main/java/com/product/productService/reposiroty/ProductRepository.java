package com.product.productService.reposiroty;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.product.productService.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
	List<Product> findAllByUserId(String userId);
}