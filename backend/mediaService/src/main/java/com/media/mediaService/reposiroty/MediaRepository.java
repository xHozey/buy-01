package com.media.mediaService.reposiroty;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.media.mediaService.model.Media;

public interface MediaRepository extends MongoRepository<Media, String> {
}