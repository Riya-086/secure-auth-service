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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {ApiIntegrationTest.HIGH_USER_LIMIT, ApiIntegrationTest.HIGH_IP_LIMIT})
@Import(TestClockConfig.class)
class LogoutIT extends ApiIntegrationTest {

    @Test
    void logoutRevokesTheWholeFamilyAndClearsTheCookie() {
        register("liam@example.com");
        String original = refreshCookieValue(login("liam@example.com"));
        String successor = refreshCookieValue(refresh(original));

        ResponseEntity<Void> response = logout(successor);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .startsWith("refresh_token=;")
                .contains("Max-Age=0");
        assertThat(refreshRejected(successor).getBody().code()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
        // The rotated-away token is in the same family, so it is revoked as well.
        assertThat(refreshRejected(original).getBody().code()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    void logoutWithoutACookieIsANoOp() {
        ResponseEntity<Void> response = logout("");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void logoutWithAnUnknownTokenDoesNotAffectOtherSessions() {
        register("mia@example.com");
        String session = refreshCookieValue(login("mia@example.com"));

        logout("not-a-real-token");

        ResponseEntity<ApiError> stillValid = refreshRejected(session);
        assertThat(stillValid.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
