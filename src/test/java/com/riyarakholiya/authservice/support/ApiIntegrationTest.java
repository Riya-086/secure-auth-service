package com.riyarakholiya.authservice.support;

import com.riyarakholiya.authservice.auth.RefreshTokenDocument;
import com.riyarakholiya.authservice.auth.dto.LoginRequest;
import com.riyarakholiya.authservice.auth.dto.RegisterRequest;
import com.riyarakholiya.authservice.auth.dto.TokenResponse;
import com.riyarakholiya.authservice.auth.dto.UserResponse;
import com.riyarakholiya.authservice.user.UserDocument;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Base for tests that run the full HTTP stack against a real MongoDB. Each concrete test class
 * declares its own {@code @SpringBootTest}, so per-class property overrides stay explicit.
 */
public abstract class ApiIntegrationTest extends MongoIntegrationTest {

    protected static final String PASSWORD = "correct-horse-battery";
    private static final String REFRESH_COOKIE = "refresh_token";

    @Autowired
    protected TestRestTemplate http;

    @Autowired
    protected MongoTemplate mongo;

    @BeforeEach
    void clearDatabase() {
        mongo.remove(new Query(), UserDocument.class);
        mongo.remove(new Query(), RefreshTokenDocument.class);
    }

    protected ResponseEntity<UserResponse> register(String email) {
        return http.postForEntity("/auth/register", new RegisterRequest(email, PASSWORD), UserResponse.class);
    }

    protected ResponseEntity<TokenResponse> login(String email) {
        return http.postForEntity("/auth/login", new LoginRequest(email, PASSWORD), TokenResponse.class);
    }

    protected ResponseEntity<TokenResponse> refresh(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, REFRESH_COOKIE + "=" + refreshToken);
        return http.exchange("/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), TokenResponse.class);
    }

    /** Pulls the raw refresh token out of a Set-Cookie header, or returns null if the response set none. */
    protected static String refreshCookieValue(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        if (setCookie == null || !setCookie.startsWith(REFRESH_COOKIE + "=")) {
            return null;
        }
        int end = setCookie.indexOf(';');
        return setCookie.substring((REFRESH_COOKIE + "=").length(), end < 0 ? setCookie.length() : end);
    }

    protected static HttpHeaders bearer(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
