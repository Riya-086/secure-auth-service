package com.riyarakholiya.authservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Refresh refresh,
        @Valid @NotNull RateLimit rateLimit) {

    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl) {
    }

    public record Refresh(@NotNull Duration sessionTtl, @NotNull Duration graceWindow) {
    }

    public record RateLimit(@Valid @NotNull Bucket refreshPerUser, @Valid @NotNull Bucket refreshPerIp) {
    }

    /** A token bucket: {@code capacity} is the burst size, {@code refillPerMinute} the steady rate. */
    public record Bucket(@Positive int capacity, @Positive int refillPerMinute) {
    }
}
