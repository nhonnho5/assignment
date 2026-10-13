package com.studyanchor.tutor.domain;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-07 AC5. Published to {@code tutor.understanding} when a session ends; Planner keeps the level. */
public record UnderstandingRecorded(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID tutoringSessionId,
        UUID topicId,
        int level) implements DomainEvent {

    static UnderstandingRecorded of(TutoringSession s, Instant now) {
        return new UnderstandingRecorded(UUID.randomUUID(), "UnderstandingRecorded", now, s.userId(), s.id(),
                s.topicId(), s.level());
    }
}
