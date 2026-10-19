package com.media.mediaService.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ProductEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ProductEventConsumer.class);

    @KafkaListener(topics = "product-topic", groupId = "media-service")
    public void consumeProduct(String productMessage) {
        logger.info("Received product event: {}", productMessage);
    }
}