package com.media.mediaService.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

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
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String PNG_CONTENT_TYPE = "image/png";

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
        if (!isSupportedContentType(file.getContentType()) || !isSupportedImageFormat(file)) {
            throw new IllegalArgumentException("Only JPG, JPEG, and PNG images are supported");
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

    private boolean isSupportedContentType(String contentType) {
        return JPEG_CONTENT_TYPE.equalsIgnoreCase(contentType) || PNG_CONTENT_TYPE.equalsIgnoreCase(contentType);
    }

    private boolean isSupportedImageFormat(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream();
                ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                return false;
            }

            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                return false;
            }

            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName();
                return "JPEG".equalsIgnoreCase(format) || "PNG".equalsIgnoreCase(format);
            } finally {
                reader.dispose();
            }
        }
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