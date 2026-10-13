package com.studyanchor.social.infrastructure;

import com.studyanchor.social.domain.Presence;

import java.time.Instant;
import java.util.UUID;

/**
 * Implements FR-09. Social's own copy of the {@code focus.sessions} contract (docs/05-events.md). One record
 * holds the fields of both FocusSessionStarted and FocusSessionEnded; fields that don't apply are null.
 */
record FocusSessionEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID sessionId,
        UUID timeBlockId,
        UUID topicId,
        Integer plannedMinutes,
        Instant startedAt,
        Long actualSeconds,
        Instant endedAt) {

    static final String STARTED = "FocusSessionStarted";
    static final String ENDED = "FocusSessionEnded";

    boolean isStarted() {
        return STARTED.equals(eventType);
    }

    boolean isEnded() {
        return ENDED.equals(eventType);
    }

    Presence toPresence() {
        return new Presence(userId, sessionId, topicId, startedAt);
    }
}
