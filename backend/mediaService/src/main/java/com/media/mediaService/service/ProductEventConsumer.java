package com.media.mediaService.service;

import java.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.mediaService.model.Media;
import com.media.mediaService.reposiroty.MediaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ProductEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ProductEventConsumer.class);
    private final ObjectMapper objectMapper;
    private final MediaRepository mediaRepository;

    public ProductEventConsumer(ObjectMapper objectMapper, MediaRepository mediaRepository) {
        this.objectMapper = objectMapper;
        this.mediaRepository = mediaRepository;
    }

    @KafkaListener(topics = "product-topic", groupId = "media-service")
    public void consumeProduct(String productMessage) throws JsonProcessingException {
        ProductEvent product = objectMapper.readValue(productMessage, ProductEvent.class);
        if ("DELETE".equals(product.operation()) || "UPDATE".equals(product.operation())) {
            mediaRepository.deleteByProductId(product.productId());
        }
        if ("DELETE".equals(product.operation()) || product.imageUrls() == null || product.imageUrls().length == 0) {
            return;
        }

        mediaRepository.saveAll(Arrays.stream(product.imageUrls())
            .map(imageUrl -> new Media(null, imageUrl, product.productId()))
                .toList());
        logger.info("Saved {} media entries for product {}", product.imageUrls().length, product.productId());
    }

    private record ProductEvent(String operation, String productId, String[] imageUrls) {
    }
}