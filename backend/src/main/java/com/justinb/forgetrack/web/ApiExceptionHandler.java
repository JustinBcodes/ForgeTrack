package com.justinb.forgetrack.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiModels.ApiError> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler({ConflictException.class, DataIntegrityViolationException.class})
    ResponseEntity<ApiModels.ApiError> handleConflict(RuntimeException exception, HttpServletRequest request) {
        String message = exception instanceof ConflictException
                ? exception.getMessage()
                : "That resource already exists or conflicts with existing data.";
        return error(HttpStatus.CONFLICT, message, request, Map.of());
    }

    @ExceptionHandler(RestClientResponseException.class)
    ResponseEntity<ApiModels.ApiError> handleGitHub(RestClientResponseException exception, HttpServletRequest request) {
        String message = exception.getStatusCode().value() == 404
                ? "GitHub could not find that pull request."
                : "GitHub rejected the request with status " + exception.getStatusCode().value() + ".";
        return error(HttpStatus.BAD_GATEWAY, message, request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiModels.ApiError> handleValidation(MethodArgumentNotValidException exception,
                                                         HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed.", request, fieldErrors);
    }

    private ResponseEntity<ApiModels.ApiError> error(HttpStatus status, String message,
                                                      HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiModels.ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI(), fields));
    }
}
