package com.media.mediaService.service;

import java.io.IOException;
import java.nio.file.Paths;
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
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
public class MediaService {

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

		if (file.isEmpty() || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
			throw new IllegalArgumentException("Only non-empty image files are supported");
		}

		String filename = Paths.get(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename())
				.getFileName()
				.toString();
		String key = "images/" + UUID.randomUUID() + "-" + filename;

		PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(file.getContentType())
				.build();
		s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
		return key;
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