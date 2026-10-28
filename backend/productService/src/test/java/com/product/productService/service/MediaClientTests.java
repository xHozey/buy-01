package com.product.productService.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class MediaClientTests {
    private HttpServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void getImageUrlsReturnsMediaServiceResponse() {
        server.createContext("/api/v1/media/products/product-1", exchange -> respond(exchange, 200,
                "[\"image-1\",\"image-2\"]"));
        MediaClient mediaClient = new MediaClient(baseUrl());

        assertArrayEquals(new String[] { "image-1", "image-2" }, mediaClient.getImageUrls("product-1"));
    }

    @Test
    void getImageUrlsReturnsEmptyArrayForEmptyResponse() {
        server.createContext("/api/v1/media/products/product-1", exchange -> respond(exchange, 200, ""));
        MediaClient mediaClient = new MediaClient(baseUrl());

        assertArrayEquals(new String[0], mediaClient.getImageUrls("product-1"));
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}