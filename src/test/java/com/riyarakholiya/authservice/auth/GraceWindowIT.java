package com.riyarakholiya.authservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.riyarakholiya.authservice.auth.dto.TokenResponse;
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
        properties = {ApiIntegrationTest.HIGH_USER_LIMIT, ApiIntegrationTest.HIGH_IP_LIMIT})
@Import(TestClockConfig.class)
class GraceWindowIT extends ApiIntegrationTest {

    @Test
    void retryInsideTheGraceWindowGetsAnAccessTokenWithoutANewCookie() {
        register("hana@example.com");
        String original = refreshCookieValue(login("hana@example.com"));
        String successor = refreshCookieValue(refresh(original));

        // A second tab still holds the old cookie and retries a few seconds later.
        clock.advance(Duration.ofSeconds(3));
        ResponseEntity<TokenResponse> retry = refresh(original);

        assertThat(retry.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retry.getBody().accessToken()).isNotBlank();
        assertThat(retry.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
        // The grace retry must not kill the family, so the successor still works.
        assertThat(refreshCookieValue(refresh(successor))).isNotNull();
    }

    @Test
    void graceWindowBoundaryIsInclusiveAndOneMillisecondLaterIsAReplay() {
        register("ivan@example.com");
        String original = refreshCookieValue(login("ivan@example.com"));
        refresh(original);

        clock.advance(Duration.ofSeconds(10));
        assertThat(refresh(original).getStatusCode()).isEqualTo(HttpStatus.OK);

        clock.advance(Duration.ofMillis(1));
        ResponseEntity<ApiError> replay = refreshRejected(original);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(replay.getBody().code()).isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);
    }
}
