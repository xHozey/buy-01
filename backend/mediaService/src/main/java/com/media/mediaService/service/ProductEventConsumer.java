package com.media.mediaService.service;

import java.util.Arrays;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.media.mediaService.model.Media;
import com.media.mediaService.reposiroty.MediaRepository;

@Service
public class ProductEventConsumer {

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
    }

    private record ProductEvent(String operation, String productId, String[] imageUrls) {
    }
}