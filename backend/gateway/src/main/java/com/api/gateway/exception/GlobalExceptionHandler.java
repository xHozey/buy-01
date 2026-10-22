package com.api.gateway.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException exception) {
        return error(HttpStatus.NOT_FOUND.value(), "Route not found");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException exception) {
        return error(exception.getStatusCode().value(), exception.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected server error occurred");
    }

    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status, "message",
                message == null || message.isBlank() ? "Request failed" : message));
    }
}
