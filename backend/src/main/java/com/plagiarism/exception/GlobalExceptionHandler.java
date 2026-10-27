package com.plagiarism.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>>
    handleAccessDenied(
            AccessDeniedException exception) {

        Map<String, Object> response =
                new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.FORBIDDEN.value());
        response.put("error", "Forbidden");
        response.put("message", exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    // ==========================================
    // RESOURCE NOT FOUND - 404
    // ==========================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>>
    handleNotFound(
            ResourceNotFoundException exception) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                HttpStatus.NOT_FOUND.value()
        );

        response.put(
                "error",
                "Not Found"
        );

        response.put(
                "message",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }


    // ==========================================
    // BAD REQUEST - 400
    // ==========================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>>
    handleBadRequest(
            IllegalArgumentException exception) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                HttpStatus.BAD_REQUEST.value()
        );

        response.put(
                "error",
                "Bad Request"
        );

        response.put(
                "message",
                exception.getMessage()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }


    // ==========================================
    // GENERAL RUNTIME EXCEPTION - 400
    // ==========================================

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>>
    handleRuntimeException(
            RuntimeException exception) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                HttpStatus.BAD_REQUEST.value()
        );

        response.put(
                "error",
                "Bad Request"
        );

        response.put(
                "message",
                exception.getMessage()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }


    // ==========================================
    // UNEXPECTED EXCEPTION - 500
    // ==========================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>>
    handleGeneralException(
            Exception exception) {

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "timestamp",
                LocalDateTime.now()
        );

        response.put(
                "status",
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );

        response.put(
                "error",
                "Internal Server Error"
        );

        response.put(
                "message",
                "Something went wrong on the server."
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}