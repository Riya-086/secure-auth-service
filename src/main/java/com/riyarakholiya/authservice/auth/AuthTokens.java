package com.riyarakholiya.authservice.auth;

/**
 * Result of a successful login.
 *
 * @param accessToken the JWT for the Authorization header
 * @param expiresIn   access-token lifetime in seconds
 * @param refreshToken the new raw refresh token, sent only as an HttpOnly cookie. It is null when a
 *                     grace-window retry returns only an access token, because the cookie already
 *                     holds the successor from the first response.
 */
record AuthTokens(String accessToken, long expiresIn, String refreshToken) {
}
