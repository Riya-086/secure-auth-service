package com.riyarakholiya.authservice.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token buckets, one per key. Each key starts with {@code capacity} tokens (the burst size)
 * and regains one token every {@code 60 / refillPerMinute} seconds, up to capacity.
 *
 * <p>State lives in this JVM only. With several instances, each one enforces its own limit. Use a
 * shared store such as Redis when that matters.
 */
public final class RateLimiter {

    private final int capacity;
    private final double refillPerSecond;
    private final Clock clock;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int capacity, int refillPerMinute, Clock clock) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerMinute / 60.0;
        this.clock = clock;
    }

    /** Takes one token for {@code key}, or throws {@link RateLimitExceededException} if none is left. */
    public void acquire(String key) {
        Instant now = clock.instant();
        long retryAfterSeconds = buckets.computeIfAbsent(key, k -> new TokenBucket(now)).tryTake(now);
        if (retryAfterSeconds > 0) {
            throw new RateLimitExceededException(retryAfterSeconds);
        }
    }

    private final class TokenBucket {

        private double tokens;
        private Instant updatedAt;

        TokenBucket(Instant now) {
            this.tokens = capacity;
            this.updatedAt = now;
        }

        /** Returns 0 if a token was taken, or else the seconds until the next token is available. */
        synchronized long tryTake(Instant now) {
            if (now.isAfter(updatedAt)) {
                double elapsedSeconds = Duration.between(updatedAt, now).toNanos() / 1_000_000_000.0;
                tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
                updatedAt = now;
            }
            if (tokens >= 1) {
                tokens -= 1;
                return 0;
            }
            return (long) Math.ceil((1 - tokens) / refillPerSecond);
        }
    }
}
