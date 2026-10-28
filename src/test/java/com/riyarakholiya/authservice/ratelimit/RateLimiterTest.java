package com.riyarakholiya.authservice.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.riyarakholiya.authservice.support.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

    @Test
    void allowsBurstUpToCapacityThenRejects() {
        RateLimiter limiter = new RateLimiter(3, 60, clock); // one token per second

        limiter.acquire("user-1");
        limiter.acquire("user-1");
        limiter.acquire("user-1");

        assertThatThrownBy(() -> limiter.acquire("user-1"))
                .isInstanceOfSatisfying(RateLimitExceededException.class,
                        e -> assertThat(e.getRetryAfterSeconds()).isEqualTo(1));
    }

    @Test
    void refillsOneTokenPerInterval() {
        RateLimiter limiter = new RateLimiter(1, 60, clock);
        limiter.acquire("key");

        assertThatThrownBy(() -> limiter.acquire("key")).isInstanceOf(RateLimitExceededException.class);

        clock.advance(Duration.ofSeconds(1));
        assertThatCode(() -> limiter.acquire("key")).doesNotThrowAnyException();
    }

    @Test
    void refillNeverExceedsCapacity() {
        RateLimiter limiter = new RateLimiter(2, 60, clock);

        clock.advance(Duration.ofHours(1));
        limiter.acquire("key");
        limiter.acquire("key");

        assertThatThrownBy(() -> limiter.acquire("key")).isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void keysHaveIndependentBuckets() {
        RateLimiter limiter = new RateLimiter(1, 60, clock);
        limiter.acquire("alice");

        assertThatCode(() -> limiter.acquire("bob")).doesNotThrowAnyException();
    }
}
