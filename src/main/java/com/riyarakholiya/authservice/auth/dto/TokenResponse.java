package com.riyarakholiya.authservice.auth.dto;

/** Response body for login and refresh. The refresh token is never in the body; it travels in a cookie. */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
