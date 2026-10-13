package com.studyanchor.social.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Implements FR-09. Value object: a student is in a focus session right now. Derived from
 * {@code focus.sessions} events (Social never asks Focus directly); {@code topicId} may be null.
 */
public record Presence(String userId, UUID sessionId, UUID topicId, Instant startedAt) {

    /** FR-09 AC8: time since the session started, in whole seconds; never negative (clock skew). */
    public Duration elapsed(Instant now) {
        Duration elapsed = Duration.between(startedAt, now).truncatedTo(ChronoUnit.SECONDS);
        return elapsed.isNegative() ? Duration.ZERO : elapsed;
    }
}
