package com.riyarakholiya.authservice.ratelimit;

/** Thrown when a bucket is empty. The web layer turns it into HTTP 429 with a Retry-After header. */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Too many requests");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
