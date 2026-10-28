package com.riyarakholiya.authservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.riyarakholiya.authservice.auth.dto.LoginRequest;
import com.riyarakholiya.authservice.auth.dto.RegisterRequest;
import com.riyarakholiya.authservice.auth.dto.TokenResponse;
import com.riyarakholiya.authservice.auth.dto.UserResponse;
import com.riyarakholiya.authservice.support.ApiIntegrationTest;
import com.riyarakholiya.authservice.support.TestClockConfig;
import com.riyarakholiya.authservice.web.ApiError;
import com.riyarakholiya.authservice.web.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestClockConfig.class)
class AuthFlowIT extends ApiIntegrationTest {

    @Test
    void registerThenLoginIssuesAccessTokenAndHardenedRefreshCookie() {
        ResponseEntity<UserResponse> registered = register("Alice@Example.com");
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getBody().email()).isEqualTo("alice@example.com");

        ResponseEntity<TokenResponse> login = login("alice@example.com");
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().accessToken()).isNotBlank();
        assertThat(login.getBody().tokenType()).isEqualTo("Bearer");
        assertThat(login.getBody().expiresIn()).isEqualTo(900);

        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith("refresh_token=")
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/auth");
        assertThat(login.getBody().toString()).doesNotContain(refreshCookieValue(login));
    }

    @Test
    void meRequiresValidAccessToken() {
        register("bob@example.com");
        String accessToken = login("bob@example.com").getBody().accessToken();

        ResponseEntity<ApiError> anonymous = http.getForEntity("/me", ApiError.class);
        assertThat(anonymous.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(anonymous.getBody().code()).isEqualTo(ErrorCode.UNAUTHORIZED);

        ResponseEntity<UserResponse> me = http.exchange("/me", HttpMethod.GET,
                new HttpEntity<>(bearer(accessToken)), UserResponse.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody().email()).isEqualTo("bob@example.com");
    }

    @Test
    void duplicateRegistrationIsRejectedWithConflict() {
        register("carol@example.com");

        ResponseEntity<ApiError> duplicate = http.postForEntity("/auth/register",
                new RegisterRequest("CAROL@example.com", PASSWORD), ApiError.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().code()).isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
    }

    @Test
    void wrongPasswordAndUnknownEmailReturnTheSameError() {
        register("dave@example.com");

        ResponseEntity<ApiError> wrongPassword = http.postForEntity("/auth/login",
                new LoginRequest("dave@example.com", "not-the-password"), ApiError.class);
        ResponseEntity<ApiError> unknownEmail = http.postForEntity("/auth/login",
                new LoginRequest("nobody@example.com", PASSWORD), ApiError.class);

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownEmail.getBody()).isEqualTo(wrongPassword.getBody());
        assertThat(wrongPassword.getBody().code()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void invalidInputReturnsValidationError() {
        ResponseEntity<ApiError> response = http.postForEntity("/auth/register",
                new RegisterRequest("not-an-email", "short"), ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(response.getBody().message()).contains("email", "password");
    }
}
