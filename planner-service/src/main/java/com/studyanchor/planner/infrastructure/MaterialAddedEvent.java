package com.studyanchor.planner.infrastructure;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-02 AC8. Planner's own copy of the {@code tutor.materials} contract (docs/05-events.md). */
record MaterialAddedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID materialId,
        UUID topicId,
        String title) {
}
