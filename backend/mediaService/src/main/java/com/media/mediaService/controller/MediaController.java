package com.media.mediaService.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
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

    @PostMapping("/upload")
    public Map<String, String> uploadImage(@RequestPart("file") MultipartFile file) {
        try {
            return Map.of("key", mediaService.uploadImage(file));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not upload image", exception);
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
}