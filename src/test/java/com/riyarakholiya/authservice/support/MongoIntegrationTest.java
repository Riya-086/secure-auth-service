package com.riyarakholiya.authservice.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Starts one MongoDB container for the whole test run. Every integration test context points at
 * it. The container is started once and never stopped here; Testcontainers' Ryuk reaper removes
 * it when the JVM exits.
 */
public abstract class MongoIntegrationTest {

    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    static {
        MONGO.start();
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> MONGO.getConnectionString() + "/authservice-test");
        // Test-only key. Production reads JWT_SECRET from the environment.
        registry.add("auth.jwt.secret", () -> "test-only-signing-key-at-least-32-bytes-long");
    }
}
