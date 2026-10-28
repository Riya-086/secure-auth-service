package com.riyarakholiya.authservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.riyarakholiya.authservice.support.ApiIntegrationTest;
import com.riyarakholiya.authservice.support.TestClockConfig;
import com.riyarakholiya.authservice.web.ApiError;
import com.riyarakholiya.authservice.web.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "auth.rate-limit.refresh-per-user.capacity=5")
@Import(TestClockConfig.class)
class RateLimitIT extends ApiIntegrationTest {

    @Test
    void perUserLimitReturns429WithRetryAfterAndRejectedRequestDoesNotRotate() {
        register("judy@example.com");
        String token = refreshCookieValue(login("judy@example.com"));
        for (int i = 0; i < 5; i++) {
            token = refreshCookieValue(refresh(token));
        }

        ResponseEntity<ApiError> limited = refreshRejected(token);

        assertThat(limited.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(limited.getBody().code()).isEqualTo(ErrorCode.RATE_LIMITED);
        assertThat(Long.parseLong(limited.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))).isPositive();

        // The 429 came before the claim, so the same token still works once the bucket refills.
        clock.advance(Duration.ofMinutes(1));
        assertThat(refresh(token).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
