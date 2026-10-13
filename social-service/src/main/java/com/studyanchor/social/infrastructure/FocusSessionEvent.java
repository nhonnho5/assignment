package com.studyanchor.social.infrastructure;

import java.time.Instant;
import java.util.UUID;

/**
 * Implements FR-05 (consumer side). Social's own copy of the {@code focus.sessions} contract
 * (docs/05-events.md). One record holds the fields of both FocusSessionStarted and FocusSessionEnded;
 * fields that don't apply to an event type are null.
 */
public record FocusSessionEvent(
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
}
