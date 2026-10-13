package com.studyanchor.planner.infrastructure;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-07 AC14. Planner's own copy of the {@code tutor.understanding} contract (docs/05-events.md). */
record UnderstandingRecordedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID tutoringSessionId,
        UUID topicId,
        int level) {
}
