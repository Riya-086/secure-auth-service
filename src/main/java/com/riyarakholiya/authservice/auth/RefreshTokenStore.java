package com.riyarakholiya.authservice.auth;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for refresh tokens. The two state transitions that matter for
 * security are written as single atomic MongoDB operations here.
 */
@Repository
class RefreshTokenStore {

    private final MongoTemplate mongo;

    RefreshTokenStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    RefreshTokenDocument insert(RefreshTokenDocument token) {
        return mongo.insert(token);
    }

    Optional<RefreshTokenDocument> findByHash(String tokenHash) {
        return Optional.ofNullable(
                mongo.findOne(query(where("tokenHash").is(tokenHash)), RefreshTokenDocument.class));
    }

    /**
     * Compare-and-set {@code ACTIVE -> ROTATED}. MongoDB applies the filter and the update in one
     * atomic step on a single document, so when two requests race for the same token only one
     * gets a document back. The other gets empty.
     */
    Optional<RefreshTokenDocument> claimActive(String tokenHash, Instant now) {
        Query claim = query(where("tokenHash").is(tokenHash)
                .and("status").is(TokenStatus.ACTIVE)
                .and("sessionExpiresAt").gt(now));
        Update rotate = new Update()
                .set("status", TokenStatus.ROTATED)
                .set("rotatedAt", now);
        return Optional.ofNullable(mongo.findAndModify(claim, rotate,
                FindAndModifyOptions.options().returnNew(true), RefreshTokenDocument.class));
    }

    /** Marks every token in the family REVOKED. Returns how many tokens changed. */
    long revokeFamily(String familyId) {
        Query family = query(where("familyId").is(familyId).and("status").ne(TokenStatus.REVOKED));
        return mongo.updateMulti(family, new Update().set("status", TokenStatus.REVOKED),
                RefreshTokenDocument.class).getModifiedCount();
    }
}
