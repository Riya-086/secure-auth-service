package com.riyarakholiya.authservice.support;

import com.riyarakholiya.authservice.auth.RefreshTokenDocument;
import com.riyarakholiya.authservice.auth.dto.LoginRequest;
import com.riyarakholiya.authservice.auth.dto.RegisterRequest;
import com.riyarakholiya.authservice.auth.dto.TokenResponse;
import com.riyarakholiya.authservice.auth.dto.UserResponse;
import com.riyarakholiya.authservice.user.UserDocument;
import com.riyarakholiya.authservice.web.ApiError;
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

    /** Raises the limits so a test can refresh many times without tripping them. Used in @SpringBootTest. */
    public static final String HIGH_USER_LIMIT = "auth.rate-limit.refresh-per-user.capacity=100";
    public static final String HIGH_IP_LIMIT = "auth.rate-limit.refresh-per-ip.capacity=1000";

    @Autowired
    protected TestRestTemplate http;

    @Autowired
    protected MongoTemplate mongo;

    @Autowired
    protected MutableClock clock;

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
        return http.exchange("/auth/refresh", HttpMethod.POST, refreshRequest(refreshToken), TokenResponse.class);
    }

    protected ResponseEntity<ApiError> refreshRejected(String refreshToken) {
        return http.exchange("/auth/refresh", HttpMethod.POST, refreshRequest(refreshToken), ApiError.class);
    }

    private static HttpEntity<Void> refreshRequest(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, REFRESH_COOKIE + "=" + refreshToken);
        return new HttpEntity<>(headers);
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
