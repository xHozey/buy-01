package com.media.mediaService.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class MediaService {
    public static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;

    private final Cloudinary cloudinary;
    private final RestClient restClient = RestClient.create();

    public MediaService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Image file must not be empty");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image file must be at most 2 MB");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are supported");
        }

        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "products",
                        "public_id", UUID.randomUUID().toString(),
                        "resource_type", "image"));
        String secureUrl = (String) result.get("secure_url");
        if (secureUrl == null || secureUrl.isBlank()) {
            throw new IOException("Cloudinary did not return an image URL");
        }
        return secureUrl;
    }

    public StoredImage fetchImage(String imageUrl) {
        ResponseEntity<byte[]> response = restClient.get()
                .uri(imageUrl)
                .retrieve()
                .toEntity(byte[].class);
        byte[] content = response.getBody();
        if (content == null) {
            throw new IllegalStateException("Cloudinary returned an empty image");
        }
        MediaType contentType = response.getHeaders().getContentType();
        return new StoredImage(content, contentType == null ? MediaType.APPLICATION_OCTET_STREAM : contentType);
    }

    public record StoredImage(byte[] content, MediaType contentType) {
    }
}