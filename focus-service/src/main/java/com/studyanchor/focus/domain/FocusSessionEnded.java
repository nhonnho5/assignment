package com.studyanchor.focus.domain;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-05. Published to {@code focus.sessions} when a session ends. */
public record FocusSessionEnded(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID sessionId,
        UUID timeBlockId,
        UUID topicId,
        long actualSeconds,
        Instant endedAt) implements DomainEvent {

    public static final String TYPE = "FocusSessionEnded";

    static FocusSessionEnded of(FocusSession session) {
        return new FocusSessionEnded(UUID.randomUUID(), TYPE, session.endedAt(), session.userId(),
                session.id(), session.timeBlockId(), session.topicId(),
                session.actualDuration().toSeconds(), session.endedAt());
    }
}
