package com.riyarakholiya.authservice.user;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public record UserDocument(
        @Id String id,
        @Indexed(unique = true) String email,
        String passwordHash,
        Instant createdAt) {
}
