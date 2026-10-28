package com.riyarakholiya.authservice.auth;

import com.riyarakholiya.authservice.auth.dto.LoginRequest;
import com.riyarakholiya.authservice.auth.dto.RegisterRequest;
import com.riyarakholiya.authservice.auth.dto.UserResponse;
import com.riyarakholiya.authservice.config.AuthProperties;
import com.riyarakholiya.authservice.user.UserDocument;
import com.riyarakholiya.authservice.user.UserRepository;
import com.riyarakholiya.authservice.web.AuthException;
import com.riyarakholiya.authservice.web.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokens;
    private final AccessTokenIssuer accessTokens;
    private final AuthProperties.Refresh refreshProperties;
    private final Clock clock;
    // Checked when the email is unknown, so an unknown email costs the same BCrypt time as a wrong password.
    private final String dummyPasswordHash;

    AuthService(UserRepository users, PasswordEncoder passwordEncoder, RefreshTokenStore refreshTokens,
                AccessTokenIssuer accessTokens, AuthProperties properties, Clock clock) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
        this.accessTokens = accessTokens;
        this.refreshProperties = properties.refresh();
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new AuthException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        try {
            UserDocument saved = users.save(new UserDocument(null, email,
                    passwordEncoder.encode(request.password()), clock.instant()));
            return new UserResponse(saved.id(), saved.email());
        } catch (DuplicateKeyException e) {
            // A concurrent register for the same email won between the check and the insert.
            throw new AuthException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
    }

    AuthTokens login(LoginRequest request) {
        Optional<UserDocument> user = users.findByEmail(normalizeEmail(request.email()));
        String hashToCheck = user.map(UserDocument::passwordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);
        if (user.isEmpty() || !passwordMatches) {
            throw new AuthException(ErrorCode.INVALID_CREDENTIALS);
        }
        return startSession(user.get().id());
    }

    /** A login starts a new family: a new session whose first refresh token is ACTIVE. */
    private AuthTokens startSession(String userId) {
        Instant now = clock.instant();
        String refreshToken = RefreshTokens.generate();
        refreshTokens.insert(RefreshTokenDocument.active(RefreshTokens.hash(refreshToken),
                UUID.randomUUID().toString(), userId, now, now.plus(refreshProperties.sessionTtl())));
        return new AuthTokens(accessTokens.issue(userId), accessTokens.ttl().toSeconds(), refreshToken);
    }

    /**
     * Single-use rotation. The presented token is exchanged for a successor, and the old token can
     * never be used to rotate again. The compare-and-set in {@code claimActive} lets exactly one
     * concurrent caller win. Everyone else falls through to {@link #handleNonActive}.
     */
    AuthTokens refresh(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw new AuthException(ErrorCode.REFRESH_TOKEN_MISSING);
        }
        String hash = RefreshTokens.hash(presentedToken);
        Instant now = clock.instant();

        Optional<RefreshTokenDocument> claimed = refreshTokens.claimActive(hash, now);
        if (claimed.isEmpty()) {
            return handleNonActive(hash, now);
        }
        RefreshTokenDocument consumed = claimed.get();
        String successor = RefreshTokens.generate();
        refreshTokens.insert(RefreshTokenDocument.active(RefreshTokens.hash(successor), consumed.familyId(),
                consumed.userId(), now, consumed.sessionExpiresAt()));
        return new AuthTokens(accessTokens.issue(consumed.userId()), accessTokens.ttl().toSeconds(), successor);
    }

    private AuthTokens handleNonActive(String hash, Instant now) {
        RefreshTokenDocument token = refreshTokens.findByHash(hash)
                .orElseThrow(() -> new AuthException(ErrorCode.REFRESH_TOKEN_INVALID));
        if (!token.sessionExpiresAt().isAfter(now) || token.status() != TokenStatus.ROTATED) {
            // Expired, revoked, or unknown: nothing to recover.
            throw new AuthException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (Duration.between(token.rotatedAt(), now).compareTo(refreshProperties.graceWindow()) <= 0) {
            // A concurrent retry, such as a second tab still holding the old cookie. The browser already
            // has the successor from the first response, so only a new access token is returned.
            return new AuthTokens(accessTokens.issue(token.userId()), accessTokens.ttl().toSeconds(), null);
        }
        // A rotated token used after the grace window means the token leaked. Revoke the whole family.
        refreshTokens.revokeFamily(token.familyId());
        throw new AuthException(ErrorCode.REFRESH_TOKEN_REUSED);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
