package com.studyanchor.focus.domain;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-05. Published to {@code focus.sessions} when a session starts. */
public record FocusSessionStarted(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID sessionId,
        UUID timeBlockId,
        UUID topicId,
        int plannedMinutes,
        Instant startedAt) implements DomainEvent {

    public static final String TYPE = "FocusSessionStarted";

    static FocusSessionStarted of(FocusSession session) {
        return new FocusSessionStarted(UUID.randomUUID(), TYPE, session.startedAt(), session.userId(),
                session.id(), session.timeBlockId(), session.topicId(),
                session.plannedMinutes().value(), session.startedAt());
    }
}
