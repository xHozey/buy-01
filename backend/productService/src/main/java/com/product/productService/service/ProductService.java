package com.product.productService.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.product.productService.dto.ProductReq;
import com.product.productService.dto.ProductRes;
import com.product.productService.config.ProductTopic;
import com.product.productService.model.Product;
import com.product.productService.reposiroty.ProductRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductRes> kafkaTemplate;

    public List<ProductRes> getAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ProductRes getById(String productId) {
        return toResponse(findProduct(productId));
    }

    public ProductRes create(ProductReq req, Authentication authentication) {
        requireSeller(authentication);

        Product product = new Product(null, req.name(), req.description(), req.price(), req.quantity(),
                authentication.getName());
        ProductRes response = toResponse(productRepository.save(product), req.imageUrls());
        sendProduct(response);
        return response;
    }

    public void sendProduct(ProductRes product) {
        kafkaTemplate.send(ProductTopic.NAME, product.id(), product);
    }

    public ProductRes update(String productId, ProductReq req, Authentication authentication) {
        Product product = findOwnedProduct(productId, authentication);

        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setQuantity(req.quantity());
        return toResponse(productRepository.save(product));
    }

    public void delete(String productId, Authentication authentication) {
        Product product = findOwnedProduct(productId, authentication);
        productRepository.delete(product);
    }

    private Product findOwnedProduct(String productId, Authentication authentication) {
        requireSeller(authentication);
        Product product = findProduct(productId);
        if (!authentication.getName().equals(product.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this product");
        }
        return product;
    }

    private Product findProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    private void requireSeller(Authentication authentication) {
        boolean seller = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SELLER")
                        || authority.getAuthority().equals("SELLER"));
        if (!seller) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller role required");
        }
    }

    private ProductRes toResponse(Product product) {
        return new ProductRes(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getUserId(), null);
    }

    private ProductRes toResponse(Product product, String[] imageUrls) {
        return new ProductRes(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getUserId(), imageUrls);
    }
}