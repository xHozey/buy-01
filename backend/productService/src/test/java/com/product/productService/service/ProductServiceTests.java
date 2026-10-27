package com.product.productService.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.product.productService.config.ProductTopic;
import com.product.productService.dto.ProductEvent;
import com.product.productService.dto.ProductReq;
import com.product.productService.dto.ProductRes;
import com.product.productService.model.Product;
import com.product.productService.reposiroty.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTests {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, ProductEvent> kafkaTemplate;

    @Mock
    private MediaClient mediaClient;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProductService productService;

    @Test
    void getAllReturnsProductsWithMediaUrls() {
        Product product = product("product-1", "seller-1");
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(mediaClient.getImageUrls("product-1")).thenReturn(new String[] { "image-1" });

        List<ProductRes> result = productService.getAll();

        assertEquals(1, result.size());
        assertProductResponse(result.get(0), product, "image-1");
    }

    @Test
    void getMineUsesAuthenticatedUserId() {
        Product product = product("product-1", "seller-1");
        when(authentication.getName()).thenReturn("seller-1");
        when(productRepository.findAllByUserId("seller-1")).thenReturn(List.of(product));
        when(mediaClient.getImageUrls("product-1")).thenReturn(new String[0]);

        List<ProductRes> result = productService.getMine(authentication);

        verify(productRepository).findAllByUserId("seller-1");
        assertEquals("product-1", result.get(0).id());
    }

    @Test
    void getByIdReturnsProductWithMediaUrls() {
        Product product = product("product-1", "seller-1");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));
        when(mediaClient.getImageUrls("product-1")).thenReturn(new String[] { "image-1", "image-2" });

        ProductRes result = productService.getById("product-1");

        assertProductResponse(result, product, "image-1", "image-2");
    }

    @Test
    void createSavesProductAndPublishesCreateEvent() {
        ProductReq request = request(new String[] { "image-1" });
        Product savedProduct = product("product-1", "seller-1");
        when(authentication.getName()).thenReturn("seller-1");
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductRes result = productService.create(request, authentication);

        verify(productRepository).save(any(Product.class));
        ProductEvent event = publishedEvent();
        assertEquals("CREATE", event.operation());
        assertEquals("product-1", event.productId());
        assertArrayEquals(request.imageUrls(), event.imageUrls());
        assertProductResponse(result, savedProduct, "image-1");
    }

    @Test
    void updateRequiresOwnershipAndPublishesUpdateEvent() {
        Product existingProduct = product("product-1", "seller-1");
        ProductReq request = request(new String[] { "updated-image" });
        when(authentication.getName()).thenReturn("seller-1");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(existingProduct)).thenReturn(existingProduct);

        ProductRes result = productService.update("product-1", request, authentication);

        assertEquals("new name", existingProduct.getName());
        assertEquals("new description", existingProduct.getDescription());
        assertEquals(25.0, existingProduct.getPrice());
        assertEquals(4, existingProduct.getQuantity());
        ProductEvent event = publishedEvent();
        assertEquals("UPDATE", event.operation());
        assertEquals("product-1", event.productId());
        assertArrayEquals(request.imageUrls(), event.imageUrls());
        assertProductResponse(result, existingProduct, "updated-image");
    }

    @Test
    void deleteRequiresOwnershipAndPublishesDeleteEvent() {
        Product product = product("product-1", "seller-1");
        when(authentication.getName()).thenReturn("seller-1");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

        productService.delete("product-1", authentication);

        verify(productRepository).delete(product);
        ProductEvent event = publishedEvent();
        assertEquals("DELETE", event.operation());
        assertEquals("product-1", event.productId());
        assertEquals(null, event.imageUrls());
    }

    @Test
    void updateRejectsProductOwnedByAnotherUser() {
        Product product = product("product-1", "seller-1");
        when(authentication.getName()).thenReturn("seller-2");
        when(productRepository.findById("product-1")).thenReturn(Optional.of(product));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.update("product-1", request(null), authentication));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void getByIdThrowsNotFoundForUnknownProduct() {
        when(productRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.getById("missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void createRejectsNullImageUrl() {
        ProductReq request = request(new String[] { null });

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.create(request, authentication));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createRejectsImageUrlLongerThan500Characters() {
        ProductReq request = request(new String[] { "x".repeat(501) });

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> productService.create(request, authentication));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void sendProductPublishesToProductTopic() {
        ProductEvent event = new ProductEvent("CREATE", "product-1", new String[] { "image-1" });

        productService.sendProduct(event);

        verify(kafkaTemplate).send(ProductTopic.NAME, "product-1", event);
    }

    private ProductEvent publishedEvent() {
        ArgumentCaptor<ProductEvent> eventCaptor = ArgumentCaptor.forClass(ProductEvent.class);
        verify(kafkaTemplate).send(eq(ProductTopic.NAME), any(String.class), eventCaptor.capture());
        return eventCaptor.getValue();
    }

    private static ProductReq request(String[] imageUrls) {
        return new ProductReq("new name", "new description", 25.0, 4, imageUrls);
    }

    private static Product product(String id, String userId) {
        return new Product(id, "new name", "new description", 25.0, 4, userId);
    }

    private static void assertProductResponse(ProductRes response, Product product, String... imageUrls) {
        assertEquals(product.getId(), response.id());
        assertEquals(product.getName(), response.name());
        assertEquals(product.getDescription(), response.description());
        assertEquals(product.getPrice(), response.price());
        assertEquals(product.getQuantity(), response.quantity());
        assertEquals(product.getUserId(), response.userId());
        assertArrayEquals(imageUrls, response.imageUrls());
    }

}