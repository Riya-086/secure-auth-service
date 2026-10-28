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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestClockConfig.class)
class ReplayDetectionIT extends ApiIntegrationTest {

    @Test
    void replayAfterGraceWindowRevokesTheWholeFamily() {
        register("frank@example.com");
        String original = refreshCookieValue(login("frank@example.com"));
        String successor = refreshCookieValue(refresh(original));
        // A second login is a different family and must survive the replay below.
        String otherSession = refreshCookieValue(login("frank@example.com"));

        clock.advance(Duration.ofSeconds(11));

        ResponseEntity<ApiError> replay = refreshRejected(original);
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(replay.getBody().code()).isEqualTo(ErrorCode.REFRESH_TOKEN_REUSED);

        // The legitimate successor from the same family is now dead too.
        assertThat(refreshRejected(successor).getBody().code()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
        // Another login session of the same user is untouched.
        assertThat(refresh(otherSession).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
