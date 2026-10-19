package com.product.productService.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.product.productService.dto.ProductReq;
import com.product.productService.dto.ProductRes;
import com.product.productService.dto.ProductEvent;
import com.product.productService.config.ProductTopic;
import com.product.productService.model.Product;
import com.product.productService.reposiroty.ProductRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductEvent> kafkaTemplate;
    private final MediaClient mediaClient;

    public List<ProductRes> getAll() {
        return productRepository.findAll().stream().map(product -> toResponse(product, mediaClient.getImageUrls(product.getId())))
                .toList();
    }

    public List<ProductRes> getMine(Authentication authentication) {
        requireSeller(authentication);
        return productRepository.findAllByUserId(authentication.getName()).stream()
                .map(product -> toResponse(product, mediaClient.getImageUrls(product.getId())))
                .toList();
    }

    public ProductRes getById(String productId) {
        Product product = findProduct(productId);
        return toResponse(product, mediaClient.getImageUrls(product.getId()));
    }

    public ProductRes create(ProductReq req, Authentication authentication) {
        requireSeller(authentication);

        Product product = new Product(null, req.name(), req.description(), req.price(), req.quantity(),
                authentication.getName());
        Product savedProduct = productRepository.save(product);
        ProductRes response = toResponse(savedProduct, req.imageUrls());
        sendProduct(new ProductEvent("CREATE", savedProduct.getId(), req.imageUrls()));
        return response;
    }

    public void sendProduct(ProductEvent productEvent) {
        kafkaTemplate.send(ProductTopic.NAME, productEvent.productId(), productEvent);
    }

    public ProductRes update(String productId, ProductReq req, Authentication authentication) {
        Product product = findOwnedProduct(productId, authentication);

        product.setName(req.name());
        product.setDescription(req.description());
        product.setPrice(req.price());
        product.setQuantity(req.quantity());
        Product savedProduct = productRepository.save(product);
        sendProduct(new ProductEvent("UPDATE", savedProduct.getId(), req.imageUrls()));
        return toResponse(savedProduct, req.imageUrls());
    }

    public void delete(String productId, Authentication authentication) {
        Product product = findOwnedProduct(productId, authentication);
        productRepository.delete(product);
        sendProduct(new ProductEvent("DELETE", productId, null));
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

    private ProductRes toResponse(Product product, String[] imageUrls) {
        return new ProductRes(product.getId(), product.getName(), product.getDescription(), product.getPrice(),
                product.getQuantity(), product.getUserId(), imageUrls);
    }
}