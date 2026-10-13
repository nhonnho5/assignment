package com.studyanchor.planner.infrastructure;

import java.time.Instant;
import java.util.UUID;

/**
 * Implements FR-12 AC3, AC12. Planner's own copy of the {@code focus.sessions} contract (docs/05-events.md): one
 * record for both FocusSessionStarted and FocusSessionEnded; the fields of the other type are null.
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
        Long actualSeconds,
        Instant startedAt,
        Instant endedAt) {
}
