package com.riyarakholiya.authservice.support;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the system clock with a {@link MutableClock}. It starts at real time, not at a fixed date,
 * because JWT expiry is validated against the real clock. It is truncated to milliseconds because
 * MongoDB stores dates at millisecond precision, and the grace-window boundary must match exactly.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock(Instant.now().truncatedTo(ChronoUnit.MILLIS));
    }
}
