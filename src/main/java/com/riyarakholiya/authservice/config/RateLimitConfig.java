package com.riyarakholiya.authservice.config;

import com.riyarakholiya.authservice.ratelimit.RateLimiter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class RateLimitConfig {

    @Bean
    RateLimiter refreshPerUserLimiter(AuthProperties properties, Clock clock) {
        return create(properties.rateLimit().refreshPerUser(), clock);
    }

    @Bean
    RateLimiter refreshPerIpLimiter(AuthProperties properties, Clock clock) {
        return create(properties.rateLimit().refreshPerIp(), clock);
    }

    private static RateLimiter create(AuthProperties.Bucket bucket, Clock clock) {
        return new RateLimiter(bucket.capacity(), bucket.refillPerMinute(), clock);
    }
}
