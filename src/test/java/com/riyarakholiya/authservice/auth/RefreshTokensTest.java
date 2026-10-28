package com.riyarakholiya.authservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokensTest {

    @Test
    void generatedTokensAreUniqueAndUrlSafe() {
        String first = RefreshTokens.generate();
        String second = RefreshTokens.generate();

        assertThat(first).isNotEqualTo(second);
        // 32 bytes in unpadded base64url is 43 characters.
        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void hashIsDeterministicSha256Hex() {
        String hash = RefreshTokens.hash("abc");

        assertThat(hash)
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
                .isEqualTo(RefreshTokens.hash("abc"));
    }

    @Test
    void hashNeverEqualsRawToken() {
        String token = RefreshTokens.generate();

        assertThat(RefreshTokens.hash(token)).isNotEqualTo(token).hasSize(64);
    }
}
