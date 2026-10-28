package com.riyarakholiya.authservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.riyarakholiya.authservice.support.ApiIntegrationTest;
import com.riyarakholiya.authservice.support.TestClockConfig;
import com.riyarakholiya.authservice.web.ApiError;
import com.riyarakholiya.authservice.web.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Every test client in this JVM shares 127.0.0.1, so the per-IP bucket is hit across different users. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {"auth.rate-limit.refresh-per-ip.capacity=3", ApiIntegrationTest.HIGH_USER_LIMIT})
@Import(TestClockConfig.class)
class RateLimitPerIpIT extends ApiIntegrationTest {

    @Test
    void perIpLimitAppliesAcrossDifferentUsers() {
        register("kim@example.com");
        register("lee@example.com");
        String kim = refreshCookieValue(login("kim@example.com"));
        String lee = refreshCookieValue(login("lee@example.com"));

        kim = refreshCookieValue(refresh(kim));
        lee = refreshCookieValue(refresh(lee));
        kim = refreshCookieValue(refresh(kim));

        ResponseEntity<ApiError> fourth = refreshRejected(lee);
        assertThat(fourth.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(fourth.getBody().code()).isEqualTo(ErrorCode.RATE_LIMITED);
    }
}
