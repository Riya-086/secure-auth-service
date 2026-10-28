package com.riyarakholiya.authservice.auth;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A stored refresh token. Only {@code tokenHash} identifies the token; the raw value is never stored.
 *
 * <p>Every token in a login session shares a {@code familyId}. Each successor copies the
 * family's {@code sessionExpiresAt}, so rotation can never extend a session past its absolute end.
 */
@Document("refresh_tokens")
public record RefreshTokenDocument(
        @Id String id,
        @Indexed(unique = true) String tokenHash,
        @Indexed String familyId,
        String userId,
        TokenStatus status,
        Instant issuedAt,
        Instant rotatedAt,
        @Indexed(expireAfterSeconds = 0) Instant sessionExpiresAt) {

    static RefreshTokenDocument active(String tokenHash, String familyId, String userId,
                                       Instant issuedAt, Instant sessionExpiresAt) {
        return new RefreshTokenDocument(null, tokenHash, familyId, userId, TokenStatus.ACTIVE,
                issuedAt, null, sessionExpiresAt);
    }
}
