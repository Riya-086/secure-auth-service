package com.riyarakholiya.authservice.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Creates opaque refresh tokens and their storage form.
 *
 * <p>The raw token only ever leaves the server in the client's cookie. The database holds
 * the SHA-256 hash, so a leaked collection cannot be replayed directly.
 */
final class RefreshTokens {

    private static final int TOKEN_BYTES = 32; // 256 bits of randomness
    private static final SecureRandom RANDOM = new SecureRandom();

    private RefreshTokens() {
    }

    static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Every JRE is required to provide SHA-256.
            throw new IllegalStateException(e);
        }
    }
}
