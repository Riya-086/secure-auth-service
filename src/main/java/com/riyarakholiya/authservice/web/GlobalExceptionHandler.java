package com.riyarakholiya.authservice.web;

import com.riyarakholiya.authservice.ratelimit.RateLimitExceededException;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    ResponseEntity<ApiError> handleAuthException(AuthException ex) {
        return error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ApiError> handleRateLimited(RateLimitExceededException ex) {
        return ResponseEntity.status(ErrorCode.RATE_LIMITED.getStatus())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(new ApiError(ErrorCode.RATE_LIMITED, ErrorCode.RATE_LIMITED.getDefaultMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return error(ErrorCode.VALIDATION_FAILED, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error(ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.getDefaultMessage());
    }

    private static ResponseEntity<ApiError> error(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus()).body(new ApiError(code, message));
    }
}
