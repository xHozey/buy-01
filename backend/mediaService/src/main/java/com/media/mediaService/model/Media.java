package com.media.mediaService.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("media")
public class Media {
    @Id
    String id;
    String imagePath;
    String productId;
}