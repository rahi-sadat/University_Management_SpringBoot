package com.example.university_management.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Global exception handler that catches exceptions thrown across controllers
 * and returns consistent JSON error responses: {"error": "<message>"}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles invalid login credentials -> 401 Unauthorized.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(Map.of("error", "Invalid email or password"));
    }

    /**
     * Handles access denied / authorization errors -> 403 Forbidden.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(org.springframework.security.access.AccessDeniedException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
    }

    /**
     * Handles input validation errors from service layer -> 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    /**
     * Handles Jakarta Bean Validation errors (@Valid) -> 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = "Invalid request data";
        if (exception.getBindingResult().hasFieldErrors()) {
            message = exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        }
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handles invalid JSON bodies or wrong URL ID types -> 400 Bad Request.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleMalformed(Exception exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Invalid JSON or account ID");
    }

    /**
     * Handles explicit HTTP status exceptions (e.g., 404 NOT FOUND, 409 CONFLICT).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException exception) {
        String reason = exception.getReason();
        if (reason == null || reason.isBlank()) {
            reason = "Request failed";
        }
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("error", reason));
    }

    /**
     * Handles database unique constraint violations (e.g. duplicate email) -> 409 Conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, "Account conflicts with existing data; email must be unique");
    }

    /**
     * Handles database connectivity errors -> 503 Service Unavailable.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDatabase(DataAccessException exception) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Database temporarily unavailable");
    }

    /**
     * Helper method to build a standard error response entity.
     */
    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
