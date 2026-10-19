package com.media.mediaService.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.media.mediaService.service.MediaService;
import com.media.mediaService.reposiroty.MediaRepository;
import com.media.mediaService.model.Media;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private final MediaService mediaService;
    private final MediaRepository mediaRepository;

    public MediaController(MediaService mediaService, MediaRepository mediaRepository) {
        this.mediaService = mediaService;
        this.mediaRepository = mediaRepository;
    }

    @GetMapping("/products/{productId}")
    public List<String> getProductMedia(@PathVariable String productId) {
        return mediaRepository.findByProductId(productId).stream()
                .map(media -> media.getImagePath())
                .toList();
    }

    @PostMapping("/images")
    public Map<String, String> uploadImage(@RequestPart("file") MultipartFile file,
            Authentication authentication) {
        requireSeller(authentication);
        try {
            String key = mediaService.uploadImage(file);
            Media media = mediaRepository.save(new Media(null, key, null));
            return Map.of("id", media.getId(), "url", "/api/v1/media/images/" + media.getId());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload image", exception);
        }
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable String id) {
        Media media = mediaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        try {
            MediaService.StoredImage image = mediaService.getImage(media.getImagePath());
                MediaType contentType = MediaType.parseMediaType(image.contentType());
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                    .body(image.content());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found", exception);
        }
    }

    @PostMapping("/upload/multiple")
    public Map<String, List<String>> uploadImages(@RequestPart("files") List<MultipartFile> files) {
        if (files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one image is required");
        }

        List<String> keys = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                keys.add(mediaService.uploadImage(file));
            }
            return Map.of("keys", keys);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload images", exception);
        }
    }

    private void requireSeller(Authentication authentication) {
        boolean seller = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_SELLER")
                        || authority.getAuthority().equals("SELLER"));
        if (!seller) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller role required");
        }
    }
}