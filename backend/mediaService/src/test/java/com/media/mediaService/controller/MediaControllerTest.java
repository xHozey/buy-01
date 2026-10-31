package com.media.mediaService.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.media.mediaService.model.Media;
import com.media.mediaService.reposiroty.MediaRepository;
import com.media.mediaService.service.MediaService;

class MediaControllerTest {
    private MediaService mediaService;
    private MediaRepository mediaRepository;
    private MediaController controller;

    @BeforeEach
    void setUp() {
        mediaService = org.mockito.Mockito.mock(MediaService.class);
        mediaRepository = org.mockito.Mockito.mock(MediaRepository.class);
        controller = new MediaController(mediaService, mediaRepository);
    }

    @Test
    void getProductMediaReturnsImagePaths() {
        when(mediaRepository.findByProductId("product-1")).thenReturn(List.of(
                new Media("1", "https://cdn.test/one.jpg", "product-1"),
                new Media("2", "https://cdn.test/two.jpg", "product-1")));

        assertEquals(List.of("https://cdn.test/one.jpg", "https://cdn.test/two.jpg"),
                controller.getProductMedia("product-1"));
    }

    @Test
    void getImageReturnsContentTypeBodyAndPublicCacheHeader() {
        byte[] bytes = { 1, 2, 3 };
        when(mediaRepository.findById("media-1")).thenReturn(Optional.of(
                new Media("media-1", "https://cdn.test/image.jpg", "product-1")));
        when(mediaService.fetchImage("https://cdn.test/image.jpg"))
                .thenReturn(new MediaService.StoredImage(bytes, MediaType.IMAGE_JPEG));

        var response = controller.getImage("media-1");

        assertSame(bytes, response.getBody());
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertEquals("max-age=2592000, public", response.getHeaders().getCacheControl());
    }

    @Test
    void getImageReturnsNotFoundWhenMetadataIsMissing() {
        when(mediaRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.getImage("missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(mediaService, never()).fetchImage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void getImageMapsFetchFailureToNotFound() {
        when(mediaRepository.findById("media-1")).thenReturn(Optional.of(
                new Media("media-1", "https://cdn.test/image.jpg", "product-1")));
        when(mediaService.fetchImage("https://cdn.test/image.jpg"))
                .thenThrow(new IllegalStateException("empty image"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.getImage("media-1"));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void uploadImageReturnsUploadedUrlAsIdAndUrl() throws Exception {
        var file = new MockMultipartFile("file", "image.png", "image/png", new byte[] { 1 });
        when(mediaService.uploadImage(file)).thenReturn("https://cdn.test/image.png");

        assertEquals(Map.of("id", "https://cdn.test/image.png", "url", "https://cdn.test/image.png"),
                controller.uploadImage(file, (Authentication) null));
    }

    @Test
    void uploadImageMapsInvalidFileToBadRequest() throws Exception {
        var file = new MockMultipartFile("file", "bad.txt", "text/plain", new byte[] { 1 });
        when(mediaService.uploadImage(file)).thenThrow(new IllegalArgumentException("unsupported"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.uploadImage(file, null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void uploadImageMapsIoFailureToServerError() throws Exception {
        var file = new MockMultipartFile("file", "image.png", "image/png", new byte[] { 1 });
        when(mediaService.uploadImage(file)).thenThrow(new IOException("storage unavailable"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.uploadImage(file, null));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    @Test
    void uploadImagesRejectsEmptyAndOversizedBatchesBeforeUploading() throws Exception {
        ResponseStatusException empty = assertThrows(ResponseStatusException.class,
                () -> controller.uploadImages(List.of(), null));
        assertEquals(HttpStatus.BAD_REQUEST, empty.getStatusCode());

        List<MockMultipartFile> tooMany = List.of(file("1"), file("2"), file("3"), file("4"), file("5"), file("6"));
        ResponseStatusException oversized = assertThrows(ResponseStatusException.class,
                () -> controller.uploadImages(tooMany.stream().map(f -> (org.springframework.web.multipart.MultipartFile) f).toList(), null));
        assertEquals(HttpStatus.BAD_REQUEST, oversized.getStatusCode());
        verify(mediaService, never()).uploadImage(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void uploadImagesReturnsUrlsInInputOrder() throws Exception {
        var first = file("1");
        var second = file("2");
        when(mediaService.uploadImage(first)).thenReturn("url-1");
        when(mediaService.uploadImage(second)).thenReturn("url-2");

        assertEquals(Map.of("urls", List.of("url-1", "url-2")), controller.uploadImages(List.of(first, second), null));
    }

    @Test
    void uploadImagesStopsAndMapsInvalidFile() throws Exception {
        var first = file("1");
        when(mediaService.uploadImage(first)).thenThrow(new IllegalArgumentException("unsupported"));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> controller.uploadImages(List.of(first), null));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("files", name + ".png", "image/png", new byte[] { 1 });
    }
}
