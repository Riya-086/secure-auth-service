package com.riyarakholiya.authservice.web;

import org.springframework.http.HttpStatus;

/** Stable machine-readable error codes. Clients branch on {@code code}, never on the message text. */
public enum ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed"),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Request body is missing or malformed"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is incorrect"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Missing or invalid access token"),
    REFRESH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "Refresh token cookie is missing"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh token is invalid, expired, or revoked"),
    REFRESH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "Refresh token reuse detected; the session was revoked"),
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "An account with this email already exists"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Too many requests; retry later"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
