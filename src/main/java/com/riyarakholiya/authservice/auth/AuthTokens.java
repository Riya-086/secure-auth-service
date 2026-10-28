package com.riyarakholiya.authservice.auth;

/**
 * Result of a successful login.
 *
 * @param accessToken the JWT for the Authorization header
 * @param expiresIn   access-token lifetime in seconds
 * @param refreshToken the raw refresh token, to be sent back only as an HttpOnly cookie
 */
record AuthTokens(String accessToken, long expiresIn, String refreshToken) {
}
