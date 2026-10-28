package com.riyarakholiya.authservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull Refresh refresh) {

    public record Jwt(
            @NotBlank @Size(min = 32) String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl) {
    }

    public record Refresh(@NotNull Duration sessionTtl, @NotNull Duration graceWindow) {
    }
}
