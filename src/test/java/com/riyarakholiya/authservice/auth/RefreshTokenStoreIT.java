package com.riyarakholiya.authservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.riyarakholiya.authservice.support.MongoIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@DataMongoTest
@Import(RefreshTokenStore.class)
class RefreshTokenStoreIT extends MongoIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");
    private static final Instant SESSION_END = NOW.plus(Duration.ofDays(30));

    @Autowired
    private RefreshTokenStore store;

    @Autowired
    private MongoTemplate mongo;

    @BeforeEach
    void clean() {
        mongo.remove(new Query(), RefreshTokenDocument.class);
    }

    @Test
    void claimActiveSucceedsOnceAndMarksTokenRotated() {
        store.insert(active("hash-1", "family-1"));

        assertThat(store.claimActive("hash-1", NOW)).isPresent()
                .get().extracting(RefreshTokenDocument::status).isEqualTo(TokenStatus.ROTATED);
        assertThat(store.claimActive("hash-1", NOW)).isEmpty();
    }

    @Test
    void concurrentClaimsOfTheSameTokenHaveExactlyOneWinner() throws Exception {
        store.insert(active("hash-race", "family-race"));
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Future<Boolean>> results = IntStream.range(0, 32)
                    .mapToObj(i -> (Callable<Boolean>) () -> store.claimActive("hash-race", NOW).isPresent())
                    .map(pool::submit)
                    .toList();

            long winners = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    winners++;
                }
            }
            assertThat(winners).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void claimRejectsTokenWhoseSessionHasExpired() {
        store.insert(active("hash-expired", "family-expired"));

        assertThat(store.claimActive("hash-expired", SESSION_END.plusSeconds(1))).isEmpty();
    }

    @Test
    void revokeFamilyOnlyTouchesThatFamily() {
        store.insert(active("a-1", "family-a"));
        store.insert(active("b-1", "family-b"));

        assertThat(store.revokeFamily("family-a")).isEqualTo(1);

        assertThat(store.findByHash("a-1")).get()
                .extracting(RefreshTokenDocument::status).isEqualTo(TokenStatus.REVOKED);
        assertThat(store.findByHash("b-1")).get()
                .extracting(RefreshTokenDocument::status).isEqualTo(TokenStatus.ACTIVE);
    }

    private static RefreshTokenDocument active(String hash, String familyId) {
        return RefreshTokenDocument.active(hash, familyId, "user-1", NOW, SESSION_END);
    }
}
