package com.riyarakholiya.authservice.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import com.riyarakholiya.authservice.auth.dto.TokenResponse;
import com.riyarakholiya.authservice.support.ApiIntegrationTest;
import com.riyarakholiya.authservice.support.TestClockConfig;
import com.riyarakholiya.authservice.web.ApiError;
import com.riyarakholiya.authservice.web.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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
class RefreshRotationIT extends ApiIntegrationTest {

    @Test
    void refreshRotatesTheTokenAndSetsASuccessorCookie() {
        register("gina@example.com");
        String original = refreshCookieValue(login("gina@example.com"));

        ResponseEntity<TokenResponse> rotated = refresh(original);

        assertThat(rotated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rotated.getBody().accessToken()).isNotBlank();
        String successor = refreshCookieValue(rotated);
        assertThat(successor).isNotNull().isNotEqualTo(original);
        assertThat(rotated.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("HttpOnly", "Secure", "SameSite=Strict", "Path=/auth");
    }

    @Test
    void missingCookieIsRejected() {
        ResponseEntity<ApiError> response = http.exchange("/auth/refresh", HttpMethod.POST,
                HttpEntity.EMPTY, ApiError.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.REFRESH_TOKEN_MISSING);
    }

    @Test
    void unknownTokenIsRejected() {
        ResponseEntity<ApiError> response = refreshRejected("not-a-real-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID);
    }

    /**
     * Ten requests race on the same token. Exactly one may rotate it, which is the only request
     * that gets a new cookie. The rest get the grace response with no cookie. The database must
     * show one successor, not ten.
     */
    @Test
    void parallelRefreshesOfOneTokenRotateExactlyOnce() throws Exception {
        register("erin@example.com");
        String original = refreshCookieValue(login("erin@example.com"));

        int parallelism = 10;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(parallelism);
        try {
            List<Future<ResponseEntity<TokenResponse>>> futures = new ArrayList<>();
            IntStream.range(0, parallelism).forEach(i -> futures.add(pool.submit(() -> {
                start.await();
                return refresh(original);
            })));
            start.countDown();

            List<ResponseEntity<TokenResponse>> responses = new ArrayList<>();
            for (Future<ResponseEntity<TokenResponse>> future : futures) {
                responses.add(future.get(30, TimeUnit.SECONDS));
            }

            assertThat(responses).allSatisfy(r -> assertThat(r.getStatusCode()).isEqualTo(HttpStatus.OK));
            long newCookies = responses.stream().filter(r -> refreshCookieValue(r) != null).count();
            assertThat(newCookies).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }

        assertThat(mongo.count(query(where("status").is(TokenStatus.ACTIVE)), RefreshTokenDocument.class))
                .isEqualTo(1);
        assertThat(mongo.count(query(where("status").is(TokenStatus.ROTATED)), RefreshTokenDocument.class))
                .isEqualTo(1);
    }
}
