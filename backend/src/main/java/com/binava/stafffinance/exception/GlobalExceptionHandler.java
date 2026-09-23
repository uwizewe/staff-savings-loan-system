package com.binava.stafffinance.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiError> missingEndpoint(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "This API endpoint was not found. Check that the backend is up to date.", request, null);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> unauthorized(UnauthorizedException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, exception.getMessage(), request, null);
    }

    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiError> business(Exception exception, HttpServletRequest request) {
        String message = exception instanceof DataIntegrityViolationException
                ? "This record conflicts with existing data. Check unique references and codes."
                : exception.getMessage();
        return response(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "You do not have permission for this action", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return response(HttpStatus.BAD_REQUEST, "Please correct the highlighted information", request, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled error for {} {}", request.getMethod(), request.getRequestURI(), exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Check the server log for details.", request, null);
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message,
                                              HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), fields));
    }
}
