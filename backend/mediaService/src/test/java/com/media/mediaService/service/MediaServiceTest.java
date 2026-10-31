package com.media.mediaService.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.cloudinary.Cloudinary;

class MediaServiceTest {
    private Cloudinary cloudinary;
    private MediaService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class, RETURNS_DEEP_STUBS);
        service = new MediaService(cloudinary);
    }

    @Test
    void uploadImageUploadsSupportedPngAndReturnsSecureUrl() throws Exception {
        when(cloudinary.uploader().upload(any(byte[].class), anyMap()))
                .thenReturn(Map.of("secure_url", "https://cdn.test/image.png"));
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", pngBytes());

        assertEquals("https://cdn.test/image.png", service.uploadImage(file));
    }

    @Test
    void uploadImageRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        assertEquals("Image file must not be empty",
                assertThrows(IllegalArgumentException.class, () -> service.uploadImage(file)).getMessage());
    }

    @Test
    void uploadImageRejectsFilesLargerThanTwoMegabytes() {
        byte[] bytes = new byte[(int) MediaService.MAX_IMAGE_SIZE + 1];
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", bytes);

        assertEquals("Image file must be at most 2 MB",
                assertThrows(IllegalArgumentException.class, () -> service.uploadImage(file)).getMessage());
    }

    @Test
    void uploadImageRejectsUnsupportedMimeType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.gif", "image/gif", pngBytes());

        assertEquals("Only JPG, JPEG, and PNG images are supported",
                assertThrows(IllegalArgumentException.class, () -> service.uploadImage(file)).getMessage());
    }

    @Test
    void uploadImageRejectsContentThatIsNotAnImage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "not an image".getBytes());

        assertEquals("Only JPG, JPEG, and PNG images are supported",
                assertThrows(IllegalArgumentException.class, () -> service.uploadImage(file)).getMessage());
    }

    @Test
    void uploadImageReportsMissingCloudinaryUrlAsIoFailure() throws Exception {
        when(cloudinary.uploader().upload(any(byte[].class), anyMap())).thenReturn(Map.of());
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", pngBytes());

        assertEquals("Cloudinary did not return an image URL",
                assertThrows(IOException.class, () -> service.uploadImage(file)).getMessage());
    }

    @Test
    void fetchImageReturnsBytesAndResponseContentType() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MediaService httpService = new MediaService(cloudinary, builder.build());
        byte[] image = { 4, 5, 6 };
        server.expect(requestTo("https://cdn.test/image.png"))
                .andRespond(withSuccess(image, MediaType.IMAGE_PNG));

        MediaService.StoredImage result = httpService.fetchImage("https://cdn.test/image.png");

        assertArrayEquals(image, result.content());
        assertEquals(MediaType.IMAGE_PNG, result.contentType());
        server.verify();
    }

    @Test
    void fetchImageUsesOctetStreamWhenResponseHasNoContentType() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        MediaService httpService = new MediaService(cloudinary, builder.build());
        server.expect(requestTo("https://cdn.test/image.bin"))
                .andRespond(withSuccess(new byte[] { 1 }, null));

        assertEquals(MediaType.APPLICATION_OCTET_STREAM,
                httpService.fetchImage("https://cdn.test/image.bin").contentType());
        server.verify();
    }

    private byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
