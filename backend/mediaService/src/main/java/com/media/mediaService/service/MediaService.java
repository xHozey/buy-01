package com.media.mediaService.service;

import java.io.IOException;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class MediaService {
	public static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;

	private static final Logger logger = LoggerFactory.getLogger(MediaService.class);

	private final S3Client s3Client;
	private final String bucket;
	private volatile boolean bucketReady;

	public MediaService(S3Client s3Client, @Value("${media.storage.bucket}") String bucket) {
		this.s3Client = s3Client;
		this.bucket = bucket;
	}

	@PostConstruct
	void createBucketIfMissing() {
		try {
			ensureBucketExists();
		} catch (RuntimeException exception) {
			logger.warn("Could not initialize media bucket: {}", exception.getMessage());
		}
	}

	public String uploadImage(MultipartFile file) throws IOException {
		ensureBucketExists();

		if (file.isEmpty()) {
			throw new IllegalArgumentException("Image file must not be empty");
		}
		if (file.getSize() > MAX_IMAGE_SIZE) {
			throw new IllegalArgumentException("Image file must be at most 2 MB");
		}
		if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
			throw new IllegalArgumentException("Only image files are supported");
		}

		String key = "images/" + UUID.randomUUID();

		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(file.getContentType())
				.build();
		s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
		return key;
	}

	public StoredImage getImage(String key) {
		ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(
				GetObjectRequest.builder().bucket(bucket).key(key).build());
		return new StoredImage(response.asByteArray(), response.response().contentType());
	}

	public record StoredImage(byte[] content, String contentType) {
	}

	private void ensureBucketExists() {
		if (bucketReady) {
			return;
		}

		try {
			s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
		} catch (S3Exception exception) {
			if (exception.statusCode() != 404) {
				throw exception;
			}
			s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
		}
		bucketReady = true;
	}
}