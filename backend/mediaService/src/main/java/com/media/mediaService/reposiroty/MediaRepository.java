package com.media.mediaService.reposiroty;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

import com.media.mediaService.model.Media;

public interface MediaRepository extends MongoRepository<Media, String> {
	List<Media> findByProductId(String productId);

	void deleteByProductId(String productId);

}