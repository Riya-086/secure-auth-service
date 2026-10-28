package com.riyarakholiya.authservice.web;

/** The JSON body of every error response: {@code {"code": "...", "message": "..."}}. */
public record ApiError(ErrorCode code, String message) {
}
