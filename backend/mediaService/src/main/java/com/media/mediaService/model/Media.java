package com.media.mediaService.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document("media")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Media {
    @Id
    String id;
    String imagePath;
    String productId;
}