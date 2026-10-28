package com.riyarakholiya.authservice.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that only moves when a test advances it, so grace windows and rate limits are deterministic. */
public final class MutableClock extends Clock {

    private final ZoneId zone = ZoneOffset.UTC;
    private volatile Instant now;

    public MutableClock(Instant start) {
        this.now = start;
    }

    public void advance(Duration by) {
        now = now.plus(by);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("MutableClock always uses UTC");
    }

    @Override
    public Instant instant() {
        return now;
    }
}
